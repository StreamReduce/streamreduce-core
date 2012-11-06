package com.streamreduce.core.transformer.message;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

public class InventoryItemMessageTransformer extends SobaMessageTransformer implements MessageTransformer {

    public InventoryItemMessageTransformer(Properties messageProperties, SobaMessageDetails messageDetails) {
        super(messageProperties, messageDetails);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String doTransform(Event event) {
        EventId eventId = event.getEventId();
        String msg;

        switch (eventId) {
            case CREATE:
            case UPDATE:
            case DELETE:
                msg = getGenericInventoryItemMessage(event);
                break;

            case CLOUD_INVENTORY_ITEM_REBOOT:
            case CLOUD_INVENTORY_ITEM_REBOOT_FAILURE:
            case CLOUD_INVENTORY_ITEM_TERMINATE:
            case CLOUD_INVENTORY_ITEM_TERMINATE_FAILURE:
                msg = getCloudInventoryItemMessage(event);
                break;

            default:
                msg = super.doTransform(event);
        }

        return msg;
    }

    private String getCloudInventoryItemMessage(Event event) {
        EventId eventId = event.getEventId();
        Map<String, Object> eventMetadata = event.getMetadata();
        String rawMessage;

        switch (eventId) {
            case CLOUD_INVENTORY_ITEM_REBOOT:
                rawMessage = (String) messageProperties.get("message.cloud.inventory.item.reboot.success");
                break;
            case CLOUD_INVENTORY_ITEM_REBOOT_FAILURE:
                rawMessage = (String) messageProperties.get("message.cloud.inventory.item.reboot.failure");
                break;
            case CLOUD_INVENTORY_ITEM_TERMINATE:
                rawMessage = (String) messageProperties.get("message.cloud.inventory.item.terminate.success");
                break;
            case CLOUD_INVENTORY_ITEM_TERMINATE_FAILURE:
                rawMessage = (String) messageProperties.get("message.cloud.inventory.item.terminate.failure");
                break;
            default:
                return "";
        }

        return MessageFormat.format(rawMessage,
                                    eventMetadata.get("sourceAlias"),
                                    eventMetadata.get("targetProviderDisplayName"),
                                    eventMetadata.get("targetConnectionAlias"));
    }

    private String getGenericInventoryItemMessage(Event event) {
        EventId eventId = event.getEventId();
        Map<String, Object> eventMetadata = event.getMetadata();
        String rawMessage;

        switch(eventId) {
            case CREATE:
                rawMessage = (String) messageProperties.get("message.inventory.item.created");
                break;
            case UPDATE:
                rawMessage = (String) messageProperties.get("message.inventory.item.updated");
                break;
            case DELETE:
                rawMessage = (String) messageProperties.get("message.inventory.item.deleted");
                break;
            default:
                return "";
        }

        return MessageFormat.format(rawMessage,
                                    eventMetadata.get("targetAlias"),
                                    eventMetadata.get("targetConnectionAlias"));
    }

}
