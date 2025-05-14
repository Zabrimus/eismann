package vciptvman.view;

import com.vaadin.flow.router.Route;
import vciptvman.component.StreamComponent;

@Route("streams")
public class IPTVStreams extends MainView {

    public IPTVStreams() {
        super(SelectedTab.STREAMS);
        setContent(new StreamComponent(false));
    }
}
