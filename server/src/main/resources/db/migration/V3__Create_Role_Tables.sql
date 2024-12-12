create table role (
    id uuid default gen_random_uuid() primary key,
    app_id uuid,
    name varchar not null,
    description varchar not null
);

create table role_permission (
    role_id uuid,
    permission_id uuid
);

create unique index role_permission_idx ON role_permission (role_id, permission_id);
