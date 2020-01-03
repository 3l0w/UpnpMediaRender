package me.Fl0w.dnlaMediaRender;

import org.fourthline.cling.support.avtransport.impl.AVTransportStateMachine;
import org.seamless.statemachine.States;

@States({
        RendererNoMediaPresent.class,
        RendererStopped.class,
        RendererPlaying.class,
})
interface RendererStateMachine extends AVTransportStateMachine {}
