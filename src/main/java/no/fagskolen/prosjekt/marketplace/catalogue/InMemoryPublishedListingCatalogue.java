package no.fagskolen.prosjekt.marketplace.catalogue;

import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.Seller;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

@Component
public class InMemoryPublishedListingCatalogue implements PublishedListingCatalogue {

    private final List<Listing> listings = List.of(
            new Listing(
                    "sentrifugalpumpe-450",
                    "Sentrifugalpumpe 450 m³/t",
                    "Trøndelag",
                    ListingCondition.GOOD,
                    EquipmentCategory.PUMP,
                    BigDecimal.valueOf(185_000),
                    new Seller("Fjord Drift AS", true, "Trøndelag"),
                    LocalDate.of(2026, 9, 1),
                    "Kraftig pumpe fra oppdrettsanlegg, demontert og funksjonstestet før annonsering.",
                    List.of("Servicehistorikk", "CE-dokumentasjon")),
            new Listing(
                    "notpose-160",
                    "Komplett notpose 160 m",
                    "Møre og Romsdal",
                    ListingCondition.USED,
                    EquipmentCategory.NET,
                    BigDecimal.valueOf(92_000),
                    new Seller("Kystbruket SA", true, "Møre og Romsdal"),
                    LocalDate.of(2026, 8, 18),
                    "Brukt notpose med dokumentert inspeksjon og reparasjonslogg.",
                    List.of("Inspeksjonsrapport")),
            new Listing(
                    "forflate-24",
                    "Fôrflåte 24 m med silo",
                    "Nordland",
                    ListingCondition.GOOD,
                    EquipmentCategory.FLOATING_STRUCTURE,
                    BigDecimal.valueOf(1_250_000),
                    new Seller("Nordhav Utstyr", false, "Nordland"),
                    LocalDate.of(2026, 8, 4),
                    "Komplett fôrflåte for videre vurdering. Selger kan levere mer dokumentasjon ved forespørsel.",
                    List.of("Bildepakke")));

    @Override
    public List<Listing> findPublishedListings() {
        return listings;
    }

    @Override
    public List<Listing> searchPublishedListings(String query, String location, ListingCondition condition) {
        var normalizedQuery = normalize(query);
        var normalizedLocation = normalize(location);

        return listings.stream()
                .filter(listing -> normalizedQuery.isBlank()
                        || normalize(listing.title()).contains(normalizedQuery)
                        || normalize(listing.summary()).contains(normalizedQuery)
                        || normalize(listing.sellerName()).contains(normalizedQuery))
                .filter(listing -> normalizedLocation.isBlank()
                        || normalize(listing.location()).equals(normalizedLocation))
                .filter(listing -> condition == null || listing.condition() == condition)
                .toList();
    }

    @Override
    public Optional<Listing> findPublishedBySlug(String slug) {
        return listings.stream()
                .filter(listing -> listing.slug().equals(slug))
                .findFirst();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
