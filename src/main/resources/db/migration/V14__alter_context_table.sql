ALTER TABLE server_execution_context_
DROP COLUMN region,
DROP COLUMN specification_id,
ADD COLUMN recreate BOOLEAN NOT NULL DEFAULT FALSE;