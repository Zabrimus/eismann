package vciptvman.component;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import org.apache.commons.lang3.StringUtils;
import vciptvman.database.BookmarkDatabase;
import vciptvman.database.EpgStreamDatabase;
import vciptvman.model.OtherEpgProvider;
import vciptvman.model.VdrChannel;
import vciptvman.view.Vdr;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VdrComponent extends VerticalLayout {

    private static EpgStreamDatabase epgstream;
    private static BookmarkDatabase bookmarks;

    private Grid<VdrChannel> vdrGrid;
    private List<OtherEpgProvider> otherEpgProviders;
    private Map<String, VdrChannel> vdrs = new HashMap<>();

    public VdrComponent() {
        epgstream = new EpgStreamDatabase();
        bookmarks = new BookmarkDatabase();

        otherEpgProviders = epgstream.getOtherEpgProvider();
        List<VdrChannel> channels = bookmarks.getVdrChannels();

        channels.stream().forEach(channel -> {
            vdrs.put(channel.xmltv_id(), channel);
        });

        createLayout();
    }

    private void createLayout() {
        assert epgstream != null;

        FlexLayout actions = new FlexLayout();
        actions.setFlexWrap(FlexLayout.FlexWrap.WRAP);

        TextField hostField = new TextField();
        hostField.setPlaceholder("VDR Host/IP");

        TextField portField = new TextField();
        portField.setPlaceholder("6419");

        Button importChannels = new Button("Read channel list");
        importChannels.addClickListener(e -> importChannels(hostField.getValue(), portField.getValue()));

        actions.add(hostField, portField, importChannels);

        vdrGrid = createVdrGrid(bookmarks.getVdrChannels());

        setSizeFull();
        setMargin(false);

        add(actions, vdrGrid);
    }

    private void importChannels(String host, String portStr) {
        int port;

        try {
            if (StringUtils.isEmpty(portStr)) {
                port = 6419;
            } else {
                port = Integer.parseInt(portStr);
            }

            if (StringUtils.isEmpty(host)) {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Error");
                dialog.setText(new Html("<p>Host is empty</p>"));
                dialog.setConfirmText("OK");
                dialog.open();

                return;
            }

            bookmarks.readVdrChannels(host, port);

            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Channels imported");
            dialog.setText(new Html("<p>All VDR channels are imported.</p>"));
            dialog.setConfirmText("OK");
            dialog.addConfirmListener(event -> vdrGrid.getDataProvider().refreshAll());
            dialog.open();

        } catch (IOException e) {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Error");
            dialog.setText(new Html("<p>Unable to connect to VDR</p>"));
            dialog.setConfirmText("OK");
            dialog.open();
        }
    }

    private Grid<VdrChannel> createVdrGrid(List<VdrChannel> vdrChannels) {
        Grid<VdrChannel> grid = new Grid<>(VdrChannel.class, false);

        grid.addColumn(vdrNameRenderer(grid)).setHeader("Channel").setSortable(true).setComparator(VdrChannel::name);
        grid.addColumn(xmltvRenderer(grid)).setHeader("XMLTV").setSortable(false);
        grid.addColumn(xmltvEpgRenderer(grid)).setHeader("EPG").setSortable(false);
        grid.addColumn(otherEpgRenderer(grid)).setHeader("Other EPG Provider").setSortable(false);
        grid.addColumn(createDeleteButton());

        grid.setItems(vdrChannels);

        return grid;
    }

    private Renderer<VdrChannel> vdrNameRenderer(Grid<VdrChannel> grid) {
        return new ComponentRenderer<>(vdr -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setMargin(false);
            layout.setSpacing(false);
            layout.setPadding(false);

            Div nameDiv = new Div();
            nameDiv.setText(vdr.name());

            Div idDiv = new Div();
            idDiv.setText(vdr.channel_id());

            layout.add(nameDiv, idDiv);

            return layout;
        });
    }

    private Renderer<VdrChannel> xmltvRenderer(Grid<VdrChannel> grid) {
        return new ComponentRenderer<>(vdr -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setMargin(false);
            layout.setSpacing(false);
            layout.setPadding(false);

            Div div = new Div();

            if (vdr.xmltv_id() != null) {
                div.setText(vdr.xmltv_id());
            } else {
                div.setText("-");
            }

            layout.add(div);

            return layout;
        });
    }

    private Renderer<VdrChannel> xmltvEpgRenderer(Grid<VdrChannel> grid) {
        return new ComponentRenderer<>(vdr -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setMargin(false);
            layout.setSpacing(false);
            layout.setPadding(false);

            Div div = new Div();
            if (vdr.site() != null) {
                div.setText(vdr.site());
            } else {
                div.setText("-");
            }

            layout.add(div);

            return layout;
        });
    }

    private Renderer<VdrChannel> otherEpgRenderer(Grid<VdrChannel> grid) {
        return new ComponentRenderer<>(vdr -> {
            ComboBox<OtherEpgProvider> otherEpg = new ComboBox<>();
            otherEpg.setItems(otherEpgProviders);

            otherEpg.setItemLabelGenerator(ch -> ch.name());
            otherEpg.setValue(otherEpgProviders.stream().filter(e -> e.name().equals(vdr.other_id())).findFirst().orElse(null));

            otherEpg.addValueChangeListener(event -> {
                bookmarks.setVdrChannel(vdr, event.getValue());
            });

            return otherEpg;
        });
    }

    private Renderer<VdrChannel> createDeleteButton() {
        return new ComponentRenderer<>(vdr -> {
            Button delete = new Button(new Icon(VaadinIcon.TRASH), event -> {
                bookmarks.deleteVdr(vdr.channel_id());
                vdrGrid.setItems(bookmarks.getVdrChannels());
            });

            return delete;
        });

    }

}
