package no.fagskolen.prosjekt.marketplace.accounts;

import no.fagskolen.prosjekt.marketplace.domain.SellerAccount;

public interface SellerAccountRegistry {

    SellerAccount registerSellerAccount(String oidcSubject, String sellerName);
}
