-- Auth Center schema for PostgreSQL.
-- Prod uses ddl-auto=validate and does not create tables.
-- Apply once: psql "$DB_URL" -f server/src/main/resources/schema.sql
-- Local boot can keep ddl-auto=update and spring.session.jdbc.initialize-schema=always.

CREATE TABLE IF NOT EXISTS users (
    id              uuid         NOT NULL,
    email           varchar(255) NOT NULL,
    name            varchar(255),
    image           varchar(1024),
    email_verified  boolean      NOT NULL DEFAULT false,
    role            varchar(255) NOT NULL DEFAULT 'user',
    banned          boolean      NOT NULL DEFAULT false,
    ban_reason      varchar(255),
    ban_expires     timestamp(6) with time zone,
    google_sub      varchar(255),
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_google_sub_key UNIQUE (google_sub)
);

CREATE TABLE IF NOT EXISTS oauth_clients (
    id            uuid          NOT NULL,
    client_id     varchar(255)  NOT NULL,
    client_secret varchar(255)  NOT NULL,
    redirect_uri  varchar(1024) NOT NULL,
    name          varchar(255)  NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL,
    CONSTRAINT oauth_clients_pkey PRIMARY KEY (id),
    CONSTRAINT oauth_clients_client_id_key UNIQUE (client_id)
);

CREATE TABLE IF NOT EXISTS authorization_codes (
    id             uuid         NOT NULL,
    code           varchar(255) NOT NULL,
    client_id      varchar(255) NOT NULL,
    user_id        uuid,
    redirect_uri   varchar(255) NOT NULL,
    code_challenge varchar(255) NOT NULL,
    expires_at     timestamp(6) with time zone NOT NULL,
    used           boolean      NOT NULL DEFAULT false,
    CONSTRAINT authorization_codes_pkey PRIMARY KEY (id),
    CONSTRAINT authorization_codes_code_key UNIQUE (code)
);

-- Spring Session JDBC 4.1 (org/springframework/session/jdbc/schema-postgresql.sql).
-- Unquoted names fold to lowercase, which is what the session queries use.

CREATE TABLE IF NOT EXISTS spring_session (
    primary_id            char(36)     NOT NULL,
    session_id            char(36)     NOT NULL,
    creation_time         bigint       NOT NULL,
    last_access_time      bigint       NOT NULL,
    max_inactive_interval integer      NOT NULL,
    expiry_time           bigint       NOT NULL,
    principal_name        varchar(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS spring_session_ix1 ON spring_session (session_id);
CREATE INDEX IF NOT EXISTS spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX IF NOT EXISTS spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE IF NOT EXISTS spring_session_attributes (
    session_primary_id char(36)     NOT NULL,
    attribute_name     varchar(200) NOT NULL,
    attribute_bytes    bytea        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
