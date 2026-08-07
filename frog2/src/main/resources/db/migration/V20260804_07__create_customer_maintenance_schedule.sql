CREATE TABLE IF NOT EXISTS customer_maintenance_schedule (
    customer_name VARCHAR(100) NOT NULL,
    interval_months INT NOT NULL DEFAULT 1,
    anchor_month DATE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT customer_maintenance_schedule_pk
        PRIMARY KEY (customer_name) ENABLED,
    CONSTRAINT customer_maintenance_schedule_interval
        CHECK (interval_months IN (1, 3)) ENABLED,
    CONSTRAINT customer_maintenance_schedule_dates
        CHECK (effective_to IS NULL OR effective_to >= effective_from) ENABLED
);

INSERT INTO customer_maintenance_schedule (
    customer_name,
    interval_months,
    anchor_month,
    enabled,
    effective_from,
    effective_to,
    updated_by,
    updated_at
)
WITH distinct_inspection_months AS (
    SELECT
        customer_name,
        DATE_TRUNC('month', inspection_date)::DATE AS inspection_month
    FROM maintenance_records
    WHERE customer_name IS NOT NULL
      AND inspection_date IS NOT NULL
    GROUP BY
        customer_name,
        DATE_TRUNC('month', inspection_date)::DATE
),
history_statistics AS (
    SELECT
        customer_name,
        COUNT(*) AS observed_month_count,
        MIN(inspection_month) AS first_month,
        MAX(inspection_month) AS last_month
    FROM distinct_inspection_months
    GROUP BY customer_name
),
residue_statistics AS (
    SELECT
        customer_name,
        MOD(DATEDIFF(month, DATE '2000-01-01', inspection_month), 3)
            AS month_residue,
        COUNT(*) AS residue_month_count,
        MIN(inspection_month) AS first_residue_month
    FROM distinct_inspection_months
    GROUP BY
        customer_name,
        MOD(DATEDIFF(month, DATE '2000-01-01', inspection_month), 3)
),
dominant_residue AS (
    SELECT residue.*
    FROM residue_statistics residue
    JOIN (
        SELECT
            customer_name,
            MAX(residue_month_count) AS dominant_month_count
        FROM residue_statistics
        GROUP BY customer_name
    ) dominant
      ON dominant.customer_name = residue.customer_name
     AND dominant.dominant_month_count = residue.residue_month_count
),
quarterly_candidates AS (
    SELECT
        history.customer_name,
        dominant.first_residue_month AS anchor_month
    FROM history_statistics history
    JOIN dominant_residue dominant
      ON dominant.customer_name = history.customer_name
    WHERE dominant.residue_month_count >= 3
      AND DATEDIFF(month, history.first_month, history.last_month) >= 6
      AND dominant.residue_month_count * 100
            >= history.observed_month_count * 80
)
SELECT
    customer.customer_name,
    CASE WHEN quarterly.customer_name IS NULL THEN 1 ELSE 3 END,
    COALESCE(quarterly.anchor_month, DATE '2000-01-01'),
    TRUE,
    DATE '2000-01-01',
    NULL,
    'migration:V20260804_07',
    CURRENT_TIMESTAMP
FROM vertica_customer_detail customer
LEFT JOIN quarterly_candidates quarterly
  ON quarterly.customer_name = customer.customer_name
WHERE customer.is_deleted = 1
  AND customer.customer_type = '정기점검 계약 고객사'
  AND NOT EXISTS (
      SELECT 1
      FROM customer_maintenance_schedule schedule
      WHERE schedule.customer_name = customer.customer_name
  );
