CREATE TABLE accounts (
 username VARCHAR(24) PRIMARY KEY,
 display_name VARCHAR(60) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 created_at TIMESTAMP NOT NULL
);
