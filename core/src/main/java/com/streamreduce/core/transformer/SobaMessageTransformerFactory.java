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

package com.streamreduce.core.transformer;

import java.util.Properties;

import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.core.transformer.message.AgentMessageTransformer;
import com.streamreduce.core.transformer.message.ConnectionMessageTransformer;
import com.streamreduce.core.transformer.message.InventoryItemMessageTransformer;
import com.streamreduce.core.transformer.message.MessageTransformer;
import com.streamreduce.core.transformer.message.NodebellyMessageTransformer;
import com.streamreduce.core.transformer.message.SobaMessageTransformer;
import com.streamreduce.core.transformer.message.SystemMessageTransformer;
import com.streamreduce.core.transformer.message.UserMessageTransformer;
import com.streamreduce.util.MessageUtils;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Temporary factory to transform based on message type. */

public final class SobaMessageTransformerFactory {

    protected static Logger logger = LoggerFactory.getLogger(SobaMessageTransformerFactory.class);
    protected  static JSONObject metricConfig = JSONObject.fromObject(MessageUtils.readJSONFromClasspath("/metricConfig.json"));

    public static MessageTransformerResult transformMessage(Event event, MessageType sobaMessageType,
                                            SobaMessageDetails messageDetails, Properties messageProperties) {

        MessageTransformer messageTransformer;

        switch (sobaMessageType) {
            case CONNECTION:
                messageTransformer = new ConnectionMessageTransformer(messageProperties, messageDetails);
                break;
            case INVENTORY_ITEM:
                messageTransformer = new InventoryItemMessageTransformer(messageProperties, messageDetails);
                break;
            case AGENT:
                messageTransformer = new AgentMessageTransformer(messageProperties, messageDetails);
                break;
            case USER:
                messageTransformer = new UserMessageTransformer(messageProperties, messageDetails);
                break;
            case SYSTEM:
                messageTransformer = new SystemMessageTransformer(messageProperties, messageDetails);
                break;
            case ACTIVITY:
                messageTransformer = new ItemActivityMessageTransformer(messageProperties, messageDetails);
                break;
            case NODEBELLY:
                messageTransformer = new NodebellyMessageTransformer(messageProperties, messageDetails, metricConfig);
                break;
            default:
                messageTransformer = new SobaMessageTransformer(messageProperties, messageDetails);
                break;
        }

        String msg = messageTransformer.doTransform(event);
        SobaMessageDetails sobaMessageDetails = messageTransformer.getDetails();

        logger.debug("[MESSAGE TRANSFORMATION FACTORY] Message to send as payload for : " + sobaMessageType + " : " + msg);

        return new MessageTransformerResult(msg,sobaMessageDetails);
    }
}
