package com.streamreduce.core.model.messages;

/**
 * MessageTypes are requied fror the #SobaMessageTransformerFactor and for the UI to render icons.
 */
public enum MessageType {
    SYSTEM, // reserved for nodeable use
    AGENT,
    USER,
    CONNECTION,
    INVENTORY_ITEM,
    ACTIVITY,
    GATEWAY,
    NODEBELLY
}
