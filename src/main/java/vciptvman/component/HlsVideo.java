package vciptvman.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

@Tag("hls-video")
@JsModule("hls-video-element")
public class HlsVideo extends Component {
    public HlsVideo() {
        getElement().setProperty("controls", true);
    }

    public void setSrc(String src) {
        getElement().setProperty("src", src);
    }

    public void setAutoPlay(boolean auto) {
        getElement().setProperty("autoplay", auto);
    }

    public Registration addErrorListener(ComponentEventListener<VideoErrorEvent> listener) {
        return addListener(VideoErrorEvent.class, listener);
    }

    // FIXME: Welches Event kann hier angefangen werden?
    @DomEvent("error")
    public static class VideoErrorEvent extends ComponentEvent<HlsVideo> {
        public VideoErrorEvent(HlsVideo source, boolean fromClient) {
            super(source, fromClient);
        }
    }
}