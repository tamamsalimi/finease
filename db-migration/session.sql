CREATE TABLE session (
                         session_id SERIAL PRIMARY KEY,
                         account_id INT NOT NULL,
                         status CHAR(1) DEFAULT 'A' CHECK (status IN ('A', 'I')), -- 'A' for active, 'I' for inactive
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_session_account FOREIGN KEY (account_id) REFERENCES account(account_id) ON DELETE CASCADE
);
