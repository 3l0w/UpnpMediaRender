package me.Fl0w.dnlaMediaRender;

import org.fourthline.cling.support.avtransport.impl.state.AbstractState;
import org.fourthline.cling.support.avtransport.impl.state.Stopped;
import org.fourthline.cling.support.model.AVTransport;
import org.fourthline.cling.support.model.SeekMode;

import java.io.IOException;
import java.net.URI;

public class RendererStopped extends Stopped {
    VLCControl vlc = Main.getVLCControl();

    public RendererStopped(AVTransport transport) {
        super(transport);
    }

    public void onEntry() {
        super.onEntry();
        // Optional: Stop playing, release resources, etc.
    }

    public void onExit() {
        // Optional: Cleanup etc.
    }

    @Override
    public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {
        try {
            vlc.sendCommand("clear");
            vlc.sendCommand("add " + uri.toString() + "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(uri.toString());

        return RendererStopped.class;
    }

    @Override
    public Class<? extends AbstractState> stop() {
        /// Same here, if you are stopped already and someone calls STOP, well...
        return RendererStopped.class;
    }

    @Override
    public Class<? extends AbstractState> play(String speed) {
        // It's easier to let this classes' onEntry() method do the work
        try {
            vlc.sendCommand("play");
        } catch (IOException ignored) {
        }
        return RendererPlaying.class;
    }

    @Override
    public Class<? extends AbstractState> next() {
        return RendererStopped.class;
    }

    @Override
    public Class<? extends AbstractState> previous() {
        return RendererStopped.class;
    }

    @Override
    public Class<? extends AbstractState> seek(SeekMode unit, String target) {
        // Implement seeking with the stream in stopped state!
        System.out.println(unit.toString());
      /*  try {
            vlc.sendCommand("seek");
        } catch (IOException e) {
        }*/
        return RendererStopped.class;
    }
}
