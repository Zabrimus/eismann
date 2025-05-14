package vciptvman.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.Route;

@Route("database")
public class Database extends MainView {

    public Database() {
        super(SelectedTab.DATABASE);

        setContent(new H1("Database"));
    }
}
