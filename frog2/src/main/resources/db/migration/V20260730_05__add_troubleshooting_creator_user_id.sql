ALTER TABLE troubleshooting
    ADD COLUMN IF NOT EXISTS creator_user_id VARCHAR(100);

UPDATE troubleshooting AS troubleshooting_record
SET creator_user_id = matched_user.user_id
FROM (
    SELECT MIN(userId) AS user_id, userName AS user_name
    FROM company_users
    GROUP BY userName
    HAVING COUNT(*) = 1
) AS matched_user
WHERE troubleshooting_record.creator_user_id IS NULL
  AND troubleshooting_record.creator = matched_user.user_name;

ALTER TABLE troubleshooting
    ALTER COLUMN creator_user_id SET NOT NULL;
