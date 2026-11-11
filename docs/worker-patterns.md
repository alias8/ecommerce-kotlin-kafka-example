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
| Retries, DLQ, FAILED status | Poison messages retried forever; users stuck on PENDING | RabbitMQ: `ReportFailureRecoverer`, `config/RabbitMQConfig.kt`. SQS: `SqsReportListener` + queue redrive policy |
| RabbitMQ vs SQS | Same worker, two brokers | `messaging/*Publisher.kt`, `RabbitReportListener`, `SqsReportListener`, `app.queue.transport` |

## The flow

```
POST /report (web process)
  └─ one transaction: INSERT report_job (PENDING) + INSERT outbox ('report:42')
OutboxRelay (worker process, every 1 s)
  └─ SELECT unsent outbox rows FOR UPDATE SKIP LOCKED → publish → mark sent
Broker (RabbitMQ or SQS)
Rabbit/SqsReportListener (worker process) → ReportWorker.processReport
  ├─ claim: UPDATE report_job SET PROCESSING WHERE PENDING or stale claim   (0 rows → duplicate, skip)
  ├─ build report (30 s)
  ├─ upload to media with timeouts, file id = uuid("report:42")             (retry overwrites, never duplicates)
  ├─ success → COMPLETED, resultPath = media:<fileId>
  └─ failure → release claim, throw → retry … → last attempt: FAILED, message kept in DLQ
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

## RabbitMQ vs SQS in this code

| | RabbitMQ (`rabbit`, default) | SQS (`sqs`) |
|---|---|---|
| Send | to an exchange + routing key; bindings pick the queue | straight to a named queue |
| Deliver | broker pushes to the open connection | worker long-polls (`pollTimeoutSeconds = 20`) |
| Done | ack; worker dies → redelivered at once | delete; failure or crash → reappears after the visibility timeout (120 s) |
| Retries | in the app (Spring retry, 5 attempts with backoff) | on the queue (redrive policy `maxReceiveCount: 5`) |
| DLQ | dead-letter exchange → `report-jobs-dlq` (declared in the app) | `report-jobs-dlq` via redrive policy (created in AWS) |
| Mark FAILED | `ReportFailureRecoverer` when retries run out | `SqsReportListener` on `ApproximateReceiveCount >= 5` |

To use SQS: set `APP_QUEUE_TRANSPORT=sqs` and `APP_SQS_ENABLED=true`, give the app AWS credentials and a
region, and create the queues (or point at LocalStack):

```
aws sqs create-queue --queue-name report-jobs-dlq
aws sqs create-queue --queue-name report-jobs --attributes '{
  "VisibilityTimeout": "120",
  "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:<account>:report-jobs-dlq\",\"maxReceiveCount\":\"5\"}"
}'
```

## Known gaps (on purpose, to keep it readable)

- The outbox table is never cleaned up; production would delete sent rows after a few days.
- RabbitMQ publisher confirms are off, so a row can be marked sent before the broker has stored it.
- The read timeout covers waiting for the response, not a server trickling bytes; a hard overall deadline needs a wrapper.
- No heartbeat for SQS jobs longer than the visibility timeout.
- Redriving a FAILED job needs it reset to `PENDING` first, because `FAILED` isn't claimable.
- No alarms in code: alarm on oldest-message age and DLQ depth in CloudWatch / RabbitMQ monitoring.
