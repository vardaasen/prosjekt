package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import no.fagskolen.prosjekt.marketplace.accounts.SellerAccountRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route("app")
@PageTitle("Selgerområde | Havbruksbrukt")
public class SellerWorkspaceView extends VerticalLayout {

    public SellerWorkspaceView(SellerAccountRegistry sellerAccountRegistry) {
        var oidcUser = currentOidcUser();
        var sellerAccount = sellerAccountRegistry.registerSellerAccount(
                oidcUser.getSubject(),
                sellerName(oidcUser));

        setSpacing(true);
        setPadding(true);
        add(
                new H1("Selgerområde"),
                new Paragraph("Innlogget som " + sellerAccount.seller().name() + "."),
                new Paragraph("Nye annonser vil opprettes som utkast og eies av denne selgerkontoen."),
                new Button("Ny annonse"));
    }

    private OidcUser currentOidcUser() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser;
        }
        throw new IllegalStateException("Seller workspace requires an OIDC user");
    }

    private String sellerName(OidcUser oidcUser) {
        var preferredUsername = oidcUser.getPreferredUsername();
        return preferredUsername == null || preferredUsername.isBlank()
                ? oidcUser.getSubject()
                : preferredUsername;
    }
}
