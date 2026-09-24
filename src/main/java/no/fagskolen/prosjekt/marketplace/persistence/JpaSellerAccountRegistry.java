package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.accounts.SellerAccountRegistry;
import no.fagskolen.prosjekt.marketplace.domain.Seller;
import no.fagskolen.prosjekt.marketplace.domain.SellerAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JpaSellerAccountRegistry implements SellerAccountRegistry {

    private final SellerAccountJpaRepository sellerAccounts;

    JpaSellerAccountRegistry(SellerAccountJpaRepository sellerAccounts) {
        this.sellerAccounts = sellerAccounts;
    }

    @Override
    @Transactional
    public SellerAccount registerSellerAccount(String oidcSubject, String sellerName) {
        var requestedAccount = new SellerAccount(oidcSubject, new Seller(sellerName, false));
        var account = sellerAccounts.findById(requestedAccount.oidcSubject())
                .orElseGet(() -> sellerAccounts.save(new SellerAccountJpaEntity(
                        requestedAccount.oidcSubject(),
                        requestedAccount.seller().name())));
        return new SellerAccount(
                account.oidcSubject(),
                new Seller(account.sellerName(), account.verifiedSeller(), account.sellerLocation()));
    }

    @Override
    @Transactional
    public SellerAccount activateSellerAccount(String oidcSubject, Seller seller) {
        var account = sellerAccounts.findById(oidcSubject)
                .orElseGet(() -> sellerAccounts.save(new SellerAccountJpaEntity(oidcSubject, seller.name())));
        account.activate(seller.name(), seller.location());
        return new SellerAccount(
                account.oidcSubject(),
                new Seller(account.sellerName(), account.verifiedSeller(), account.sellerLocation()));
    }
}
