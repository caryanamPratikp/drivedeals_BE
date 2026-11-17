-- ============================================================================
-- DRIVEDEALS V2: Clean Baseline (No dummy data - live real-time entries only)
-- ============================================================================

-- 1. PLATFORM ADMINISTRATOR (Preserved for administrative operations & console login)
INSERT INTO users (id, name, email, phone, role, city, pincode, status, created_at) VALUES
('admin_user_primary', 'Platform Administrator', 'admin@drivedeals.in', '+919876543210', 'ADMIN', 'Bengaluru', '560001', 'ACTIVE', '2026-01-01 00:00:00')
ON DUPLICATE KEY UPDATE role='ADMIN';

-- All dummy cars, dummy variants, dummy groups, and dummy dealers removed.
-- Tables remain empty (0 rows) until published dynamically by administrators or live users.

