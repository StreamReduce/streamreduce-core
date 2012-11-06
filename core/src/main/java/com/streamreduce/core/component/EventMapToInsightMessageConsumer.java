package com.streamreduce.core.component;

import java.util.Map;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.service.MessageService;
import net.sf.json.JSONObject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventMapToInsightMessageConsumer implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(EventMapToInsightMessageConsumer.class);
    private int messageCounter = 0;

    @Autowired
    MessageService messageService;

    public void receiveInsightMessageComponent(Map<String, Object> eventMap) {
        String account = eventMap.containsKey("account") && eventMap.get("account") != null ?
                (String) eventMap.get("account") :
                null;

        logger.info("Event " + ++messageCounter + " consumed from queue.  It was for account id " + account + ".");

        try {
            // just the basics to construct NB messages
            // we may add more info as needed in the MessageService
            // but avoid lookups here for now since soon this will just be a queue and not send messages.
            Event event = new Event();

            // TODO: This should be rewritten to use EventService#createEvent(...) and to have a real EventId

            if (!account.equals("global")) {
                event.setAccountId(new ObjectId(account));
            }
            event.setEventId(EventId.valueOf((String) eventMap.get("type")));

            //If this came from AMQ it may be be a serialized ObjectId, or if it
            //came from elsewhere it's a string representation of ObjectId.  Instantiate
            //a new ObjectId just in case.
            event.setTargetId(new ObjectId(eventMap.get("targetId").toString()));
            event.setMetadata(eventMap);

            messageService.sendNodebellyInsightMessage(event, (Long) eventMap.get("timestamp"), null);
        } catch (Exception e) {
            logger.error("Failed to send a NodebellyInsightMessage:  " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) throws Exception {
        String payload = exchange.getIn().getBody(String.class);
        JSONObject jsonObject = JSONObject.fromObject(payload);
        receiveInsightMessageComponent(jsonObject);
    }
}
