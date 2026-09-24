package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "seller_account")
class SellerAccountJpaEntity {

    @Id
    @Column(name = "oidc_subject", nullable = false, updatable = false)
    private String oidcSubject;

    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @Column(name = "verified_seller", nullable = false)
    private boolean verifiedSeller;

    @Column(name = "seller_location", nullable = false)
    private String sellerLocation;

    protected SellerAccountJpaEntity() {
    }

    SellerAccountJpaEntity(String oidcSubject, String sellerName) {
        this.oidcSubject = oidcSubject;
        this.sellerName = sellerName;
        this.verifiedSeller = false;
        this.sellerLocation = "";
    }

    String oidcSubject() {
        return oidcSubject;
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
}
