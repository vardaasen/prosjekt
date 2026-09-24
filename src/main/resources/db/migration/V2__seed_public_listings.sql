insert into listing (
    slug,
    title,
    location,
    condition,
    category,
    publication_status,
    price_nok,
    seller_name,
    verified_seller,
    seller_location,
    published_at,
    summary
) values
    (
        'sentrifugalpumpe-450',
        'Sentrifugalpumpe 450 m³/t',
        'Trøndelag',
        'GOOD',
        'PUMP',
        'PUBLISHED',
        185000.00,
        'Fjord Drift AS',
        true,
        'Trøndelag',
        date '2026-09-01',
        'Kraftig pumpe fra oppdrettsanlegg, demontert og funksjonstestet før annonsering.'
    ),
    (
        'notpose-160',
        'Komplett notpose 160 m',
        'Møre og Romsdal',
        'USED',
        'NET',
        'PUBLISHED',
        92000.00,
        'Kystbruket SA',
        true,
        'Møre og Romsdal',
        date '2026-08-18',
        'Brukt notpose med dokumentert inspeksjon og reparasjonslogg.'
    ),
    (
        'forflate-24',
        'Fôrflåte 24 m med silo',
        'Nordland',
        'GOOD',
        'FLOATING_STRUCTURE',
        'PUBLISHED',
        1250000.00,
        'Nordhav Utstyr',
        false,
        'Nordland',
        date '2026-08-04',
        'Komplett fôrflåte for videre vurdering. Selger kan levere mer dokumentasjon ved forespørsel.'
    );

insert into listing_documentation (listing_id, documentation)
select id, 'Servicehistorikk' from listing where slug = 'sentrifugalpumpe-450'
union all
select id, 'CE-dokumentasjon' from listing where slug = 'sentrifugalpumpe-450'
union all
select id, 'Inspeksjonsrapport' from listing where slug = 'notpose-160'
union all
select id, 'Bildepakke' from listing where slug = 'forflate-24';
