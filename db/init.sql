-- Ensure sequences are created after tables are ready
DO $$
BEGIN
    -- Create sequence for account
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'account_sequence') THEN
CREATE SEQUENCE account_sequence START 1;
END IF;

    -- Create sequence for owed_transaction
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'owed_transaction_seq') THEN
CREATE SEQUENCE owed_transaction_seq START 1;
END IF;

    -- Create sequence for transaction
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'transaction_seq') THEN
CREATE SEQUENCE transaction_seq START 1;
END IF;
END $$;
