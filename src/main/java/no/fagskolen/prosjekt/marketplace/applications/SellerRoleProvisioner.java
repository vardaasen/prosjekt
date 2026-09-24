package no.fagskolen.prosjekt.marketplace.applications;

public interface SellerRoleProvisioner {

    void grantSellerRole(String oidcSubject);
}
