package vciptvman.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.apache.commons.lang3.StringUtils;

@Route("")
public class MainView extends AppLayout {

    public enum SelectedTab {
        STREAMS,
        BOOKMARKS,
        DATABASE,
    }

    private Tabs tabs;

    public MainView() {
        init(null);
        tabs.setSelectedIndex(-1);
    }

    public MainView(SelectedTab selected) {
        init(null);
        tabs.setSelectedIndex(selected.ordinal());
    }

    public MainView(int selectedIndex, String messageText) {
        init(messageText);

        tabs.setSelectedIndex(selectedIndex);
    }

    private void init(String messageText) {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("EPG/IPTV Stream Manager Version 0.2");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        tabs = getTabs();

        if (StringUtils.isEmpty(messageText)) {
            addToDrawer(tabs);
            addToNavbar(toggle, title);
        } else {
            H1 message = new H1(messageText);
            addToDrawer(tabs);
            addToNavbar(toggle, title, message);
        }
    }

    private Tabs getTabs() {
        Tabs tabs = new Tabs();
        tabs.add(createTab(VaadinIcon.MOVIE, "Streams", IPTVStreams.class),
                 createTab(VaadinIcon.NEWSPAPER, "Bookmarks", Bookmarks.class)
                 /* ,createTab(VaadinIcon.DATABASE, "Database", Database.class) */
        );

        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        return tabs;
    }

    private Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> route) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setRoute(route);
        link.setTabIndex(-1);

        return new Tab(link);
    }
}
