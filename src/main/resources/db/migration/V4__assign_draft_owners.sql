alter table listing
    add column seller_account_subject varchar(255)
        references seller_account (oidc_subject);

create index listing_seller_account_draft_idx
    on listing (seller_account_subject, publication_status, id desc);
