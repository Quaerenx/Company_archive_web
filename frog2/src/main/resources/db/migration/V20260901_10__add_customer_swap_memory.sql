-- Preconditions:
-- - Confirm that the three customer-detail tables exist and that none already
--   defines swap_memory with an incompatible type.
-- - Record aggregate row counts before applying this additive migration.
--
-- Existing rows intentionally remain NULL. The new application accepts NULL,
-- while older application versions ignore this nullable column.

ALTER TABLE vertica_customer_detail
    ADD COLUMN IF NOT EXISTS swap_memory VARCHAR(255);

ALTER TABLE vertica_customer_detail_stg
    ADD COLUMN IF NOT EXISTS swap_memory VARCHAR(255);

ALTER TABLE vertica_customer_detail_dev
    ADD COLUMN IF NOT EXISTS swap_memory VARCHAR(255);

-- Rollback is intentionally not executable in this forward migration.
-- Drop these columns only after confirming that no stored swap-memory values
-- are needed and after separately approving the destructive schema change.
