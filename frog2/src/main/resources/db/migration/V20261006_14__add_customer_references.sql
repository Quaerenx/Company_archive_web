-- Requires V20260904_13, a verified backup and a quiesced write window.
-- Run the aggregate-only integrity preflight before executing this file.
-- Preserve unmatched historical names as identities, without adding active customers.
INSERT INTO customer_identity (customer_name)
SELECT names.customer_name FROM (
    SELECT TRIM(customer_name) AS customer_name FROM vertica_customer_detail
    UNION SELECT TRIM(customer_name) FROM vertica_customer_detail_stg
    UNION SELECT TRIM(customer_name) FROM vertica_customer_detail_dev
    UNION SELECT TRIM(customer_name) FROM maintenance_records
    UNION SELECT TRIM(customer_name) FROM troubleshooting
    UNION SELECT TRIM(customer_name) FROM monthly_customer_response
    UNION SELECT TRIM(customer_name) FROM customer_maintenance_schedule
) names
WHERE NULLIF(names.customer_name, '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM customer_identity i WHERE i.customer_name = names.customer_name);

ALTER TABLE maintenance_records ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE monthly_customer_response ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE troubleshooting ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE customer_maintenance_schedule ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE vertica_customer_detail ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE vertica_customer_detail_stg ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE vertica_customer_detail_dev ADD COLUMN IF NOT EXISTS customer_id UUID;

UPDATE maintenance_records SET customer_id = i.customer_id FROM customer_identity i
WHERE maintenance_records.customer_id IS NULL AND i.customer_name = TRIM(maintenance_records.customer_name);
UPDATE monthly_customer_response SET customer_id = i.customer_id FROM customer_identity i
WHERE monthly_customer_response.customer_id IS NULL AND i.customer_name = TRIM(monthly_customer_response.customer_name);
UPDATE troubleshooting SET customer_id = i.customer_id FROM customer_identity i
WHERE troubleshooting.customer_id IS NULL AND i.customer_name = TRIM(troubleshooting.customer_name);
UPDATE customer_maintenance_schedule SET customer_id = i.customer_id FROM customer_identity i
WHERE customer_maintenance_schedule.customer_id IS NULL AND i.customer_name = TRIM(customer_maintenance_schedule.customer_name);
UPDATE vertica_customer_detail SET customer_id = i.customer_id FROM customer_identity i
WHERE vertica_customer_detail.customer_id IS NULL AND i.customer_name = TRIM(vertica_customer_detail.customer_name);
UPDATE vertica_customer_detail_stg SET customer_id = i.customer_id FROM customer_identity i
WHERE vertica_customer_detail_stg.customer_id IS NULL AND i.customer_name = TRIM(vertica_customer_detail_stg.customer_name);
UPDATE vertica_customer_detail_dev SET customer_id = i.customer_id FROM customer_identity i
WHERE vertica_customer_detail_dev.customer_id IS NULL AND i.customer_name = TRIM(vertica_customer_detail_dev.customer_name);

-- Keep IDs nullable for rollback-compatible legacy imports, and audit missing references.
-- Vertica does not enforce foreign keys; application writes resolve IDs transactionally.
