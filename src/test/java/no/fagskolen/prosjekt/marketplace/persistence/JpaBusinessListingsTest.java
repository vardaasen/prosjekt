package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.catalogue.PublishedListingCatalogue;
import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.ListingContent;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListing;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListings;
import no.fagskolen.prosjekt.marketplace.listings.ListingAuditEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Userflow 12: fra utkast til publisert annonse, eid av virksomheten (0007).
 */
@SpringBootTest
@Testcontainers
class JpaBusinessListingsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private Affiliations affiliations;

    @Autowired
    private BusinessListings listings;

    @Autowired
    private PublishedListingCatalogue catalogue;

    @Test
    void aPersonWithAPendingAffiliationCannotDraft() {
        var affiliation = affiliations.register("drafting-person", organisationNumber("92100000"), "Kyst Pumpe AS", "Bodø");

        assertThatIllegalStateException()
                .isThrownBy(() -> listings.createDraft("drafting-person", affiliation.business().id(), content("Pumpe")))
                .withMessage("Tilknytningen din til virksomheten må være verifisert før du kan lage eller endre en annonse.");

        affiliations.verify(affiliation.id(), "admin-one");
        var draft = listings.createDraft("drafting-person", affiliation.business().id(), content("Sentrifugalpumpe 450"));

        assertThat(draft.status()).isEqualTo(ListingPublicationStatus.DRAFT);
        assertThat(draft.businessName()).isEqualTo("Kyst Pumpe AS");
        assertThat(draft.slug()).startsWith("sentrifugalpumpe-450-");
        assertThat(listings.findForPerson("drafting-person")).extracting(BusinessListing::id).containsExactly(draft.id());
    }

    @Test
    void onlyPersonsAffiliatedWithTheBusinessSeeAndChangeItsListings() {
        var owner = affiliations.register("first-colleague", organisationNumber("92100001"), "Merd Service AS", "Sula");
        affiliations.verify(owner.id(), "admin-one");
        var colleague = affiliations.register("second-colleague", owner.business().organisationNumber(), "", "");
        affiliations.verify(colleague.id(), "admin-one");
        affiliations.register("outsider", organisationNumber("92100002"), "Annen AS", "Molde");
        var draft = listings.createDraft("first-colleague", owner.business().id(), content("Notpose 160 m"));

        assertThat(colleague.business()).isEqualTo(owner.business());
        assertThat(listings.findForPerson("second-colleague")).extracting(BusinessListing::id).contains(draft.id());
        assertThat(listings.findForPerson("outsider")).extracting(BusinessListing::id).doesNotContain(draft.id());
        assertThatIllegalStateException()
                .isThrownBy(() -> listings.createDraft("outsider", owner.business().id(), content("Snik")));
        assertThatIllegalStateException()
                .isThrownBy(() -> listings.submitForApproval("outsider", draft.id(), null));
    }

    @Test
    void aRejectedAffiliationCannotDraft() {
        var affiliation = affiliations.register("rejected-drafter", organisationNumber("92100003"), "Avvist AS", "Hitra");
        affiliations.reject(affiliation.id(), "admin-one", "Ingen kobling.");

        assertThatIllegalStateException()
                .isThrownBy(() -> listings.createDraft("rejected-drafter", affiliation.business().id(), content("Nei")));
    }

    @Test
    void anApprovedListingIsPublishedWithTheBusinessAsSeller() {
        var affiliation = affiliations.register("approved-seller", organisationNumber("92100004"), "Fôr og Flåte AS", "Frøya");
        affiliations.verify(affiliation.id(), "admin-one");
        var draft = listings.createDraft("approved-seller", affiliation.business().id(), content("Fôrflåte 24 m"));

        var submitted = listings.submitForApproval("approved-seller", draft.id(), null);
        assertThat(submitted.status()).isEqualTo(ListingPublicationStatus.SUBMITTED);
        assertThat(listings.findAwaitingApproval()).extracting(BusinessListing::id).contains(draft.id());
        assertThat(catalogue.findPublishedBySlug(draft.slug())).isEmpty();

        var published = listings.approve(draft.id(), "admin-one");

        assertThat(published.status()).isEqualTo(ListingPublicationStatus.PUBLISHED);
        assertThat(listings.findAwaitingApproval()).extracting(BusinessListing::id).doesNotContain(draft.id());
        assertThat(catalogue.findPublishedBySlug(draft.slug()))
                .hasValueSatisfying(listing -> assertThat(listing.seller().name()).isEqualTo("Fôr og Flåte AS"));
    }

    @Test
    void theAdministratorCanAskForChangesWithFeedback() {
        var affiliation = affiliations.register("revising-seller", organisationNumber("92100005"), "Rett AS", "Vikna");
        affiliations.verify(affiliation.id(), "admin-one");
        var draft = listings.createDraft("revising-seller", affiliation.business().id(), content("Pumpe"));
        listings.submitForApproval("revising-seller", draft.id(), null);

        var returned = listings.returnForChanges(draft.id(), "admin-one", "Beskriv tilstanden nærmere.");

        assertThat(returned.status()).isEqualTo(ListingPublicationStatus.DRAFT);
        assertThat(listings.findForPerson("revising-seller"))
                .filteredOn(listing -> listing.id() == draft.id())
                .extracting(BusinessListing::feedback)
                .containsExactly("Beskriv tilstanden nærmere.");
    }

    @Test
    void theCatalogueShowsTheMostRecentlyPublishedListingFirst() {
        var affiliation = affiliations.register("busy-seller", organisationNumber("92100006"), "Flink AS", "Bergen");
        affiliations.verify(affiliation.id(), "admin-one");
        var first = publish("busy-seller", affiliation.business().id(), "Første annonse");
        var second = publish("busy-seller", affiliation.business().id(), "Andre annonse");

        var slugs = catalogue.findPublishedListings().stream().map(listing -> listing.slug()).toList();
        assertThat(slugs.indexOf(second.slug())).isLessThan(slugs.indexOf(first.slug()));
    }

    @Test
    void aDraftCanBeEditedAndTheReturnedFeedbackStaysUntilItIsResubmitted() {
        var affiliation = affiliations.register("editing-seller", organisationNumber("92100010"), "Rediger AS", "Bodø");
        affiliations.verify(affiliation.id(), "admin-one");
        var draft = listings.createDraft("editing-seller", affiliation.business().id(), content("Pumpe"));
        listings.submitForApproval("editing-seller", draft.id(), null);
        listings.returnForChanges(draft.id(), "admin-one", "Legg ved bilde.");

        var edited = listings.updateDraft("editing-seller", draft.id(), content("Pumpe med bilde"));

        assertThat(edited.title()).isEqualTo("Pumpe med bilde");
        assertThat(edited.feedback()).isEqualTo("Legg ved bilde.");
        assertThatIllegalStateException().isThrownBy(() -> {
            listings.submitForApproval("editing-seller", draft.id(), null);
            listings.updateDraft("editing-seller", draft.id(), content("Endret etter innsending"));
        });
    }

    @Test
    void aVerifiedColleagueMustConfirmBeforeSubmittingADraftFromSomeoneNoLongerVerified() {
        var verified = affiliations.register("verified-colleague", organisationNumber("92100011"), "Sammen AS", "Molde");
        affiliations.verify(verified.id(), "admin-one");
        var leaver = affiliations.register("leaving-colleague", verified.business().organisationNumber(), "", "");
        affiliations.verify(leaver.id(), "admin-one");
        var draft = listings.createDraft("leaving-colleague", verified.business().id(), content("Laget før avgang"));
        affiliations.withdraw(leaver.id(), "admin-one", "Har sluttet i virksomheten.");

        assertThat(listings.findForPerson("verified-colleague"))
                .filteredOn(listing -> listing.id() == draft.id())
                .extracting(BusinessListing::createdBySubject, BusinessListing::createdByVerified)
                .containsExactly(tuple("leaving-colleague", false));
        assertThat(listings.findForPerson("leaving-colleague")).isEmpty();
        assertThatIllegalStateException()
                .isThrownBy(() -> listings.submitForApproval("verified-colleague", draft.id(), " "))
                .withMessage("Utkastet er laget av en person som ikke lenger er verifisert. "
                        + "Skriv en merknad om hva du har kontrollert før du sender det.");

        var submitted = listings.submitForApproval("verified-colleague", draft.id(),
                "Kontrollert pris og tilstand mot servicerapporten.");

        assertThat(submitted.status()).isEqualTo(ListingPublicationStatus.SUBMITTED);
        assertThat(listings.findAuditTrail(draft.id()))
                .extracting(ListingAuditEntry::action, ListingAuditEntry::actorSubject, ListingAuditEntry::note)
                .containsExactly(
                        tuple(ListingAuditEntry.Action.CREATED, "leaving-colleague", null),
                        tuple(ListingAuditEntry.Action.SUBMITTED_FOR_UNVERIFIED_CREATOR, "verified-colleague",
                                "Kontrollert pris og tilstand mot servicerapporten."));
    }

    @Test
    void theAuditTrailShowsWhoCreatedSubmittedAndPublished() {
        var affiliation = affiliations.register("traced-seller", organisationNumber("92100012"), "Spor AS", "Sula");
        affiliations.verify(affiliation.id(), "admin-one");
        var draft = listings.createDraft("traced-seller", affiliation.business().id(), content("Sporet annonse"));
        listings.updateDraft("traced-seller", draft.id(), content("Sporet annonse, endret"));
        listings.submitForApproval("traced-seller", draft.id(), null);
        listings.approve(draft.id(), "admin-one");

        assertThat(listings.findAuditTrail(draft.id()))
                .extracting(ListingAuditEntry::action, ListingAuditEntry::actorSubject)
                .containsExactly(
                        tuple(ListingAuditEntry.Action.CREATED, "traced-seller"),
                        tuple(ListingAuditEntry.Action.UPDATED, "traced-seller"),
                        tuple(ListingAuditEntry.Action.SUBMITTED, "traced-seller"),
                        tuple(ListingAuditEntry.Action.APPROVED, "admin-one"));
        assertThat(listings.findAll()).extracting(BusinessListing::id).contains(draft.id());
    }

    @Test
    void aPendingAffiliationSeesNoneOfTheBusinessListings() {
        // Hvem som helst kan registrere et organisasjonsnummer; en tilknytning som
        // venter gir ikke innsyn i virksomhetens annonser (0009, funnet ved manuell test).
        var first = affiliations.register("first-drafter", organisationNumber("92100020"), "Innsyn AS", "Hitra");
        affiliations.verify(first.id(), "admin-one");
        var claimant = affiliations.register("unverified-claimant", first.business().organisationNumber(), "", "");
        var firstDraft = listings.createDraft("first-drafter", first.business().id(), content("Hemmelig utkast"));

        assertThat(listings.findForPerson("unverified-claimant")).isEmpty();
        assertThatIllegalStateException()
                .isThrownBy(() -> listings.updateDraft("unverified-claimant", firstDraft.id(), content("Endret av andre")));

        affiliations.verify(claimant.id(), "admin-one");

        assertThat(listings.findForPerson("unverified-claimant")).extracting(BusinessListing::id).contains(firstDraft.id());
    }

    private BusinessListing publish(String person, java.util.UUID businessId, String title) {
        var draft = listings.createDraft(person, businessId, content(title));
        listings.submitForApproval(person, draft.id(), null);
        return listings.approve(draft.id(), "admin-one");
    }

    private static ListingContent content(String title) {
        return new ListingContent(title, "Bergen", ListingCondition.GOOD, EquipmentCategory.OTHER,
                new BigDecimal("10000"), "Normal slitasje etter drift.");
    }

    private static OrganisationNumber organisationNumber(String firstEightDigits) {
        int[] weights = {3, 2, 7, 6, 5, 4, 3, 2};
        for (var candidate = Integer.parseInt(firstEightDigits); ; candidate++) {
            var digits = String.format("%08d", candidate);
            var sum = 0;
            for (var i = 0; i < 8; i++) {
                sum += weights[i] * Character.digit(digits.charAt(i), 10);
            }
            var check = sum % 11 == 0 ? 0 : 11 - sum % 11;
            if (check != 10) {
                return new OrganisationNumber(digits + check);
            }
        }
    }
}
