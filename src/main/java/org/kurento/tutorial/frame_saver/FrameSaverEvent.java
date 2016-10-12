package org.kurento.tutorial.frame_saver;

import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

import org.kurento.client.Tag;
import org.kurento.client.MediaEvent;
import org.kurento.client.MediaObject;
import org.kurento.client.internal.server.Param;



public class FrameSaverEvent extends MediaEvent
{
    private static SimpleDateFormat s_DateFormat = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss.SSS");

    private static String           s_EventTime = s_DateFormat.format( new Date() );
    
    private static ArrayList<Tag>   s_EventTags = new ArrayList<Tag>();

    private String mEventInfo;  


    /**
     *  
     * Event raised by a {@link FrameSaverFilter} 
     *  
     * @param aSource --- Object that raised the event 
     * @param aType   --- Type of event 
     * @param aInfo   --- Info of event 
     *  
     **/
    public FrameSaverEvent(@Param("source") MediaObject source,
                           @Param("type")   String      type,
                           @Param("info")   String      info)
    {
        super(source,s_EventTime, s_EventTags, type);
        
        mEventInfo = info;
    }

    /**
     *  
     * Getter for mEventInfo property 
     *  
     * @return --- info of event
     **/
    public String getInfo()
    {
        return mEventInfo;
    }

    /**
     *  
     * Setter for mEventInfo property 
     *  
     * @param aInfo --- Info of event   
     *  
     **/
    public void setInfo(String aInfo)
    {
        this.mEventInfo = aInfo;
    }
}

