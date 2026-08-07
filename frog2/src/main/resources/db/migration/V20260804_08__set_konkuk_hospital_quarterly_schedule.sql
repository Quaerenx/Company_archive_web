UPDATE customer_maintenance_schedule
SET
    interval_months = 3,
    anchor_month = DATE '2000-03-01',
    enabled = TRUE,
    effective_to = NULL,
    updated_by = 'manual:V20260804_08',
    updated_at = CURRENT_TIMESTAMP
WHERE customer_name = '건국대병원';
