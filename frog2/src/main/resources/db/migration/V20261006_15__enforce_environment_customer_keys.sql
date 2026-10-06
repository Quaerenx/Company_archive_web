-- Requires V20261006_14, a verified backup and a quiesced write window.
-- Stop if duplicate/blank names or duplicate IDs exist. Never deduplicate customer data here.
ALTER TABLE vertica_customer_detail_stg ADD CONSTRAINT pk_customer_detail_stg
    PRIMARY KEY (customer_name) ENABLED;
ALTER TABLE vertica_customer_detail_dev ADD CONSTRAINT pk_customer_detail_dev
    PRIMARY KEY (customer_name) ENABLED;
ALTER TABLE vertica_customer_detail ADD CONSTRAINT uq_customer_detail_id
    UNIQUE (customer_id) ENABLED;
ALTER TABLE vertica_customer_detail_stg ADD CONSTRAINT uq_customer_detail_stg_id
    UNIQUE (customer_id) ENABLED;
ALTER TABLE vertica_customer_detail_dev ADD CONSTRAINT uq_customer_detail_dev_id
    UNIQUE (customer_id) ENABLED;
ALTER TABLE customer_maintenance_schedule ADD CONSTRAINT uq_customer_schedule_id
    UNIQUE (customer_id) ENABLED;
