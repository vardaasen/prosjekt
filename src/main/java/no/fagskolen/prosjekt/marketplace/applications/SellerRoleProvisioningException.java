package no.fagskolen.prosjekt.marketplace.applications;

public class SellerRoleProvisioningException extends RuntimeException {

    public SellerRoleProvisioningException(String message) {
        super(message);
    }

    public SellerRoleProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
