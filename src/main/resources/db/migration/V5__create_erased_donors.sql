create table erased_donors
(
    channel_hash varchar(64) primary key,
    erased_at    timestamptz not null default now()
);
