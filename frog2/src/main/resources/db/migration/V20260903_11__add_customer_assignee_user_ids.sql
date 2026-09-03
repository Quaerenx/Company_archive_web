-- Preconditions:
-- - Confirm that company_users.userId is the stable login identifier.
-- - Review duplicate display names; only a case-insensitive unique name is
--   eligible for automatic backfill.
-- - Quiesce application writes while both columns are added and backfilled.
--
-- Nullable columns preserve customers without an assigned user and ambiguous
-- legacy display names. The application rejects a one-column partial state.

ALTER TABLE vertica_customer_detail
    ADD COLUMN IF NOT EXISTS main_manager_user_id VARCHAR(100);

ALTER TABLE vertica_customer_detail
    ADD COLUMN IF NOT EXISTS sub_manager_user_id VARCHAR(100);

UPDATE vertica_customer_detail AS customer
SET main_manager_user_id = matched_user.user_id
FROM (
    SELECT
        MIN(userId) AS user_id,
        LOWER(TRIM(userName)) AS normalized_user_name
    FROM company_users
    WHERE NULLIF(TRIM(userName), '') IS NOT NULL
    GROUP BY LOWER(TRIM(userName))
    HAVING COUNT(*) = 1
) AS matched_user
WHERE customer.main_manager_user_id IS NULL
  AND NULLIF(TRIM(customer.main_manager), '') IS NOT NULL
  AND LOWER(TRIM(customer.main_manager))
        = matched_user.normalized_user_name;

UPDATE vertica_customer_detail AS customer
SET sub_manager_user_id = matched_user.user_id
FROM (
    SELECT
        MIN(userId) AS user_id,
        LOWER(TRIM(userName)) AS normalized_user_name
    FROM company_users
    WHERE NULLIF(TRIM(userName), '') IS NOT NULL
    GROUP BY LOWER(TRIM(userName))
    HAVING COUNT(*) = 1
) AS matched_user
WHERE customer.sub_manager_user_id IS NULL
  AND NULLIF(TRIM(customer.sub_manager), '') IS NOT NULL
  AND LOWER(TRIM(customer.sub_manager))
        = matched_user.normalized_user_name;

-- Postconditions:
-- - Review nonblank manager names whose corresponding user-ID column remains
--   NULL; they are intentionally not guessed.
-- - Restart every application node so schema capability caches are refreshed.
--
-- Rollback is intentionally not executable here. Export both ID columns and
-- separately approve a destructive DROP COLUMN operation if rollback is ever
-- required.
