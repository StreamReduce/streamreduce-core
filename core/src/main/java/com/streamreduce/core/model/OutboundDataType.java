package com.streamreduce.core.model;

/**
 * Enum representing the possible allowed types of messages that will be sent to an outbound
 * connection.
 */
public enum OutboundDataType {
    /**
     * Outbound type indicating anything that is output to an outbound connection exactly as it is received when
     * the message/event was originally sent to or polled by Nodeable.
     */
    RAW,

    /**
     * Outbound type indicating any {@link com.streamreduce.core.model.messages.SobaMessage} created from within Nodeable
     * after it has been received from an inbound connection and persisted by the MessageService, but does not include
     * insight messages.
     */
    PROCESSED,

    /**
     * Outbound type indicating all messages that are generated through Nodeable analytics.
     */
    INSIGHT,

    /**
     * Outbound type indicating all generated {@link Event} objects that are created and persisted internally
     * within Nodeable.
     */
    EVENT
}
