ALTER TABLE maintenance_records
    ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(100);

UPDATE maintenance_records AS maintenance_record
SET created_by_user_id = matched_user.user_id
FROM (
    SELECT MIN(userId) AS user_id, userName AS user_name
    FROM company_users
    GROUP BY userName
    HAVING COUNT(*) = 1
) AS matched_user
WHERE maintenance_record.created_by_user_id IS NULL
  AND maintenance_record.inspector_name = matched_user.user_name;

ALTER TABLE monthly_customer_response
    ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(100);

UPDATE monthly_customer_response AS response_record
SET created_by_user_id = matched_user.user_id
FROM (
    SELECT MIN(userId) AS user_id, userName AS user_name
    FROM company_users
    GROUP BY userName
    HAVING COUNT(*) = 1
) AS matched_user
WHERE response_record.created_by_user_id IS NULL
  AND response_record.created_by = matched_user.user_name;
