package com.streamreduce.core.transformer.message;

import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

public interface MessageTransformer {

    /**
     * Creates a message based on the event.
     *
     * @param event the event to create the message from
     *
     * @return the message
     */
    public String doTransform(Event event);


    public SobaMessageDetails getDetails();

}
