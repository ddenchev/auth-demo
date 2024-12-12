create table resource(
    id uuid primary key,
    name varchar not null unique,
    description varchar not null,
    key serial
);

create table app_resource(
    app_id uuid,
    resource_id uuid,

    primary key (app_id, resource_id)
)