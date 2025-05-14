package vciptvman.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class ExportDialog extends Div {

    private final Dialog dialog;
    private String text;

    public ExportDialog(String title, String text) {
        this.text = text;

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

        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            dialog.close();
        });

        TextArea textArea = new TextArea();
        textArea.setValue(text);

        dialogLayout.add(textArea, closeButton);

        return dialogLayout;
    }
}

