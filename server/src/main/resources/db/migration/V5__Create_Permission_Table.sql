create table permission (
    id uuid primary key,
    resource_id uuid references resource(id),
    action varchar not null,
    description varchar,
    key serial,

    unique(resource_id, action)
);