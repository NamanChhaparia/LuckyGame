-- Sample data for testing and demonstration
-- This file is automatically executed on application startup

-- Insert sample brands
INSERT INTO brands (name, wallet_balance, daily_spend_limit, is_active, created_at, updated_at) 
VALUES 
    ('Nike', 10000.00, 5000.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Adidas', 8000.00, 4000.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Puma', 6000.00, 3000.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample users
INSERT INTO users (username, email, full_name, is_active, created_at, updated_at) 
VALUES 
    ('john_doe', 'john@example.com', 'John Doe', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('jane_smith', 'jane@example.com', 'Jane Smith', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bob_wilson', 'bob@example.com', 'Bob Wilson', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample vouchers for Nike
INSERT INTO vouchers (voucher_code, description, cost, initial_quantity, current_quantity, brand_id, is_active, created_at, updated_at) 
VALUES 
    ('NIKE50', '50% off on Nike products', 5.00, 100, 100, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('NIKE100', '$100 Nike Gift Card', 10.00, 50, 50, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('NIKEFREE', 'Free Nike T-Shirt', 3.00, 200, 200, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample vouchers for Adidas
INSERT INTO vouchers (voucher_code, description, cost, initial_quantity, current_quantity, brand_id, is_active, created_at, updated_at) 
VALUES 
    ('ADIDAS40', '40% off on Adidas products', 4.00, 100, 100, 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADIDAS75', '$75 Adidas Gift Card', 7.50, 60, 60, 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample vouchers for Puma
INSERT INTO vouchers (voucher_code, description, cost, initial_quantity, current_quantity, brand_id, is_active, created_at, updated_at) 
VALUES 
    ('PUMA30', '30% off on Puma products', 3.00, 150, 150, 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PUMACAP', 'Free Puma Cap', 2.50, 200, 200, 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

