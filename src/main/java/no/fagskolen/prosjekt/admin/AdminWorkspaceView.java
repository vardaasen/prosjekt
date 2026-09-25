package no.fagskolen.prosjekt.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.applications.SellerApplications;
import no.fagskolen.prosjekt.marketplace.applications.SellerRoleProvisioningException;
import no.fagskolen.prosjekt.marketplace.domain.Affiliation;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationAuditEntry;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplication;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationAuditEntry;
import no.fagskolen.prosjekt.marketplace.people.PersonContact;
import no.fagskolen.prosjekt.marketplace.people.PersonDirectory;
import no.fagskolen.prosjekt.marketplace.ui.AffiliationTexts;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Administrasjonen (userflow 11, #18): tilknytninger som venter, oversikt over
 * alle tilknytninger med beslutninger, og den gamle selgersøknaden holdt
 * tydelig adskilt til den fjernes (#26).
 */
@Route("admin")
@RolesAllowed("ADMIN")
@PageTitle("Administrasjon | Havbruksbrukt")
public class AdminWorkspaceView extends VerticalLayout {

    private final Affiliations affiliations;
    private final PersonDirectory personDirectory;
    private final Map<String, String> people = new HashMap<>();
    private final Grid<Affiliation> pendingAffiliations = new Grid<>(Affiliation.class, false);
    private final Grid<Affiliation> allAffiliations = new Grid<>(Affiliation.class, false);

    public AdminWorkspaceView(
            SellerApplications sellerApplications,
            Affiliations affiliations,
            PersonDirectory personDirectory) {
        this.affiliations = affiliations;
        this.personDirectory = personDirectory;

        pendingAffiliations.addColumn(this::business).setHeader("Virksomhet");
        pendingAffiliations.addColumn(affiliation -> person(affiliation.personSubject())).setHeader("Person");
        pendingAffiliations.addColumn(affiliation -> AffiliationTexts.time(affiliation.registeredAt()))
                .setHeader("Registrert");
        pendingAffiliations.addComponentColumn(affiliation -> {
            var verify = new Button("Verifiser", event -> decide(() ->
                    affiliations.verify(affiliation.id(), currentAdministratorSubject()),
                    "Tilknytningen er verifisert."));
            verify.addThemeVariants(ButtonVariant.PRIMARY);
            return verify;
        }).setHeader("Verifiser");
        pendingAffiliations.addComponentColumn(affiliation -> reasonButton(
                "Avvis", "Avvis tilknytningen til " + affiliation.business().name() + "?", "Avvis tilknytning",
                (reason, subject) -> affiliations.reject(affiliation.id(), subject, reason),
                "Tilknytningen er avvist.")).setHeader("Avvis");
        pendingAffiliations.setAllRowsVisible(true);

        allAffiliations.addColumn(this::business).setHeader("Virksomhet");
        allAffiliations.addColumn(affiliation -> person(affiliation.personSubject())).setHeader("Person");
        allAffiliations.addColumn(affiliation -> AffiliationTexts.status(affiliation.status())).setHeader("Status");
        allAffiliations.addColumn(affiliation -> affiliation.decidedBy() == null ? "" : person(affiliation.decidedBy()))
                .setHeader("Avgjort av");
        allAffiliations.addColumn(affiliation -> AffiliationTexts.time(affiliation.decidedAt())).setHeader("Avgjort");
        allAffiliations.addColumn(affiliation -> affiliation.reason() == null ? "" : affiliation.reason())
                .setHeader("Begrunnelse");
        allAffiliations.addComponentColumn(affiliation -> affiliation.status() == AffiliationStatus.VERIFIED
                ? reasonButton("Trekk tilbake", "Trekk tilbake tilknytningen til " + affiliation.business().name() + "?",
                        "Trekk tilbake tilknytning",
                        (reason, subject) -> affiliations.withdraw(affiliation.id(), subject, reason),
                        "Tilknytningen er trukket tilbake.")
                : new Paragraph()).setHeader("Trekk tilbake");
        allAffiliations.addComponentColumn(this::affiliationAuditButton).setHeader("Revisjonsspor");
        allAffiliations.setAllRowsVisible(true);
        refreshAffiliations();

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

        setSpacing(true);
        setPadding(true);
        add(
                new H1("Administrasjon"),
                new H2("Tilknytninger som venter"),
                new Paragraph("Verifiser at personen handler for virksomheten. Navn og e-post hentes fra "
                        + "innloggingstjenesten når du ser saken, og lagres ikke."),
                pendingAffiliations,
                new H2("Alle tilknytninger"),
                allAffiliations,
                new H2("Selgersøknader (gammel ordning, fjernes)"),
                new Paragraph("Den gamle ordningen gir Keycloak-rollen SELLER. Den verifiserer ikke en virksomhet, "
                        + "og fjernes når tilknytninger styrer tilgangen (#26)."),
                pendingApplications);
    }

    private String business(Affiliation affiliation) {
        return affiliation.business().name() + " (" + affiliation.business().organisationNumber().value() + ")";
    }

    private String person(String oidcSubject) {
        return people.computeIfAbsent(oidcSubject, subject -> personDirectory.lookup(subject)
                .map(AdminWorkspaceView::describe)
                .orElse("Navn og e-post ikke tilgjengelig"));
    }

    private static String describe(PersonContact contact) {
        return contact.email().isBlank() ? contact.displayName() : contact.displayName() + " <" + contact.email() + ">";
    }

    private void decide(Runnable decision, String confirmation) {
        try {
            decision.run();
            refreshAffiliations();
            Notification.show(confirmation);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            refreshAffiliations();
            Notification.show(exception.getMessage());
        }
    }

    private Button reasonButton(
            String label,
            String question,
            String confirmLabel,
            BiConsumer<String, String> decision,
            String confirmation) {
        var button = new Button(label);
        button.addClickListener(event -> {
            var reason = new TextArea("Begrunnelse");
            reason.setHelperText("Personen får se begrunnelsen.");
            reason.setRequiredIndicatorVisible(true);
            var dialog = new Dialog(new Paragraph(question), reason);
            dialog.getFooter().add(new Button("Avbryt", click -> dialog.close()), new Button(confirmLabel, click -> {
                if (reason.getValue().isBlank()) {
                    reason.setInvalid(true);
                    reason.setErrorMessage("Skriv inn en begrunnelse.");
                    return;
                }
                decide(() -> decision.accept(reason.getValue(), currentAdministratorSubject()), confirmation);
                dialog.close();
            }));
            dialog.open();
        });
        return button;
    }

    private Button affiliationAuditButton(Affiliation affiliation) {
        var audit = new Button("Vis");
        audit.addClickListener(event -> {
            var entries = new Grid<AffiliationAuditEntry>(AffiliationAuditEntry.class, false);
            entries.addColumn(entry -> action(entry.action())).setHeader("Handling");
            entries.addColumn(entry -> person(entry.actorSubject())).setHeader("Utført av");
            entries.addColumn(entry -> AffiliationTexts.time(entry.occurredAt())).setHeader("Tidspunkt");
            entries.addColumn(entry -> entry.reason() == null ? "" : entry.reason()).setHeader("Begrunnelse");
            entries.setItems(affiliations.findAuditTrail(affiliation.id()));
            new Dialog(new H2("Revisjonsspor"), entries).open();
        });
        return audit;
    }

    private static String action(AffiliationAuditEntry.Action action) {
        return switch (action) {
            case REGISTERED -> "Registrert";
            case VERIFIED -> "Verifisert";
            case REJECTED -> "Avvist";
            case WITHDRAWN -> "Trukket tilbake";
        };
    }

    private void refreshAffiliations() {
        pendingAffiliations.setItems(affiliations.findPending());
        allAffiliations.setItems(affiliations.findAll());
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
