package com.streamreduce.core.transformer.message;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

public class SystemMessageTransformer extends SobaMessageTransformer implements MessageTransformer {

    public SystemMessageTransformer(Properties messageProperties, SobaMessageDetails messageDetails) {
        super(messageProperties, messageDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doTransform(Event event) {
        EventId eventId = event.getEventId();
        Map<String, Object> eventMetadata = event.getMetadata();
        String userFullname = (String)eventMetadata.get("targetFullname");
        String userAlias = (String)eventMetadata.get("targetAlias");
        String msg;

        // System Message types control the add/remove users messages
        switch (eventId) {
            case CREATE:
                msg = MessageFormat.format((String) messageProperties.get("message.user.joined"), userFullname, userAlias);
                break;

            case DELETE:
                msg = MessageFormat.format((String) messageProperties.get("message.user.deleted"), userFullname, userAlias);
                break;

            case SUBSCRIPTION_TRIAL_EXPIRING:
                msg = MessageFormat.format((String) messageProperties.get("message.trial.subscription.expiring"), "");
                break;

            case SUBSCRIPTION_TRIAL_EXPIRED:
                msg = MessageFormat.format((String) messageProperties.get("message.trial.subscription.expired"), "");
                break;

            default:
                msg = super.doTransform(event);
                break;
        }

        return msg;
    }

}
