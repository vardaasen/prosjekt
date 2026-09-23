package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("app")
@PageTitle("Selgerområde | Havbruksbrukt")
public class SellerWorkspaceView extends VerticalLayout {

    public SellerWorkspaceView() {
        setSpacing(true);
        setPadding(true);
        add(
                new H1("Selgerområde"),
                new Paragraph("Vaadin Flow-flaten starter her. Offentlige SEO-sider ligger utenfor Flow-rutene."),
                new Button("Ny annonse"));
    }
}
