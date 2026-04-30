CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    merchant VARCHAR(255),
    category VARCHAR(100),
    confidence_score DECIMAL(5,2),
    is_corrected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);