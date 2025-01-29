CREATE TABLE transaction (
                             transaction_id SERIAL PRIMARY KEY,
                             account_id INT NOT NULL,
                             transaction_type CHAR(1) CHECK (transaction_type IN ('D', 'C')), -- 'D' for Debit, 'C' for Credit
                             amount NUMERIC(15,2) NOT NULL CHECK (amount > 0), -- Transaction amount must be positive
                             balance_before NUMERIC(15,2) NOT NULL CHECK (balance_before >= 0), -- Balance before transaction
                             balance_after NUMERIC(15,2) NOT NULL CHECK (balance_after >= 0), -- Balance after transaction
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account(account_id) ON DELETE CASCADE
);
