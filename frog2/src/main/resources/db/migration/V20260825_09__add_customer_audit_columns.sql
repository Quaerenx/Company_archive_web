-- Preconditions:
-- - Review the current vertica_customer_detail definition and confirm that
--   none of these names are used with incompatible types.
-- - Take an approved backup or snapshot and assign a rollback owner.
-- - Deploy application code that tolerates both the old and new schema before
--   applying this artifact.
-- - Quiesce every application node before the first ALTER and keep it stopped
--   until all four statements succeed. Vertica can expose a partially applied
--   schema between statements, and the application deliberately rejects it.
-- - Restart every application node after completion so its schema capability
--   cache observes all four columns together.
--
-- Existing rows intentionally remain NULL. Backfilling an actor or timestamp
-- would create false provenance.

ALTER TABLE vertica_customer_detail
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE vertica_customer_detail
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE vertica_customer_detail
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE vertica_customer_detail
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

-- Rollback is intentionally not executable in this forward migration.
-- A reviewed rollback may drop the four nullable columns only after exporting
-- their values; dropping them permanently destroys customer audit evidence.
