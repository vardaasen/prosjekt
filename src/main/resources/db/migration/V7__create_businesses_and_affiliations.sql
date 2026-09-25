-- Virksomhet og tilknytning (beslutningsnotat 0007, userflow 11).
-- Personen lagres bare som Keycloak-subject (dataminimering, 0009).
create table business (
    id uuid primary key,
    organisation_number varchar(9) not null unique check (organisation_number ~ '^[0-9]{9}$'),
    name varchar(255) not null,
    location varchar(255) not null default '',
    registered_at timestamptz not null
);

create table affiliation (
    id uuid primary key,
    person_subject varchar(255) not null,
    business_id uuid not null references business (id),
    status varchar(16) not null check (status in ('PENDING', 'VERIFIED', 'REJECTED', 'WITHDRAWN')),
    registered_at timestamptz not null,
    unique (person_subject, business_id)
);

create index affiliation_business_id_idx on affiliation (business_id);
