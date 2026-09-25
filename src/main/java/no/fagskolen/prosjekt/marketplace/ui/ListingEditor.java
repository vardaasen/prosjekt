package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.ListingContent;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListing;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Feltene i en annonse, brukt både for ny annonse og for å endre et utkast.
 * Påkrevde felt og feilmeldinger følger docs/design/feilmeldinger.md.
 */
class ListingEditor extends FormLayout {

    private final Binder<Form> binder = new Binder<>();

    ListingEditor() {
        var title = new TextField("Tittel");
        title.setMaxLength(255);
        var category = new ComboBox<EquipmentCategory>("Utstyrskategori");
        category.setItems(EquipmentCategory.values());
        category.setItemLabelGenerator(EquipmentCategory::label);
        var condition = new ComboBox<ListingCondition>("Tilstand");
        condition.setItems(ListingCondition.values());
        condition.setItemLabelGenerator(ListingCondition::displayName);
        var price = new BigDecimalField("Pris i NOK");
        var location = new TextField("Lokasjon");
        location.setHelperText("For eksempel kommune eller anlegg.");
        var summary = new TextArea("Beskrivelse");
        summary.setMaxLength(2000);
        summary.setHelperText("Beskriv utstyret og den faktiske tilstanden.");

        binder.forField(title).asRequired("Skriv inn tittelen.").bind(Form::getTitle, Form::setTitle);
        binder.forField(category).asRequired("Velg utstyrskategori.").bind(Form::getCategory, Form::setCategory);
        binder.forField(condition).asRequired("Velg tilstand.").bind(Form::getCondition, Form::setCondition);
        binder.forField(price).asRequired("Skriv inn prisen.")
                .withValidator(value -> value.signum() >= 0, "Prisen kan ikke være negativ.")
                .bind(Form::getPrice, Form::setPrice);
        binder.forField(location).asRequired("Skriv inn lokasjonen.").bind(Form::getLocation, Form::setLocation);
        binder.forField(summary).asRequired("Skriv inn en beskrivelse.").bind(Form::getSummary, Form::setSummary);

        add(title, category, condition, price, location, summary);
        setColspan(summary, 2);
    }

    void show(BusinessListing listing) {
        var form = new Form();
        form.setTitle(listing.title());
        form.setCategory(listing.category());
        form.setCondition(listing.condition());
        form.setPrice(listing.priceNok());
        form.setLocation(listing.location());
        form.setSummary(listing.summary());
        binder.readBean(form);
    }

    void clear() {
        binder.readBean(new Form());
    }

    /** Innholdet hvis alle felt er gyldige; ellers vises feilene ved feltene. */
    Optional<ListingContent> content() {
        var form = new Form();
        if (!binder.writeBeanIfValid(form)) {
            return Optional.empty();
        }
        return Optional.of(new ListingContent(form.getTitle(), form.getLocation(), form.getCondition(),
                form.getCategory(), form.getPrice(), form.getSummary()));
    }

    static final class Form {
        private String title = "";
        private EquipmentCategory category;
        private ListingCondition condition;
        private BigDecimal price;
        private String location = "";
        private String summary = "";

        String getTitle() { return title; }
        void setTitle(String title) { this.title = title; }
        EquipmentCategory getCategory() { return category; }
        void setCategory(EquipmentCategory category) { this.category = category; }
        ListingCondition getCondition() { return condition; }
        void setCondition(ListingCondition condition) { this.condition = condition; }
        BigDecimal getPrice() { return price; }
        void setPrice(BigDecimal price) { this.price = price; }
        String getLocation() { return location; }
        void setLocation(String location) { this.location = location; }
        String getSummary() { return summary; }
        void setSummary(String summary) { this.summary = summary; }
    }
}
