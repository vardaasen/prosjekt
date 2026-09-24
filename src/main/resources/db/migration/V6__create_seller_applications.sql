create table seller_application (
    id uuid primary key,
    oidc_subject varchar(255) not null,
    seller_name varchar(255) not null,
    seller_location varchar(255) not null,
    status varchar(32) not null,
    submitted_at timestamp with time zone not null,
    decided_at timestamp with time zone,
    decided_by varchar(255),
    rejection_reason varchar(1000),
    version bigint not null default 0
);

create index seller_application_pending_idx
    on seller_application (status, submitted_at);

create unique index seller_application_one_pending_per_subject_idx
    on seller_application (oidc_subject)
    where status = 'PENDING';

create table seller_application_audit (
    id bigint generated always as identity primary key,
    application_id uuid not null references seller_application (id),
    actor_subject varchar(255) not null,
    action varchar(64) not null,
    occurred_at timestamp with time zone not null
);

create index seller_application_audit_application_idx
    on seller_application_audit (application_id, occurred_at);

create function prevent_seller_application_audit_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'seller_application_audit is append-only';
end;
$$;

create trigger seller_application_audit_no_update
    before update on seller_application_audit
    for each row
    execute function prevent_seller_application_audit_mutation();

create trigger seller_application_audit_no_delete
    before delete on seller_application_audit
    for each row
    execute function prevent_seller_application_audit_mutation();
