package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.button.Button;
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
import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;
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
    private final AuthenticationContext authenticationContext;
    private final String personSubject;
    private final Grid<Affiliation> affiliationGrid = new Grid<>(Affiliation.class, false);

    public MinSideView(Affiliations affiliations, AuthenticationContext authenticationContext) {
        this.affiliations = affiliations;
        this.authenticationContext = authenticationContext;
        this.personSubject = authenticationContext.getAuthenticatedUser(OidcUser.class)
                .map(OidcUser::getSubject)
                .orElseThrow(() -> new IllegalStateException("Min side krever en innlogget person."));

        affiliationGrid.addColumn(affiliation -> affiliation.business().name()).setHeader("Virksomhet");
        affiliationGrid.addColumn(affiliation -> affiliation.business().organisationNumber().value())
                .setHeader("Organisasjonsnummer");
        affiliationGrid.addColumn(affiliation -> statusText(affiliation.status())).setHeader("Status");
        affiliationGrid.setAllRowsVisible(true);
        refresh();

        setSpacing(true);
        setPadding(true);
        add(new H1("Min side"),
                new H2("Dine virksomheter"),
                new Paragraph("En virksomhet du registrerer, venter på verifisering av markedsplassadministratoren. "
                        + "Du kan lage utkast og gi bud før den er verifisert, men ikke publisere eller fullføre en handel."),
                affiliationGrid,
                new H2("Registrer virksomhet"),
                registrationForm());
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
                refresh();
                Notification.show("Virksomheten er registrert og venter på verifisering.");
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

    static String statusText(AffiliationStatus status) {
        return switch (status) {
            case PENDING -> "Venter på verifisering";
            case VERIFIED -> "Verifisert";
            case REJECTED -> "Avvist";
            case WITHDRAWN -> "Trukket tilbake";
        };
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
