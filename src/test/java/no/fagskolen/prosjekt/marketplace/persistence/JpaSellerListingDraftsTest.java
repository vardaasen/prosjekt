package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.accounts.SellerAccountRegistry;
import no.fagskolen.prosjekt.marketplace.drafts.ListingDraft;
import no.fagskolen.prosjekt.marketplace.drafts.SellerListingDrafts;
import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.catalogue.PublishedListingCatalogue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class JpaSellerListingDraftsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private SellerAccountRegistry sellerAccounts;

    @Autowired
    private SellerListingDrafts sellerListingDrafts;

    @Autowired
    private PublishedListingCatalogue publishedListings;

    @Test
    void createsDraftsForTheirRegisteredSellerOnly() {
        var firstSeller = sellerAccounts.registerSellerAccount("owner-alpha", "Fjord Drift AS");
        var secondSeller = sellerAccounts.registerSellerAccount("owner-beta", "Kystbruket SA");

        var draft = sellerListingDrafts.createDraft(firstSeller, draft("pumpeutkast"));

        assertThat(draft.publicationStatus()).isEqualTo(no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus.DRAFT);
        assertThat(draft.sellerName()).isEqualTo("Fjord Drift AS");
        assertThat(sellerListingDrafts.findDrafts(firstSeller))
                .extracting("slug")
                .containsExactly("pumpeutkast");
        assertThat(sellerListingDrafts.findDrafts(secondSeller)).isEmpty();
    }

    @Test
    void preventsDuplicateSlugsAcrossSellerAccounts() {
        var firstSeller = sellerAccounts.registerSellerAccount("owner-gamma", "Fjord Drift AS");
        var secondSeller = sellerAccounts.registerSellerAccount("owner-delta", "Kystbruket SA");
        sellerListingDrafts.createDraft(firstSeller, draft("shared-slug"));

        assertThatThrownBy(() -> sellerListingDrafts.createDraft(secondSeller, draft("shared-slug")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A listing with this slug already exists");
    }

    @Test
    void publishesOnlyTheOwnersDraftToThePublicCatalogue() {
        var owner = sellerAccounts.registerSellerAccount("owner-epsilon", "Fjord Drift AS");
        var otherSeller = sellerAccounts.registerSellerAccount("owner-zeta", "Kystbruket SA");
        sellerListingDrafts.createDraft(owner, draft("publiserbar-pumpe"));

        assertThat(publishedListings.findPublishedBySlug("publiserbar-pumpe")).isEmpty();
        assertThatThrownBy(() -> sellerListingDrafts.publishDraft(otherSeller, "publiserbar-pumpe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Draft is not available for this seller");

        var published = sellerListingDrafts.publishDraft(owner, "publiserbar-pumpe");

        assertThat(published.publicationStatus())
                .isEqualTo(no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus.PUBLISHED);
        assertThat(publishedListings.findPublishedBySlug("publiserbar-pumpe"))
                .contains(published);
        assertThat(sellerListingDrafts.findDrafts(owner)).isEmpty();
    }

    private static ListingDraft draft(String slug) {
        return new ListingDraft(
                slug,
                "Sentrifugalpumpe",
                "Trøndelag",
                ListingCondition.GOOD,
                EquipmentCategory.PUMP,
                BigDecimal.valueOf(150_000),
                "Klargjort for inspeksjon.");
    }
}
