package no.fagskolen.prosjekt.admin;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("admin")
@PageTitle("Administrasjon | Havbruksbrukt")
public class AdminWorkspaceView extends VerticalLayout {

    public AdminWorkspaceView() {
        setSpacing(true);
        setPadding(true);
        add(
                new H1("Administrasjon"),
                new Paragraph("Selgersøknader og revisjonsspor vises her når godkjenningsflyten implementeres."));
    }
}
