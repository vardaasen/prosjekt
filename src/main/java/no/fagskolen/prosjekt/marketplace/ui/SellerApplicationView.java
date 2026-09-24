package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import no.fagskolen.prosjekt.marketplace.applications.SellerApplications;
import no.fagskolen.prosjekt.marketplace.domain.Seller;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplication;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route("selgersoknad")
@PageTitle("Søk som selger | Havbruksbrukt")
public class SellerApplicationView extends VerticalLayout {

    public SellerApplicationView(SellerApplications sellerApplications) {
        var oidcUser = currentOidcUser();
        var sellerName = new TextField("Virksomhetsnavn");
        sellerName.setRequired(true);
        var sellerLocation = new TextField("Lokasjon");
        sellerLocation.setRequired(true);
        var form = new FormLayout();
        form.setAutoResponsive(true);
        form.setColumnWidth("18rem");
        form.setExpandColumns(true);
        form.addFormRow(sellerName, sellerLocation);

        var latestApplication = sellerApplications.findLatestFor(oidcUser.getSubject());
        var status = new Paragraph();
        latestApplication.ifPresent(application -> status.setText(statusMessage(application)));

        var submit = new Button("Send søknad");
        submit.addClickListener(event -> {
            try {
                var application = sellerApplications.submit(
                        oidcUser.getSubject(),
                        new Seller(sellerName.getValue(), false, sellerLocation.getValue()));
                status.setText(statusMessage(application));
                submit.setEnabled(false);
                Notification.show("Selgersøknaden er sendt.");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                Notification.show(exception.getMessage());
            }
        });
        if (latestApplication.isPresent() && latestApplication.get().status() == SellerApplicationStatus.PENDING) {
            submit.setEnabled(false);
        }

        setSpacing(true);
        setPadding(true);
        add(
                new H1("Søk som selger"),
                new Paragraph("Søknaden gir ingen publiseringsrettigheter før en markedsplassadministrator har godkjent den."),
                form,
                submit,
                status);
    }

    private OidcUser currentOidcUser() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser;
        }
        throw new IllegalStateException("Seller application requires an OIDC user");
    }

    private String statusMessage(SellerApplication application) {
        return switch (application.status()) {
            case PENDING -> "Søknaden venter på vurdering.";
            case APPROVED -> "Søknaden er godkjent. Logg inn på nytt for å få tilgang til selgerområdet.";
            case REJECTED -> "Søknaden ble avslått: " + application.rejectionReason();
        };
    }
}
