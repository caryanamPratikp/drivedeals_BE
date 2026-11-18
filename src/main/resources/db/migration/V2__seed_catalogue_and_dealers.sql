-- ============================================================================
-- DRIVEDEALS V2: Clean Baseline (No dummy data - live real-time entries only)
-- ============================================================================

-- 1. PLATFORM ADMINISTRATOR (Preserved for administrative operations & console login)
INSERT INTO users (id, name, email, phone, role, city, pincode, status, created_at) VALUES
('admin_user_primary', 'Platform Administrator', 'admin.919665879804@motorcircle.in', '+919665879804', 'ADMIN', 'Pune', '411001', 'ACTIVE', '2026-01-01 00:00:00')
ON DUPLICATE KEY UPDATE role='ADMIN', status='ACTIVE';

-- All dummy cars, dummy variants, dummy groups, and dummy dealers removed.
-- Tables remain empty (0 rows) until published dynamically by administrators or live users.
