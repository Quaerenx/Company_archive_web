-- Requires a verified backup and a quiesced write window.
-- The historical license_size_gb column stores TB, despite its name.
-- Retain all raw strings; unsupported legacy units remain visible with numeric NULL.
ALTER TABLE maintenance_records ADD COLUMN IF NOT EXISTS license_capacity_tb NUMERIC(18,6);
ALTER TABLE maintenance_records ADD COLUMN IF NOT EXISTS license_used_tb NUMERIC(18,6);
ALTER TABLE maintenance_records ADD COLUMN IF NOT EXISTS license_usage_pct_value NUMERIC(18,6);

UPDATE maintenance_records SET license_capacity_tb =
    CASE WHEN REGEXP_LIKE(UPPER(TRIM(license_size_gb)), 'GB$')
         THEN ROUND(CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_size_gb), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_size_gb), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6)) / 1024, 6)
         ELSE CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_size_gb), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_size_gb), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6)) END
WHERE license_capacity_tb IS NULL
  AND REGEXP_LIKE(UPPER(TRIM(license_size_gb)), '^[+-]?[0-9]+([.][0-9]{1,6})?[[:space:]]*(TB|GB)?$')
  AND CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_size_gb), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_size_gb), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6)) BETWEEN 0 AND 1000000;

UPDATE maintenance_records SET license_used_tb =
    CASE WHEN REGEXP_LIKE(UPPER(TRIM(license_usage_size)), 'GB$')
         THEN ROUND(CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_usage_size), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_usage_size), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6)) / 1024, 6)
         ELSE CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_usage_size), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_usage_size), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6)) END
WHERE license_used_tb IS NULL
  AND REGEXP_LIKE(UPPER(TRIM(license_usage_size)), '^[+-]?[0-9]+([.][0-9]{1,6})?[[:space:]]*(TB|GB)?$')
  AND CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_usage_size), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_usage_size), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6)) BETWEEN 0 AND 1000000;

UPDATE maintenance_records SET license_usage_pct_value =
    CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_usage_pct), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_usage_pct), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6))
WHERE license_usage_pct_value IS NULL
  AND REGEXP_LIKE(TRIM(license_usage_pct), '^[+-]?[0-9]+([.][0-9]{1,6})?[[:space:]]*%?$')
  AND CAST(CASE WHEN LENGTH(REGEXP_SUBSTR(TRIM(license_usage_pct), '^[+-]?[0-9]+')) <= 8 THEN REGEXP_SUBSTR(TRIM(license_usage_pct), '^[+-]?[0-9]+([.][0-9]{1,6})?') END AS NUMERIC(18,6)) BETWEEN 0 AND 1000000;

ALTER TABLE maintenance_records ADD CONSTRAINT ck_license_capacity_tb
    CHECK (license_capacity_tb BETWEEN 0 AND 1000000) ENABLED;
ALTER TABLE maintenance_records ADD CONSTRAINT ck_license_used_tb
    CHECK (license_used_tb BETWEEN 0 AND 1000000) ENABLED;
ALTER TABLE maintenance_records ADD CONSTRAINT ck_license_usage_pct_value
    CHECK (license_usage_pct_value BETWEEN 0 AND 1000000) ENABLED;
