package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import no.fagskolen.prosjekt.marketplace.domain.Business;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListing;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListings;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import no.fagskolen.prosjekt.marketplace.people.PersonContact;
import no.fagskolen.prosjekt.marketplace.people.PersonDirectory;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.domain.Affiliation;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Min side (userflow 10 og 11): personens virksomheter og tilknytninger, og
 * registrering av en virksomhet. Åpen for alle innloggede personer.
 */
@Route("")
@PermitAll
@PageTitle("Min side | Havbruksbrukt")
public class MinSideView extends VerticalLayout implements BeforeEnterObserver {

    private final Affiliations affiliations;
    private final BusinessListings businessListings;
    private final PersonDirectory personDirectory;
    private final AuthenticationContext authenticationContext;
    private final Grid<BusinessListing> listingGrid = new Grid<>(BusinessListing.class, false);
    private final String personSubject;
    private final Grid<Affiliation> affiliationGrid = new Grid<>(Affiliation.class, false);

    public MinSideView(
            Affiliations affiliations,
            BusinessListings businessListings,
            PersonDirectory personDirectory,
            AuthenticationContext authenticationContext) {
        this.affiliations = affiliations;
        this.businessListings = businessListings;
        this.personDirectory = personDirectory;
        this.authenticationContext = authenticationContext;
        this.personSubject = authenticationContext.getAuthenticatedUser(OidcUser.class)
                .map(OidcUser::getSubject)
                .orElseThrow(() -> new IllegalStateException("Min side krever en innlogget person."));

        affiliationGrid.addColumn(affiliation -> affiliation.business().name()).setHeader("Virksomhet");
        affiliationGrid.addColumn(affiliation -> affiliation.business().organisationNumber().value())
                .setHeader("Organisasjonsnummer");
        affiliationGrid.addColumn(affiliation -> AffiliationTexts.status(affiliation.status())).setHeader("Status");
        // Begrunnelsen for avvisning eller tilbaketrekking vises til personen (#18).
        affiliationGrid.addColumn(affiliation -> affiliation.reason() == null ? "" : affiliation.reason())
                .setHeader("Begrunnelse");
        affiliationGrid.setAllRowsVisible(true);
        refresh();

        listingGrid.addColumn(BusinessListing::title).setHeader("Tittel");
        listingGrid.addColumn(BusinessListing::businessName).setHeader("Virksomhet");
        listingGrid.addColumn(listing -> ListingTexts.status(listing.status())).setHeader("Status");
        listingGrid.addColumn(this::creator).setHeader("Laget av");
        listingGrid.addColumn(listing -> listing.feedback() == null ? "" : listing.feedback())
                .setHeader("Tilbakemelding");
        listingGrid.addComponentColumn(this::listingAction).setHeader("Handling");
        listingGrid.setAllRowsVisible(true);
        refreshListings();

        setSpacing(true);
        setPadding(true);
        add(new H1("Min side"),
                new H2("Annonser"),
                new Paragraph("Annonsene eies av virksomheten, og alle med verifisert tilknytning til den ser dem. "
                        + "Et utkast blir offentlig når markedsplassadministratoren har godkjent det."),
                listingGrid,
                new H2("Ny annonse"),
                newListingForm(),
                new H2("Dine virksomheter"),
                new Paragraph("En virksomhet du registrerer, venter på verifisering av markedsplassadministratoren. "
                        + "Når den er verifisert, kan du lage annonser og se virksomhetens annonser."),
                affiliationGrid,
                new H2("Registrer virksomhet"),
                registrationForm());
    }

    private com.vaadin.flow.component.Component listingAction(BusinessListing listing) {
        return switch (listing.status()) {
            case DRAFT -> new HorizontalLayout(new Button("Endre", event -> openEditor(listing)),
                    new Button("Send til godkjenning", event -> submit(listing)));
            case PUBLISHED -> {
                var link = new Anchor("/utstyr/" + listing.slug(), "Se annonsen");
                link.setRouterIgnore(true);
                yield link;
            }
            default -> new Paragraph();
        };
    }

