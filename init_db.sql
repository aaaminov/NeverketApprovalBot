DROP TABLE IF EXISTS user_state;
DROP TABLE IF EXISTS approval_route;
DROP TABLE IF EXISTS request;
DROP TABLE IF EXISTS request_status;
DROP TABLE IF EXISTS users;

-- Таблица пользователей
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
	is_reviewer bool DEFAULT False,
    first_name VARCHAR(256),
    last_name VARCHAR(256),
    user_name VARCHAR(256)
);

-- Таблица статусов
CREATE TABLE request_status (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL
);

-- Таблица заявок
CREATE TABLE request (
    id SERIAL PRIMARY KEY,
    status_id INT REFERENCES request_status(id),
    user_id BIGINT REFERENCES users(id),
    text TEXT
);

CREATE TABLE approval_route  (
    id SERIAL PRIMARY KEY,
    request_id BIGINT REFERENCES request(id),
	level INT DEFAULT 0,
    reviewer_id BIGINT REFERENCES users(id),
	approval_status INT DEFAULT 0
);

CREATE TABLE user_state  (
    id SERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    state INT DEFAULT 0,
    request_in_progress_id BIGINT REFERENCES request(id),
	current_approval_level INT DEFAULT 0
);

select * from request_status;


INSERT INTO request_status (id, name) VALUES
    (1, 'NEW'),               -- Новая (черновик)
    (2, 'PENDING_APPROVAL'),  -- Ожидает согласования (после /done)
    (3, 'APPROVED'),          -- Полностью согласована
    (4, 'REJECTED'),          -- Отклонена
    (5, 'IN_REVIEW'),         -- В процессе согласования
    (6, 'NEEDS_REVISION');    -- Требует доработки


INSERT INTO users VALUES
(1, 1002202976, false, 'Arslan', null, 'neverket'),
(2, 857862919, true, 'Arslan', null, 'aaaaminov')
;

