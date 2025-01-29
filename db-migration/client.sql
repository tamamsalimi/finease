CREATE TABLE client (
                        client_id SERIAL PRIMARY KEY,
                        client_name VARCHAR(255) NOT NULL,
                        api_key TEXT UNIQUE NOT NULL,
                        application_id TEXT UNIQUE NOT NULL,
                        status VARCHAR(50) DEFAULT 'active',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
