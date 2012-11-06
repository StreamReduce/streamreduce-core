package com.streamreduce.core.transformer;

import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.core.transformer.message.MessageTransformer;
import com.streamreduce.core.transformer.message.SobaMessageTransformer;

import java.util.Map;
import java.util.Properties;

public class ItemActivityMessageTransformer extends SobaMessageTransformer implements MessageTransformer {

    public ItemActivityMessageTransformer(Properties messageProperties, SobaMessageDetails messageDetails) {
        super(messageProperties, messageDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doTransform(Event event) {
        Map<String, Object> eventMetadata = event.getMetadata();
        StringBuilder sb = new StringBuilder();

        // TODO: We should generate messages here based on the event instead of making the event poller do it

        // Title
        sb.append((String) eventMetadata.get("activityTitle"));

        // Content
        if (eventMetadata.get("activityContent") != null) {
            sb.append("\n");
            sb.append((String) eventMetadata.get("activityContent"));
        }

        return sb.toString();
    }
}