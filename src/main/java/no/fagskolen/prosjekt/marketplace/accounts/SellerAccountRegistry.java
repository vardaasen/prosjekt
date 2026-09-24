package no.fagskolen.prosjekt.marketplace.accounts;

import no.fagskolen.prosjekt.marketplace.domain.SellerAccount;
import no.fagskolen.prosjekt.marketplace.domain.Seller;

public interface SellerAccountRegistry {

    SellerAccount registerSellerAccount(String oidcSubject, String sellerName);

    SellerAccount activateSellerAccount(String oidcSubject, Seller seller);
}
