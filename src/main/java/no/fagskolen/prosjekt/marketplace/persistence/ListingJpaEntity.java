package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "listing")
class ListingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Version
    private long version;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingPublicationStatus publicationStatus;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceNok;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private BusinessJpaEntity business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_through_affiliation_id", updatable = false)
    private AffiliationJpaEntity createdThroughAffiliation;

    @Column(name = "review_feedback")
    private String reviewFeedback;

    @Column(name = "submitted_at")
    private java.time.Instant submittedAt;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private java.time.Instant decidedAt;

    @Column(nullable = false)
    private String sellerName;

    @Column(nullable = false)
    private boolean verifiedSeller;

    @Column(nullable = false)
    private String sellerLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_account_subject")
    private SellerAccountJpaEntity sellerAccount;

    @Column(nullable = false)
    private LocalDate publishedAt;

    @Column(nullable = false, length = 2000)
    private String summary;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "listing_documentation", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "documentation", nullable = false)
    private Set<String> documentation = new LinkedHashSet<>();

    protected ListingJpaEntity() {
    }

    ListingJpaEntity(
            String slug,
            String title,
            String location,
            ListingCondition condition,
            EquipmentCategory category,
            ListingPublicationStatus publicationStatus,
            BigDecimal priceNok,
            String sellerName,
            boolean verifiedSeller,
            String sellerLocation,
            LocalDate publishedAt,
            String summary,
            Set<String> documentation) {
        this(
                slug,
                title,
                location,
                condition,
                category,
                publicationStatus,
                priceNok,
                sellerName,
                verifiedSeller,
                sellerLocation,
                null,
                publishedAt,
                summary,
                documentation);
    }

    ListingJpaEntity(
            String slug,
            String title,
            String location,
            ListingCondition condition,
            EquipmentCategory category,
            ListingPublicationStatus publicationStatus,
            BigDecimal priceNok,
            String sellerName,
            boolean verifiedSeller,
            String sellerLocation,
            SellerAccountJpaEntity sellerAccount,
            LocalDate publishedAt,
            String summary,
            Set<String> documentation) {
        this.slug = slug;
        this.title = title;
        this.location = location;
        this.condition = condition;
        this.category = category;
        this.publicationStatus = publicationStatus;
        this.priceNok = priceNok;
        this.sellerName = sellerName;
        this.verifiedSeller = verifiedSeller;
        this.sellerLocation = sellerLocation;
        this.sellerAccount = sellerAccount;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.documentation = new LinkedHashSet<>(documentation);
    }

    String slug() {
        return slug;
    }

    String title() {
        return title;
    }

    String location() {
        return location;
    }

    ListingCondition condition() {
        return condition;
    }

    EquipmentCategory category() {
        return category;
    }

    ListingPublicationStatus publicationStatus() {
        return publicationStatus;
    }

    BigDecimal priceNok() {
        return priceNok;
    }

    String sellerName() {
        return sellerName;
    }

    boolean verifiedSeller() {
        return verifiedSeller;
    }

    String sellerLocation() {
        return sellerLocation;
    }

    SellerAccountJpaEntity sellerAccount() {
        return sellerAccount;
    }

    LocalDate publishedAt() {
        return publishedAt;
    }

    String summary() {
        return summary;
    }

    Set<String> documentation() {
        return Set.copyOf(documentation);
    }

    void publish(LocalDate publicationDate) {
        publicationStatus = ListingPublicationStatus.PUBLISHED;
        publishedAt = publicationDate;
    }

    /** Et utkast eid av en virksomhet (userflow 12). */
    static ListingJpaEntity draftFor(
            BusinessJpaEntity business,
            AffiliationJpaEntity createdThroughAffiliation,
            String slug,
            no.fagskolen.prosjekt.marketplace.domain.ListingContent content) {
        var entity = new ListingJpaEntity();
        entity.slug = slug;
        entity.title = content.title();
        entity.location = content.location();
        entity.condition = content.condition();
        entity.category = content.category();
        entity.publicationStatus = ListingPublicationStatus.DRAFT;
        entity.priceNok = content.priceNok();
        entity.sellerName = business.name();
        entity.verifiedSeller = false;
        entity.sellerLocation = business.location();
        entity.business = business;
        entity.createdThroughAffiliation = createdThroughAffiliation;
        entity.summary = content.summary();
        entity.documentation = new LinkedHashSet<>();
        return entity;
    }

    Long id() {
        return id;
    }

    BusinessJpaEntity business() {
        return business;
    }

    String reviewFeedback() {
        return reviewFeedback;
    }

    AffiliationJpaEntity createdThroughAffiliation() {
        return createdThroughAffiliation;
    }

    void updateContent(no.fagskolen.prosjekt.marketplace.domain.ListingContent content) {
        title = content.title();
        location = content.location();
        condition = content.condition();
        category = content.category();
        priceNok = content.priceNok();
        summary = content.summary();
    }

    void submit(ListingPublicationStatus newStatus, java.time.Instant at) {
        publicationStatus = newStatus;
        submittedAt = at;
    }

    void approve(ListingPublicationStatus newStatus, String administratorSubject, java.time.Instant at, LocalDate date) {
        publicationStatus = newStatus;
        decidedBy = administratorSubject;
        decidedAt = at;
        publishedAt = date;
        reviewFeedback = null;
        // Publisert etter verifisert tilknytning og annonsegodkjenning (0007).
        verifiedSeller = true;
        sellerName = business.name();
    }

    void returnForChanges(ListingPublicationStatus newStatus, String administratorSubject, java.time.Instant at,
            String feedback) {
        publicationStatus = newStatus;
        decidedBy = administratorSubject;
        decidedAt = at;
        reviewFeedback = feedback.trim();
    }
}
