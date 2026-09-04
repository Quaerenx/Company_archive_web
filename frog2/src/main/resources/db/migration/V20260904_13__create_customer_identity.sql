-- Preconditions:
-- - V20260904_12 is applied and all earlier migrations are reconciled.
-- - Review blank and duplicate customer names across all three environments.
-- - Take an approved database backup or snapshot.
-- - Quiesce customer writes while this table is created and backfilled.

CREATE TABLE customer_identity (
    customer_id UUID DEFAULT UUID_GENERATE() NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_customer_identity PRIMARY KEY (customer_id) ENABLED,
    CONSTRAINT uq_customer_identity_name UNIQUE (customer_name) ENABLED
);

INSERT INTO customer_identity (customer_name)
SELECT DISTINCT names.customer_name
FROM (
    SELECT TRIM(customer_name) AS customer_name
    FROM vertica_customer_detail
    UNION
    SELECT TRIM(customer_name) AS customer_name
    FROM vertica_customer_detail_stg
    UNION
    SELECT TRIM(customer_name) AS customer_name
    FROM vertica_customer_detail_dev
) AS names
WHERE NULLIF(names.customer_name, '') IS NOT NULL;

-- Postconditions:
-- - Every nonblank customer name across all environments maps to one UUID.
-- - No existing business table row is changed by this additive phase.
-- - URL and child-table foreign-key migration remains a separately reviewed phase.
