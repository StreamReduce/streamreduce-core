package com.streamreduce.core.transformer.message;

import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

import java.util.Properties;

public class UserMessageTransformer extends SobaMessageTransformer implements MessageTransformer {

    public UserMessageTransformer(Properties messageProperties, SobaMessageDetails messageDetails) {
        super(messageProperties, messageDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doTransform(Event event) {
        return super.doTransform(event);
    }

}
