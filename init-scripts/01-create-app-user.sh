#!/bin/bash
# init-scripts/01-create-app-user.sh

set -e

echo "Creating application user with Liquibase permissions..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Создаем пользователя приложения (если не существует)
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'app_user') THEN
            CREATE USER app_user WITH PASSWORD 'app_password_123';
        END IF;
    END
    \$\$;

    -- Даем права на схему public (включая CREATE для Liquibase)
    GRANT CONNECT ON DATABASE userservice TO app_user;
    GRANT USAGE, CREATE ON SCHEMA public TO app_user;

    -- Даем права на все существующие таблицы
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;

    -- Даем права на последовательности
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

    -- Для будущих таблиц (Liquibase создает свои таблицы)
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO app_user;

    -- Дополнительно: даем права на создание таблиц для Liquibase
    GRANT CREATE ON SCHEMA public TO app_user;

    SELECT 'User app_user created/verified with CREATE permission' as status;
EOSQL

echo "Init script completed successfully!"