-- Auditspor for annonser (#19, #22): hvem som laget, endret, sendte inn,
-- godkjente eller sendte tilbake, og når. Bare til å legge til.
create table listing_audit (
    id bigint generated always as identity primary key,
    listing_id bigint not null references listing (id),
    action varchar(40) not null check (action in (
        'CREATED', 'UPDATED', 'SUBMITTED', 'SUBMITTED_FOR_UNVERIFIED_CREATOR', 'APPROVED', 'RETURNED_FOR_CHANGES')),
    actor_subject varchar(255) not null,
    note varchar(2000),
    occurred_at timestamptz not null
);

create index listing_audit_listing_idx on listing_audit (listing_id, occurred_at);

create function prevent_listing_audit_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'listing_audit is append-only';
end;
$$;

create trigger listing_audit_no_update
    before update on listing_audit
    for each row
    execute function prevent_listing_audit_mutation();

create trigger listing_audit_no_delete
    before delete on listing_audit
    for each row
    execute function prevent_listing_audit_mutation();
