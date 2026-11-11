# Worker patterns: report jobs

`POST /report` starts a slow (~30 s) report. The request returns a job id straight away; a worker
builds the report and uploads it; the client polls `GET /report/{id}` until `COMPLETED` or `FAILED`.

This branch shows the patterns that make that reliable. Each one is its own commit.

| Pattern | Problem it solves | Where |
|---|---|---|
| Web / worker split | Slow jobs starving the API; scaling both on one metric | `report/ReportWorker.kt`, `app.worker.enabled` |
| Transactional outbox | Crash between "save job" and "send message" loses the job silently | `db/migration/V2__outbox.sql`, `outbox/OutboxRepository.kt`, `outbox/OutboxRelay.kt`, `ReportJobService.createReport` |
| Idempotent consumer | At-least-once delivery (and the outbox) can deliver a job twice | `ReportJobRepository.claim` / `release`, `V3__report_job_claimed_at.sql` |
| Timeouts | A stuck downstream hangs the worker forever, with no errors | `report/MediaClient.kt` |
| Idempotent side effect | A timed-out upload may have succeeded; the retry must not duplicate it | `MediaClient.upload` (file id derived from the job key) |
| Retries, DLQ, FAILED status | Poison messages retried forever; users stuck on PENDING | queue redrive policy (in AWS) + `SqsReportListener` |
| SQS send and receive | Long polling, visibility timeout, delete on success | `messaging/SqsMessagePublisher.kt`, `report/SqsReportListener.kt` |

## The flow

```
POST /report (web process)
  └─ one transaction: INSERT report_job (PENDING) + INSERT outbox ('report:42')
OutboxRelay (worker process, every 1 s)
  └─ SELECT unsent outbox rows FOR UPDATE SKIP LOCKED → publish → mark sent
SQS queue report-jobs
SqsReportListener (worker process, long-polls) → ReportWorker.processReport
  ├─ claim: UPDATE report_job SET PROCESSING WHERE PENDING or stale claim   (0 rows → duplicate, skip)
  ├─ build report (30 s)
  ├─ upload to media with timeouts, file id = uuid("report:42")             (retry overwrites, never duplicates)
  ├─ success → COMPLETED, resultPath = media:<fileId>
  └─ failure → release claim, throw → not deleted → visible again after 120 s → … → 5th receive: FAILED, SQS moves it to the DLQ
GET /report/42 (web process) → status
```

## Running web and worker separately

Same JAR, two processes:

```
# web: serves HTTP, never consumes
APP_WORKER_ENABLED=false java -jar build/libs/app.jar

# worker: consumes queues and runs the outbox relay, no HTTP server
APP_WORKER_ENABLED=true java -jar build/libs/app.jar --spring.main.web-application-type=none
```

Locally, `app.worker.enabled` defaults to `true`, so a single process does both.

## How SQS behaves here

| | |
|---|---|
| Send | straight to the named queue `report-jobs` (no exchanges; fan-out would be SNS → several queues) |
| Deliver | the worker long-polls (`pollTimeoutSeconds = 20`); SQS never pushes |
| Done | returning normally deletes the message; throwing or crashing leaves it, and it reappears after the visibility timeout (120 s, longer than the job) |
| Retries | counted by SQS (`ApproximateReceiveCount`); the redrive policy (`maxReceiveCount: 5`) moves it to `report-jobs-dlq` |
| Mark FAILED | `SqsReportListener` on the 5th receive, before rethrowing |
| Alarms | `ApproximateAgeOfOldestMessage` on `report-jobs`, and DLQ depth > 0 |

If you know RabbitMQ: it pushes to an open connection and redelivers as soon as a consumer dies; SQS waits for
the visibility timeout. RabbitMQ retries are usually configured in the app; SQS retries live on the queue.
(Shipping notifications in this project still use RabbitMQ.)

Running it needs AWS credentials, a region, and the queues (or LocalStack, with
`spring.cloud.aws.sqs.endpoint=http://localhost:4566`):

```
aws sqs create-queue --queue-name report-jobs-dlq
aws sqs create-queue --queue-name report-jobs --attributes '{
  "VisibilityTimeout": "120",
  "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:<account>:report-jobs-dlq\",\"maxReceiveCount\":\"5\"}"
}'
```

## Known gaps (on purpose, to keep it readable)

- The outbox table is never cleaned up; production would delete sent rows after a few days.
- The read timeout covers waiting for the response, not a server trickling bytes; a hard overall deadline needs a wrapper.
- No heartbeat for SQS jobs longer than the visibility timeout.
- Redriving a FAILED job needs it reset to `PENDING` first, because `FAILED` isn't claimable.
- No alarms in code: alarm on oldest-message age and DLQ depth in CloudWatch.
