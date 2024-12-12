create type user_status AS enum (
    'PENDING',
    'VERIFIED',
    'DEACTIVATED'
);

create table app_user (
    id uuid default gen_random_uuid() primary key,
    app_id uuid not null,
    username varchar not null,
    first_name varchar,
    last_name varchar,
    email varchar not null,
    user_status user_status not null
);

create index app_user_id_app_id_idx on app_user (app_id, id);
create index app_user_id_app_username_idx on app_user (app_id, username);

create table app_user_credentials (
    user_id uuid primary key,
    password_hash varchar,
    password_salt varchar not null
);

create table app_user_role (
    user_id uuid not null,
    role_id uuid not null,
    PRIMARY KEY (user_id, role_id)
);

create table app_user_password_reset_request (
    password_reset_token varchar primary key,
    user_id uuid not null,
    expiry timestamp not null
);
