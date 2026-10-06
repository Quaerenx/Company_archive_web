# Immutable customer identity migration

Date: 2026-09-04
Status: phase 1 and phase 2 database relationships applied and verified

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

## Phase 2 database relationships

The 2026-10-06 release implements nullable child/environment UUID columns,
exact-name backfill, transactional dual-write and UUID-based customer history
lookups. Historical names absent from the master receive identity-only records;
they are preserved and never promoted to active customers. See
[integrity-improvements.md](integrity-improvements.md) for the four migrations,
verification, retention policy and rollout/rollback procedure.

## Separate route and filesystem migration

Do not replace name-based public routes in the database release. Remaining work:

1. replace public name-based links with UUIDs while retaining compatible routes;
2. require non-null UUIDs after legacy importers are reconciled (Vertica does not
   enforce foreign keys; keep application checks and integrity audits);
3. migrate filesystem customer-history records with a separately backed-up,
   deterministic reconciliation tool.

This sequencing keeps the current application rollback-compatible and avoids a
single high-risk cross-store cutover.
