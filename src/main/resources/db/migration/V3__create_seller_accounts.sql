create table seller_account (
    oidc_subject varchar(255) primary key,
    seller_name varchar(255) not null,
    verified_seller boolean not null default false,
    seller_location varchar(255) not null default ''
);
