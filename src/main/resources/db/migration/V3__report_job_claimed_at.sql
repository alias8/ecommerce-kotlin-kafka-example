-- When a worker claimed the job. Lets a worker take over a job whose previous worker
-- crashed mid-way (claim older than the lease), while still refusing to run a job
-- that another worker is processing right now.
ALTER TABLE public.report_job ADD COLUMN claimed_at timestamp(6) with time zone;
