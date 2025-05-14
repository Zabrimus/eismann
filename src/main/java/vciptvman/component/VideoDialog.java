package vciptvman.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoDialog extends Div {

    private final String videoUrl;
    private final Dialog dialog;
    private HlsVideo video;

    public VideoDialog(String name, String url) {
        videoUrl = url;

        dialog = new Dialog();
        dialog.setHeaderTitle(name);
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
        dialogLayout.getStyle().set("min-width", "800px").set("max-width", "100%").set("height", "100%").set("min-height", "600px");

        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            if (video != null) {
                dialogLayout.remove(video);
            }

            dialog.close();
        });

        // Try at first to load the m3u
        boolean connectionError = false;
        try {
            URL url = new URL(videoUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);

            int status = con.getResponseCode();
            if (status > 299 ) {
                connectionError = true;
            }
        } catch (IOException e) {
            connectionError = true;
        }

        if (!connectionError) {
            video = new HlsVideo();
            video.setSrc(videoUrl);
            video.setAutoPlay(true);

            video.addErrorListener(errorEvent -> {
                // video cannot be loaded.
                dialogLayout.remove(video);
            });

            dialogLayout.add(video, closeButton);
        } else {
            H3 errorText = new H3("Video file not found. Preview is not available.");
            dialogLayout.add(errorText, closeButton);
        }

        return dialogLayout;
    }
}