package org.kurento.tutorial.frame_saver;

import org.kurento.client.Filter;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.kurento.client.EventListener;
import org.kurento.client.internal.server.Param;

import com.kurento.kmf.media.AbstractBuilder;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.tool.rom.RemoteClass; 


@RemoteClass
public interface FrameSaverFilter extends Filter
{
    ListenerRegistration addFrameSaverListener(EventListener<FrameSaverEvent> listener); 
    
    void addFrameSaverListener(EventListener<FrameSaverEvent>      listener, 
                               Continuation<ListenerRegistration>  cont); 
    
    public interface Factory 
    { 
        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline); 
    } 
    
    public interface Builder extends AbstractBuilder<FrameSaverFilter> 
    { 
        public Builder withMediaPipeline(MediaPipeline mediaPipeline); 
    }     
    
    //public interface FrameSaverFilterBuilder extends MediaObjectBuilder
    //{
    //    public build (MediaPipeline mediaPipeline); 
    //}    
}


