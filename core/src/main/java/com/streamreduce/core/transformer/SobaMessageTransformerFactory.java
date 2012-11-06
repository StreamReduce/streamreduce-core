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
