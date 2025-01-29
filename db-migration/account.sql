CREATE TABLE account (
                         account_id SERIAL PRIMARY KEY,
                         account_ref TEXT UNIQUE NOT NULL,  -- Unique identifier for external use
                         account_name VARCHAR(255) NOT NULL,
                         client_id INT NOT NULL,
                         status CHAR(1) DEFAULT 'I' CHECK (status IN ('I', 'A')),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE account
    ADD COLUMN balance NUMERIC(15,2) DEFAULT 0 CHECK (balance >= 0);