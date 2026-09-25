-- Annonser eies av virksomheten og publiseres etter annonsegodkjenning
-- (beslutningsnotat 0007, userflow 12, #19 og #22).
alter table listing
    add column business_id uuid references business (id),
    add column created_through_affiliation_id uuid references affiliation (id),
    add column review_feedback varchar(2000),
    add column submitted_at timestamptz,
    add column decided_by varchar(255),
    add column decided_at timestamptz,
    alter column published_at drop not null;

create index listing_business_idx on listing (business_id, publication_status);
create index listing_created_through_affiliation_idx on listing (created_through_affiliation_id);
create index listing_submitted_idx on listing (publication_status, submitted_at);
