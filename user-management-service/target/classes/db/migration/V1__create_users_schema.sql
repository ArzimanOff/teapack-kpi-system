CREATE TABLE IF NOT EXISTS users.roles (
                                           id      BIGSERIAL PRIMARY KEY,
                                           name    VARCHAR(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS users.users (
                                           id            BIGSERIAL PRIMARY KEY,
                                           username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    email         VARCHAR(255),
    role_id       BIGINT NOT NULL REFERENCES users.roles(id),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Начальные роли
INSERT INTO users.roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO users.roles (name) VALUES ('ROLE_OPERATOR');
INSERT INTO users.roles (name) VALUES ('ROLE_TECHNOLOGIST');

-- Начальный администратор (пароль: admin123)
INSERT INTO users.users (username, password_hash, full_name, email, role_id)
VALUES (
           'admin',
           '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
           'Администратор',
           'admin@teapack.ru',
           (SELECT id FROM users.roles WHERE name = 'ROLE_ADMIN')
       );