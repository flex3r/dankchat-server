create table badge_images
(
    name         varchar(255) primary key,
    content_type varchar(32) not null,
    data         bytea       not null,
    created_at   timestamptz not null default now()
);
