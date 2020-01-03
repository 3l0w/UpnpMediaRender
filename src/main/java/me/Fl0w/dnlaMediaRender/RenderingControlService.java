package me.Fl0w.dnlaMediaRender;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.renderingcontrol.AbstractAudioRenderingControl;
import org.fourthline.cling.support.renderingcontrol.RenderingControlException;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelMute;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

import java.io.IOException;
import java.util.logging.Logger;

public class RenderingControlService extends AbstractAudioRenderingControl {
    VLCControl vlc = Main.getVLCControl();
    private UnsignedIntegerFourBytes instanceID = new UnsignedIntegerFourBytes(0);
    private final Logger LOGGER = Logger.getLogger(RenderingControlService.class.getName());
    private int volume = 100;
    public RenderingControlService(){
      //  renderingControlHandler.getRendererMuteEvent().addListener(this::onVideoMuteChange);
    //    renderingControlHandler.getRendererVolumeEvent().addListener(this::onVideoVolumeChange);
    }

    @Override
    public boolean getMute(UnsignedIntegerFourBytes instanceID, String channelName) throws RenderingControlException {
     //   return renderingControlHandler.isMute();
        return volume == 0;
    }

    @Override
    public void setMute(UnsignedIntegerFourBytes instanceID, String channelName, boolean desiredMute) throws RenderingControlException {
        try {
            vlc.sendCommand("volume " + (desiredMute ? 0 : (int) Math.ceil((double)volume* 2.56)));

        } catch (IOException e) {
            e.printStackTrace();
        }    }

    @Override
    public UnsignedIntegerTwoBytes getVolume(UnsignedIntegerFourBytes instanceID, String channelName) throws RenderingControlException {
        return new UnsignedIntegerTwoBytes(volume);
    }

    @Override
    public void setVolume(UnsignedIntegerFourBytes instanceID, String channelName, UnsignedIntegerTwoBytes desiredVolume) throws RenderingControlException {
       // renderingControlHandler.setVideoVolume(desiredVolume.getValue().doubleValue());
        try {
            volume = desiredVolume.getValue().intValue();
            int sendVolume = (int) Math.ceil((double)volume* 2.56);
            vlc.sendCommand("volume "+sendVolume);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected Channel[] getCurrentChannels() {
        return new Channel[]{Channel.Master};
    }

    @Override
    public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
        return new UnsignedIntegerFourBytes[]{instanceID};
    }

    private void onVideoVolumeChange(double volume){
        LOGGER.fine("Notifying control point about volume change to " + volume);
        getLastChange().setEventedValue(
                instanceID,
                new RenderingControlVariable.Volume(new ChannelVolume(Channel.Master, (int)volume))
        );
    }

    private void onVideoMuteChange(boolean mute){
        LOGGER.fine("Notifying control point about mute change to " + mute);
        getLastChange().setEventedValue(
                instanceID,
                new RenderingControlVariable.Mute(new ChannelMute(Channel.Master, mute))
        );
    }
}
