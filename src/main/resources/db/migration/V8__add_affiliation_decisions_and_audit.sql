-- Markedsplassadministratorens beslutninger om tilknytninger (#18, userflow 11).
alter table affiliation
    add column decided_by varchar(255),
    add column decided_at timestamptz,
    add column reason varchar(1000),
    add column version bigint not null default 0;

create index affiliation_pending_idx on affiliation (status, registered_at);

-- Auditspor (0006): bare til å legge til.
create table affiliation_audit (
    id bigint generated always as identity primary key,
    affiliation_id uuid not null references affiliation (id),
    action varchar(16) not null check (action in ('REGISTERED', 'VERIFIED', 'REJECTED', 'WITHDRAWN')),
    actor_subject varchar(255) not null,
    reason varchar(1000),
    occurred_at timestamptz not null
);

create index affiliation_audit_affiliation_idx on affiliation_audit (affiliation_id, occurred_at);

create function prevent_affiliation_audit_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'affiliation_audit is append-only';
end;
$$;

create trigger affiliation_audit_no_update
    before update on affiliation_audit
    for each row
    execute function prevent_affiliation_audit_mutation();

create trigger affiliation_audit_no_delete
    before delete on affiliation_audit
    for each row
    execute function prevent_affiliation_audit_mutation();
