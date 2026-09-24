package no.fagskolen.prosjekt.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import no.fagskolen.prosjekt.marketplace.applications.SellerApplications;
import no.fagskolen.prosjekt.marketplace.applications.SellerRoleProvisioningException;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplication;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationAuditEntry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route("admin")
@RolesAllowed("ADMIN")
@PageTitle("Administrasjon | Havbruksbrukt")
public class AdminWorkspaceView extends VerticalLayout {

    public AdminWorkspaceView(SellerApplications sellerApplications) {
        var pendingApplications = new Grid<SellerApplication>(SellerApplication.class, false);
        pendingApplications.addColumn(application -> application.seller().name()).setHeader("Virksomhet");
        pendingApplications.addColumn(application -> application.seller().location()).setHeader("Lokasjon");
        pendingApplications.addColumn(SellerApplication::submittedAt).setHeader("Innsendt");
        pendingApplications.addComponentColumn(application -> new Button("Godkjenn", event -> {
            try {
                sellerApplications.approve(application.id(), currentAdministratorSubject());
                refresh(pendingApplications, sellerApplications);
                Notification.show("Selgersøknaden er godkjent.");
            } catch (SellerRoleProvisioningException exception) {
                Notification.show(exception.getMessage());
            } catch (IllegalArgumentException | IllegalStateException exception) {
                Notification.show(exception.getMessage());
            }
        })).setHeader("Godkjenn");
        pendingApplications.addComponentColumn(application -> rejectionButton(
                application,
                pendingApplications,
                sellerApplications)).setHeader("Avslå");
        pendingApplications.addComponentColumn(application -> auditButton(application, sellerApplications))
                .setHeader("Revisjonsspor");
        refresh(pendingApplications, sellerApplications);

        var logout = new Anchor("/logout", "Logg ut");
        // Vaadin sitt klientsideruter fanger opp alle anker-klikk og prøver å
        // navigere til dem som en Vaadin-rute ("Could not navigate to
        // 'logout'"). router-ignore ber ruteren la nettleseren gjøre en vanlig
        // full sideinnlasting til /logout i stedet.
        logout.getElement().setAttribute("router-ignore", "");

        setSpacing(true);
        setPadding(true);
        add(
                new H1("Administrasjon"),
                logout,
                new Paragraph("Vurder selgersøknader. Godkjenning tildeler ikke rettigheter før Keycloak har bekreftet SELLER-rollen."),
                new Paragraph("Ventende selgersøknader"),
                pendingApplications);
    }

    private Button rejectionButton(
            SellerApplication application,
            Grid<SellerApplication> pendingApplications,
            SellerApplications sellerApplications) {
        var reject = new Button("Avslå");
        reject.addClickListener(event -> {
            var reason = new TextArea("Begrunnelse");
            reason.setRequired(true);
            var dialog = new Dialog(
                    new Paragraph("Avslå " + application.seller().name() + "?"),
                    reason);
            dialog.getFooter().add(new Button("Avslå søknad", click -> {
                try {
                    sellerApplications.reject(application.id(), currentAdministratorSubject(), reason.getValue());
                    refresh(pendingApplications, sellerApplications);
                    dialog.close();
                    Notification.show("Selgersøknaden er avslått.");
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    Notification.show(exception.getMessage());
                }
            }));
            dialog.open();
        });
        return reject;
    }

    private Button auditButton(SellerApplication application, SellerApplications sellerApplications) {
        var audit = new Button("Vis");
        audit.addClickListener(event -> {
            var auditEntries = new Grid<SellerApplicationAuditEntry>(SellerApplicationAuditEntry.class, false);
            auditEntries.addColumn(entry -> entry.action()).setHeader("Handling");
            auditEntries.addColumn(entry -> entry.actorSubject()).setHeader("Utført av");
            auditEntries.addColumn(entry -> entry.occurredAt()).setHeader("Tidspunkt");
            auditEntries.setItems(sellerApplications.findAuditTrail(application.id()));
            var dialog = new Dialog(
                    new H1("Revisjonsspor"),
                    auditEntries);
            dialog.open();
        });
        return audit;
    }

    private void refresh(Grid<SellerApplication> grid, SellerApplications sellerApplications) {
        grid.setItems(sellerApplications.findPending());
    }

    private String currentAdministratorSubject() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        throw new IllegalStateException("Administrator workspace requires an OIDC user");
    }
}
