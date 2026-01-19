-- ============================================================================
-- R__01 - Seed users and roles (Dev/Stage only)
-- ============================================================================
-- Based on CoreMsRoles enum (no SYSTEM or SUPER_ADMIN in mockdata)
-- Password hash is of 'Password123!'
-- ============================================================================

SET search_path TO user_ms;

-- ----------------------------------------------------------------------------
-- Test Users (30 users)
-- ----------------------------------------------------------------------------
INSERT INTO app_user (uuid, provider, email, first_name, last_name, password) VALUES
    -- Admins (5)
    ('20000000-0000-0000-0000-000000000001'::uuid, 'local', 'admin@corems.local', 'Admin', 'User', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000002'::uuid, 'local', 'john.admin@corems.local', 'John', 'Admin', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000003'::uuid, 'local', 'sarah.admin@corems.local', 'Sarah', 'Admin', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000004'::uuid, 'local', 'mike.docadmin@corems.local', 'Mike', 'DocAdmin', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000005'::uuid, 'local', 'lisa.transadmin@corems.local', 'Lisa', 'TransAdmin', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    -- Regular users (25)
    ('20000000-0000-0000-0000-000000000006'::uuid, 'local', 'alice.johnson@corems.local', 'Alice', 'Johnson', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000007'::uuid, 'local', 'bob.wilson@corems.local', 'Bob', 'Wilson', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000008'::uuid, 'local', 'charlie.brown@corems.local', 'Charlie', 'Brown', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000009'::uuid, 'local', 'diana.prince@corems.local', 'Diana', 'Prince', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000010'::uuid, 'local', 'edward.stark@corems.local', 'Edward', 'Stark', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000011'::uuid, 'local', 'fiona.green@corems.local', 'Fiona', 'Green', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000012'::uuid, 'local', 'george.miller@corems.local', 'George', 'Miller', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000013'::uuid, 'local', 'hannah.davis@corems.local', 'Hannah', 'Davis', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000014'::uuid, 'local', 'ivan.petrov@corems.local', 'Ivan', 'Petrov', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000015'::uuid, 'local', 'julia.roberts@corems.local', 'Julia', 'Roberts', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000016'::uuid, 'local', 'kevin.hart@corems.local', 'Kevin', 'Hart', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000017'::uuid, 'local', 'laura.smith@corems.local', 'Laura', 'Smith', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000018'::uuid, 'local', 'marcus.lee@corems.local', 'Marcus', 'Lee', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000019'::uuid, 'local', 'nina.williams@corems.local', 'Nina', 'Williams', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000020'::uuid, 'local', 'oscar.martinez@corems.local', 'Oscar', 'Martinez', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000021'::uuid, 'local', 'peter.parker@corems.local', 'Peter', 'Parker', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000022'::uuid, 'local', 'quinn.taylor@corems.local', 'Quinn', 'Taylor', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000023'::uuid, 'local', 'rachel.adams@corems.local', 'Rachel', 'Adams', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000024'::uuid, 'local', 'steve.rogers@corems.local', 'Steve', 'Rogers', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000025'::uuid, 'local', 'tina.turner@corems.local', 'Tina', 'Turner', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000026'::uuid, 'local', 'uma.watson@corems.local', 'Uma', 'Watson', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000027'::uuid, 'local', 'victor.hugo@corems.local', 'Victor', 'Hugo', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000028'::uuid, 'local', 'wendy.clark@corems.local', 'Wendy', 'Clark', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000029'::uuid, 'local', 'xavier.jones@corems.local', 'Xavier', 'Jones', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq'),
    ('20000000-0000-0000-0000-000000000030'::uuid, 'local', 'yara.silva@corems.local', 'Yara', 'Silva', '$2a$10$qdt5KNdDULqFsZi30vj38ePzMkUi1t2NtHnL3jgpTTk0p3ElLyOoq')
ON CONFLICT (email) DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    updated_at = CURRENT_TIMESTAMP;

-- ----------------------------------------------------------------------------
-- Clear existing roles for test users and reassign
-- ----------------------------------------------------------------------------
DELETE FROM app_user_role WHERE user_id IN (
    SELECT id FROM app_user WHERE email LIKE '%@corems.local'
);

-- ----------------------------------------------------------------------------
-- Role assignments based on CoreMsRoles enum
-- ----------------------------------------------------------------------------

-- Admin user: all admin roles
INSERT INTO app_user_role (user_id, name)
SELECT id, role FROM app_user, (VALUES 
    ('USER_MS_ADMIN'), ('USER_MS_USER'), ('COMMUNICATION_MS_ADMIN'), 
    ('DOCUMENT_MS_ADMIN'), ('TRANSLATION_MS_ADMIN')
) AS roles(role) WHERE email = 'admin@corems.local';

-- John: USER_MS_ADMIN
INSERT INTO app_user_role (user_id, name)
SELECT id, role FROM app_user, (VALUES ('USER_MS_ADMIN'), ('USER_MS_USER')) AS roles(role) 
WHERE email = 'john.admin@corems.local';

-- Sarah: COMMUNICATION_MS_ADMIN
INSERT INTO app_user_role (user_id, name)
SELECT id, role FROM app_user, (VALUES ('COMMUNICATION_MS_ADMIN'), ('COMMUNICATION_MS_USER'), ('USER_MS_USER')) AS roles(role) 
WHERE email = 'sarah.admin@corems.local';

-- Mike: DOCUMENT_MS_ADMIN
INSERT INTO app_user_role (user_id, name)
SELECT id, role FROM app_user, (VALUES ('DOCUMENT_MS_ADMIN'), ('DOCUMENT_MS_USER'), ('USER_MS_USER')) AS roles(role) 
WHERE email = 'mike.docadmin@corems.local';

-- Lisa: TRANSLATION_MS_ADMIN
INSERT INTO app_user_role (user_id, name)
SELECT id, role FROM app_user, (VALUES ('TRANSLATION_MS_ADMIN'), ('USER_MS_USER')) AS roles(role) 
WHERE email = 'lisa.transadmin@corems.local';

-- Regular users: USER_MS_USER + some with other roles
INSERT INTO app_user_role (user_id, name)
SELECT u.id, 'USER_MS_USER' FROM app_user u 
WHERE u.email LIKE '%@corems.local' 
AND u.email NOT IN ('admin@corems.local', 'john.admin@corems.local', 'sarah.admin@corems.local', 
                    'mike.docadmin@corems.local', 'lisa.transadmin@corems.local')
ON CONFLICT DO NOTHING;

-- Give some users DOCUMENT_MS_USER access
INSERT INTO app_user_role (user_id, name)
SELECT id, 'DOCUMENT_MS_USER' FROM app_user 
WHERE email IN ('alice.johnson@corems.local', 'bob.wilson@corems.local', 'charlie.brown@corems.local',
                'diana.prince@corems.local', 'edward.stark@corems.local', 'peter.parker@corems.local',
                'steve.rogers@corems.local', 'xavier.jones@corems.local');

-- Give some users COMMUNICATION_MS_USER access
INSERT INTO app_user_role (user_id, name)
SELECT id, 'COMMUNICATION_MS_USER' FROM app_user 
WHERE email IN ('fiona.green@corems.local', 'george.miller@corems.local', 'hannah.davis@corems.local',
                'ivan.petrov@corems.local', 'julia.roberts@corems.local', 'kevin.hart@corems.local',
                'quinn.taylor@corems.local', 'rachel.adams@corems.local', 'yara.silva@corems.local');

RESET search_path;
