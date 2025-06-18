package vciptvman.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportDialog extends Div {

    public enum ExportType {
        EPGD_CHANNELMAP,
        EPG_CHANNELLIST,
        STREAMS,
    }

    private final Dialog dialog;
    private String text;
    private String saveTo;
    private ExportType exportType;

    private boolean optionEpgdVdr;
    private boolean optionEpgdAllOther;

    private EpgdBufferCreator epgdBufferCreator;

    public ExportDialog(String title, String text, String saveTo, ExportType exportType, EpgdBufferCreator epgdBufferCreator) {
        this.text = text;
        this.saveTo = saveTo;
        this.exportType = exportType;
        this.epgdBufferCreator = epgdBufferCreator;

        optionEpgdVdr = false;
        optionEpgdAllOther = false;

        dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(false);

        VerticalLayout dialogLayout = createDialogLayout();
        dialog.add(dialogLayout);
        dialog.setDraggable(true);
        dialog.setResizable(true);

        add(dialog);

        UI.getCurrent().add(this);
    }

    public void open() {
        dialog.open();
    }

    private VerticalLayout createDialogLayout() {
        VerticalLayout dialogLayout = new VerticalLayout();

        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "1200px").set("max-width", "100%").set("height", "100%");

        FlexLayout buttons = new FlexLayout();

        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            dialog.close();
        });

        if (saveTo != null && !saveTo.isEmpty()) {
            Button saveToButton = new Button("Export to " + saveTo);
            saveToButton.addClickListener(e -> {
                BufferedWriter br = null;
                try {
                    br = new BufferedWriter(new FileWriter(new File(saveTo)));
                    br.write(text);
                    br.close();

                    saveToButton.setText("Exported to " + saveTo);
                    saveToButton.setEnabled(false);
                } catch (IOException ex) {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException exc) {
                            // finally ignore this exception
                        }
                    }
                }
            });

            buttons.add(saveToButton);
        }

        buttons.add(closeButton);

        TextArea textArea = new TextArea();
        textArea.setValue(text);

        if (exportType ==  ExportType.EPGD_CHANNELMAP) {
            FlexLayout options =  new FlexLayout();
            Checkbox oVdr = new Checkbox("Add VDR entries");
            oVdr.addValueChangeListener(event -> {
                optionEpgdVdr = event.getValue();

                textArea.setValue(epgdBufferCreator.createExportEpgdBuffer(optionEpgdVdr, optionEpgdAllOther).toString());
            });

            Checkbox oAllOthers = new Checkbox("Add VDR for all non-configured entries");
            oAllOthers.addValueChangeListener(event -> {
                optionEpgdAllOther = event.getValue();

                textArea.setValue(epgdBufferCreator.createExportEpgdBuffer(optionEpgdVdr, optionEpgdAllOther).toString());
            });

            options.add(oVdr,  oAllOthers);
            dialogLayout.add(textArea, options, buttons);
        } else {
            dialogLayout.add(textArea, buttons);
        }

        return dialogLayout;
    }
}

