package vciptvman.view;

import com.vaadin.flow.router.Route;
import vciptvman.component.StreamComponent;
import vciptvman.component.VdrComponent;

@Route("vdr")
public class Vdr extends MainView {

    public Vdr() {
        super(SelectedTab.VDR);
        setContent(new VdrComponent());
    }
}
