ALTER TABLE server_execution_context_
DROP COLUMN region,
DROP COLUMN specification_id,
ADD COLUMN recreate BOOLEAN NOT NULL DEFAULT FALSE;

-- drop the fk constraint and index for specification_id
ALTER TABLE server_execution_context_ DROP CONSTRAINT fk_server_execution_context_specification_id;
DROP INDEX idx_server_execution_context_specification_id;