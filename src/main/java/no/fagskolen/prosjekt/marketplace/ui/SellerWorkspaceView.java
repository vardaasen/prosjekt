package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import no.fagskolen.prosjekt.marketplace.accounts.SellerAccountRegistry;
import no.fagskolen.prosjekt.marketplace.drafts.ListingDraft;
import no.fagskolen.prosjekt.marketplace.drafts.SellerListingDrafts;
import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.SellerAccount;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

@Route("app")
@RolesAllowed("SELLER")
@PageTitle("Selgerområde | Havbruksbrukt")
public class SellerWorkspaceView extends VerticalLayout {

    public SellerWorkspaceView(
            SellerAccountRegistry sellerAccountRegistry,
            SellerListingDrafts sellerListingDrafts) {
        var oidcUser = currentOidcUser();
        var sellerAccount = sellerAccountRegistry.registerSellerAccount(
                oidcUser.getSubject(),
                sellerName(oidcUser));
        var drafts = new Grid<Listing>(Listing.class, false);
        drafts.addColumn(Listing::title).setHeader("Tittel");
        drafts.addColumn(Listing::slug).setHeader("Slug");
        drafts.addColumn(Listing::publicationStatus).setHeader("Status");

        var slug = new TextField("Slug");
        var title = new TextField("Tittel");
        var location = new TextField("Lokasjon");
        var condition = new ComboBox<ListingCondition>("Tilstand");
        condition.setItems(ListingCondition.values());
        condition.setItemLabelGenerator(ListingCondition::displayName);
        condition.setValue(ListingCondition.GOOD);
        var category = new ComboBox<EquipmentCategory>("Utstyrskategori");
        category.setItems(EquipmentCategory.values());
        category.setItemLabelGenerator(EquipmentCategory::label);
        category.setValue(EquipmentCategory.OTHER);
        var price = new BigDecimalField("Pris i NOK");
        price.setValue(java.math.BigDecimal.ZERO);
        var summary = new TextArea("Beskrivelse");
        var draftForm = new FormLayout();
        draftForm.setAutoResponsive(true);
        draftForm.setColumnWidth("18rem");
        draftForm.setExpandColumns(true);
        draftForm.setExpandFields(true);
        draftForm.addFormRow(slug, title);
        draftForm.addFormRow(location, condition);
        draftForm.addFormRow(category, price);
        draftForm.addFormRow(summary);

        var createDraft = new Button("Opprett utkast", event -> {
            try {
                sellerListingDrafts.createDraft(
                        sellerAccount,
                        new ListingDraft(
                                slug.getValue(),
                                title.getValue(),
                                location.getValue(),
                                condition.getValue(),
                                category.getValue(),
                                price.getValue(),
                                summary.getValue()));
                drafts.setItems(sellerListingDrafts.findDrafts(sellerAccount));
                slug.clear();
                title.clear();
                location.clear();
                summary.clear();
                Notification.show("Utkastet er opprettet.");
            } catch (IllegalArgumentException exception) {
                Notification.show(exception.getMessage());
            }
        });
        drafts.addComponentColumn(listing -> new Button("Publiser", event -> {
            try {
                sellerListingDrafts.publishDraft(sellerAccount, listing.slug());
                drafts.setItems(sellerListingDrafts.findDrafts(sellerAccount));
                Notification.show("Annonsen er publisert.");
            } catch (IllegalArgumentException exception) {
                Notification.show(exception.getMessage());
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification.show("Annonsen ble endret i en annen økt. Last siden på nytt og prøv igjen.");
            }
        })).setHeader("Handling");
        drafts.setItems(sellerListingDrafts.findDrafts(sellerAccount));

        var logout = new Anchor("/logout", "Logg ut");
        // Se AdminWorkspaceView for hvorfor router-ignore trengs her.
        logout.getElement().setAttribute("router-ignore", "");

        setSpacing(true);
        setPadding(true);
        add(
                new H1("Selgerområde"),
                logout,
                new Paragraph("Innlogget som " + sellerAccount.seller().name() + "."),
                new Paragraph("Opprett utkast. De er bare synlige for denne selgerkontoen."),
                draftForm,
                createDraft,
                new Paragraph("Mine utkast"),
                drafts);
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