    private String creator(BusinessListing listing) {
        if (listing.createdBySubject() == null) {
            return "";
        }
        var name = listing.createdBySubject().equals(personSubject) ? "Deg"
                : personDirectory.lookup(listing.createdBySubject()).map(PersonContact::displayName)
                        .orElse("En kollega");
        return listing.createdByVerified() ? name : name + " (ikke lenger verifisert)";
    }

    private void submit(BusinessListing listing) {
        var fromUnverifiedColleague = listing.createdBySubject() != null
                && !listing.createdBySubject().equals(personSubject)
                && !listing.createdByVerified();
        if (fromUnverifiedColleague) {
            // Produkteier 2026-09-25: utkastet åpnes til gjennomgang med forklaring og
            // en påkrevd merknad, i stedet for en knapp som bare sender det.
            openReview(listing);
            return;
        }
        try {
            businessListings.submitForApproval(personSubject, listing.id(), null);
            refreshListings();
            Notification.show("Annonsen er sendt til godkjenning.");
        } catch (IllegalStateException exception) {
            Notification.show(exception.getMessage());
        }
    }

    private void openReview(BusinessListing listing) {
        var editor = new ListingEditor();
        editor.show(listing);
        var note = new TextArea("Merknad om kontrollen");
        note.setHelperText("Skriv hva du har kontrollert. Merknaden står i revisjonssporet og vises for "
                + "markedsplassadministratoren.");
        note.setRequiredIndicatorVisible(true);
        note.setMaxLength(2000);
        note.setWidthFull();
        var dialog = new Dialog(
                new H2("Kontroller utkastet før du sender det"),
                new Paragraph("«" + listing.title() + "» er laget av " + creator(listing) + ". Personen er ikke "
                        + "lenger verifisert for virksomheten. Gå gjennom innholdet, rett det som trengs, og skriv en "
                        + "merknad om kontrollen. Du står som den som sendte annonsen til godkjenning."),
                editor,
                note);
        dialog.setWidth("min(56rem, 95vw)");
        var send = new Button("Lagre og send til godkjenning", event -> {
            if (note.getValue().isBlank()) {
                note.setInvalid(true);
                note.setErrorMessage("Skriv en merknad om hva du har kontrollert.");
                return;
            }
            editor.content().ifPresent(content -> {
                try {
                    businessListings.updateDraft(personSubject, listing.id(), content);
                    businessListings.submitForApproval(personSubject, listing.id(), note.getValue());
                    dialog.close();
                    refreshListings();
                    Notification.show("Annonsen er sendt til godkjenning med merknaden din.");
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    Notification.show(exception.getMessage());
                }
            });
        });
        send.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(new Button("Avbryt", event -> dialog.close()), send);
        dialog.open();
    }

    private void openEditor(BusinessListing listing) {
        var editor = new ListingEditor();
        editor.show(listing);
        var dialog = new Dialog(new H2("Endre utkast"), editor);
        if (listing.feedback() != null) {
            dialog.add(new Paragraph("Tilbakemelding fra markedsplassadministratoren: " + listing.feedback()));
        }
        var save = new Button("Lagre endringer", event -> editor.content().ifPresent(content -> {
            try {
                businessListings.updateDraft(personSubject, listing.id(), content);
                dialog.close();
                refreshListings();
                Notification.show("Utkastet er lagret.");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                Notification.show(exception.getMessage());
            }
        }));
        save.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(new Button("Avbryt", event -> dialog.close()), save);
        dialog.open();
    }

