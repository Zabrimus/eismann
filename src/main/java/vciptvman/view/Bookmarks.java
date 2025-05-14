package vciptvman.view;

import com.vaadin.flow.router.Route;
import vciptvman.component.StreamComponent;

@Route("bookmarks")
public class Bookmarks extends MainView {

    public Bookmarks() {
        super(SelectedTab.BOOKMARKS);
        setContent(new StreamComponent(true));
    }
}
