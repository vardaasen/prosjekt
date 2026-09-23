package no.fagskolen.prosjekt.marketplace.service;

import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ListingService {

    private final List<Listing> listings = List.of(
            new Listing(
                    "sentrifugalpumpe-450",
                    "Sentrifugalpumpe 450 m³/t",
                    "Trøndelag",
                    ListingCondition.GOOD,
                    BigDecimal.valueOf(185_000),
                    "Fjord Drift AS",
                    true,
                    LocalDate.of(2026, 9, 1),
                    "Kraftig pumpe fra oppdrettsanlegg, demontert og funksjonstestet før annonsering.",
                    List.of("Servicehistorikk", "CE-dokumentasjon")),
            new Listing(
                    "notpose-160",
                    "Komplett notpose 160 m",
                    "Møre og Romsdal",
                    ListingCondition.USED,
                    BigDecimal.valueOf(92_000),
                    "Kystbruket SA",
                    true,
                    LocalDate.of(2026, 8, 18),
                    "Brukt notpose med dokumentert inspeksjon og reparasjonslogg.",
                    List.of("Inspeksjonsrapport")),
            new Listing(
                    "forflate-24",
                    "Fôrflåte 24 m med silo",
                    "Nordland",
                    ListingCondition.GOOD,
                    BigDecimal.valueOf(1_250_000),
                    "Nordhav Utstyr",
                    false,
                    LocalDate.of(2026, 8, 4),
                    "Komplett fôrflåte for videre vurdering. Selger kan levere mer dokumentasjon ved forespørsel.",
                    List.of("Bildepakke")));

    public List<Listing> findPublicListings() {
        return listings;
    }

    public Optional<Listing> findBySlug(String slug) {
        return listings.stream()
                .filter(listing -> listing.slug().equals(slug))
                .findFirst();
    }
}