    private com.vaadin.flow.component.Component newListingForm() {
        var businesses = affiliations.findFor(personSubject).stream()
                .filter(affiliation -> ListingPublicationStatus.mayCreateDraft(affiliation.status()))
                .map(Affiliation::business)
                .toList();
        if (businesses.isEmpty()) {
            return new Paragraph("Du kan lage annonser når tilknytningen din til en virksomhet er verifisert. "
                    + "Registrer virksomheten nedenfor hvis du ikke har gjort det.");
        }
        var business = new ComboBox<Business>("Virksomhet");
        business.setItems(businesses);
        business.setItemLabelGenerator(Business::name);
        business.setRequiredIndicatorVisible(true);
        if (businesses.size() == 1) {
            business.setValue(businesses.getFirst());
        }
        var editor = new ListingEditor();
        var save = new Button("Lagre utkast", event -> {
            if (business.isEmpty()) {
                business.setInvalid(true);
                business.setErrorMessage("Velg virksomheten som eier annonsen.");
            }
            editor.content().filter(content -> !business.isEmpty()).ifPresent(content -> {
                try {
                    businessListings.createDraft(personSubject, business.getValue().id(), content);
                    editor.clear();
                    refreshListings();
                    Notification.show("Utkastet er lagret. Send det til godkjenning når det er klart.");
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    Notification.show(exception.getMessage());
                }
            });
        });
        save.addThemeVariants(ButtonVariant.PRIMARY);
        var layout = new VerticalLayout(business, editor, save);
        layout.setPadding(false);
        layout.setMaxWidth("48rem");
        return layout;
    }

    private void refreshListings() {
        listingGrid.setItems(businessListings.findForPerson(personSubject));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var roles = java.util.stream.Stream.of("ADMIN", "SELLER")
                .filter(authenticationContext::hasRole)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!AppNavigation.mayUseMinSide(roles)) {
            event.forwardTo(no.fagskolen.prosjekt.admin.AdminWorkspaceView.class);
        }
    }

    private FormLayout registrationForm() {
        var organisationNumber = new TextField("Organisasjonsnummer");
        organisationNumber.setHelperText("Ni sifre, for eksempel 974 760 673.");
        var name = new TextField("Navn på virksomheten");
        var location = new TextField("Lokasjon");
        location.setHelperText("Valgfritt, for eksempel kommune.");

        var binder = new Binder<RegistrationForm>();
        binder.forField(organisationNumber)
                .asRequired("Skriv inn organisasjonsnummeret.")
                .withValidator((value, context) -> OrganisationNumberMessages.errorFor(value)
                        .map(ValidationResult::error)
                        .orElseGet(ValidationResult::ok))
                .bind(RegistrationForm::getOrganisationNumber, RegistrationForm::setOrganisationNumber);
        binder.forField(name)
                .asRequired("Skriv inn navnet på virksomheten.")
                .bind(RegistrationForm::getName, RegistrationForm::setName);
        binder.forField(location).bind(RegistrationForm::getLocation, RegistrationForm::setLocation);

        var register = new Button("Registrer virksomhet", event -> {
            var form = new RegistrationForm();
            if (binder.writeBeanIfValid(form)) {
                affiliations.register(personSubject, new OrganisationNumber(form.getOrganisationNumber()),
                        form.getName(), form.getLocation());
                binder.readBean(new RegistrationForm());
                Notification.show("Virksomheten er registrert og venter på verifisering.");
                // Skjemaet for ny annonse bygges ut fra tilknytningene; last siden på nytt.
                getUI().ifPresent(ui -> ui.getPage().reload());
            }
        });
        register.addThemeVariants(ButtonVariant.PRIMARY);

        var layout = new FormLayout(organisationNumber, name, location, register);
        layout.setMaxWidth("40rem");
        return layout;
    }

    private void refresh() {
        affiliationGrid.setItems(affiliations.findFor(personSubject));
    }

    /** Skjemadata for Binder; bare i visningen, aldri lagret. */
    static final class RegistrationForm {
        private String organisationNumber = "";
        private String name = "";
        private String location = "";

        String getOrganisationNumber() {
            return organisationNumber;
        }

        void setOrganisationNumber(String organisationNumber) {
            this.organisationNumber = organisationNumber;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        String getLocation() {
            return location;
        }

        void setLocation(String location) {
            this.location = location;
        }
    }
}
