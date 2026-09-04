# Immutable customer identity migration

Date: 2026-09-04
Status: phase 1 implemented; database execution and URL migration remain separate approvals

## Phase 1 contract

`V20260904_13` creates one canonical `customer_identity` row for every nonblank
customer name found in the production, staging or development detail tables.
The UUID is immutable; the name remains a display and compatibility key.

The deployed application remains compatible before and after the migration:

- no identity table: existing name-based behavior continues;
- complete identity table: new customer creation obtains its UUID in the same
  transaction and `CustomerDAO.getCustomerById` becomes available;
- partial identity table: readiness and customer access fail closed;
- existing URLs and child tables remain name-based during this phase.

## Required aggregate-only preflight

The migration window must stop if either query returns rows or a non-zero
count. Do not print customer names into a general deployment log.

```sql
SELECT COUNT(*) AS blank_customer_names
FROM (
    SELECT customer_name FROM vertica_customer_detail
    UNION ALL SELECT customer_name FROM vertica_customer_detail_stg
    UNION ALL SELECT customer_name FROM vertica_customer_detail_dev
) names
WHERE NULLIF(TRIM(customer_name), '') IS NULL;

SELECT COUNT(*) AS case_insensitive_duplicate_groups
FROM (
    SELECT LOWER(TRIM(customer_name)) AS normalized_name
    FROM (
        SELECT customer_name FROM vertica_customer_detail
        UNION SELECT customer_name FROM vertica_customer_detail_stg
        UNION SELECT customer_name FROM vertica_customer_detail_dev
    ) names
    WHERE NULLIF(TRIM(customer_name), '') IS NOT NULL
    GROUP BY LOWER(TRIM(customer_name))
    HAVING COUNT(DISTINCT TRIM(customer_name)) > 1
) duplicates;
```

After applying the migration, verify that the number of distinct trimmed names
equals the identity row count, every ID is non-null, and both enabled
constraints pass `ANALYZE_CONSTRAINTS`.

## Deferred phase 2

Do not replace name-based routes in the same release. A later migration should:

1. add nullable `customer_id` columns to maintenance, troubleshooting, monthly
   response, schedule and environment-detail records;
2. backfill only exact mappings and stop on unmatched names;
3. dual-write IDs while retaining names as snapshots for display;
4. change internal links and lookups to UUIDs;
5. enforce foreign keys/non-null constraints only after reconciliation;
6. migrate filesystem customer-history records with a separately backed-up,
   deterministic reconciliation tool.

This sequencing keeps the current application rollback-compatible and avoids a
single high-risk cross-store cutover.
