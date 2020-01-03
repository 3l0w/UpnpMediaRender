package me.Fl0w.dnlaMediaRender;

import org.fourthline.cling.support.avtransport.impl.state.AbstractState;
import org.fourthline.cling.support.avtransport.impl.state.Playing;
import org.fourthline.cling.support.model.AVTransport;
import org.fourthline.cling.support.model.SeekMode;

import java.io.IOException;
import java.net.URI;

public class RendererPlaying extends Playing {
    VLCControl vlc = Main.getVLCControl();

    public RendererPlaying(AVTransport transport) {
        super(transport);
    }

    @Override
    public void onEntry() {
        super.onEntry();
        // Start playing now!
    }

    @Override
    public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {
        try {
            vlc.sendCommand("clear");
            vlc.sendCommand("add " + uri.toString() + "");
        } catch (IOException ex) {
            return RendererNoMediaPresent.class;
        }
        System.out.println(uri.toString());

        return RendererStopped.class;
    }

    @Override
    public Class<? extends AbstractState> stop() {
        // Stop playing!
        try {
            vlc.sendCommand("stop");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return RendererStopped.class;
    }

    @Override
    public Class<? extends AbstractState> play(String s) {
        try {
            vlc.sendCommand("play");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.getClass();
    }

    @Override
    public Class<? extends AbstractState> pause() {
        try {
            vlc.sendCommand("pause");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return RendererPlaying.class;
    }

    @Override
    public Class<? extends AbstractState<?>> next() {
        return null;
    }

    @Override
    public Class<? extends AbstractState<?>> previous() {
        return null;
    }

    @Override
    public Class<? extends AbstractState<?>> seek(SeekMode seekMode, String s) {
        return null;
    }
}