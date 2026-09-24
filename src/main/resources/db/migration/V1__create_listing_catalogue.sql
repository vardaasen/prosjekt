create sequence listing_seq start with 1 increment by 50;

create table listing (
    id bigint primary key default nextval('listing_seq'),
    slug varchar(255) not null unique,
    title varchar(255) not null,
    location varchar(255) not null,
    condition varchar(32) not null,
    category varchar(64) not null,
    publication_status varchar(32) not null,
    price_nok numeric(19, 2) not null check (price_nok >= 0),
    seller_name varchar(255) not null,
    verified_seller boolean not null,
    seller_location varchar(255) not null,
    published_at date not null,
    summary varchar(2000) not null
);

create index listing_published_at_idx on listing (publication_status, published_at desc);
create index listing_location_idx on listing (publication_status, location);

create table listing_documentation (
    listing_id bigint not null references listing (id) on delete cascade,
    documentation varchar(255) not null,
    primary key (listing_id, documentation)
);
