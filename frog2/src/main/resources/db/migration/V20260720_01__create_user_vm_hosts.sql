CREATE TABLE user_vm_hosts (
    ip VARCHAR(15) PRIMARY KEY,
    owner_user_id VARCHAR(100) NOT NULL,
    owner_user_name VARCHAR(200) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    os_info VARCHAR(100),
    vertica_version VARCHAR(50),
    remote_host VARCHAR(100),
    note VARCHAR(1000),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
