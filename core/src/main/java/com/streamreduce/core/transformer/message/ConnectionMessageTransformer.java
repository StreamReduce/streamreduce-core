package com.streamreduce.core.transformer.message;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

public class ConnectionMessageTransformer extends SobaMessageTransformer implements MessageTransformer {

    public ConnectionMessageTransformer(Properties messageProperties, SobaMessageDetails messageDetails) {
        super(messageProperties, messageDetails);
    }

    @Override
    public String doTransform(Event event) {
        EventId eventId = event.getEventId();
        Map<String, Object> eventMetadata = event.getMetadata();
        String rawMessage;

        switch (eventId) {
            case CREATE:
                rawMessage = (String) messageProperties.get("message.connection.created");
                break;
            case DELETE:
                rawMessage = (String) messageProperties.get("message.connection.deleted");
                break;
            case UPDATE:
                rawMessage = (String) messageProperties.get("message.connection.updated");
                break;
            //case OTHER_CONNECTION_TYPES....
            default:
                return super.doTransform(event);
        }

        return MessageFormat.format(rawMessage,
                                    eventMetadata.get("sourceAlias"),
                                    eventMetadata.get("targetProviderDisplayName"),
                                    eventMetadata.get("targetAlias"));
    }

}
