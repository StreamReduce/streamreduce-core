/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
