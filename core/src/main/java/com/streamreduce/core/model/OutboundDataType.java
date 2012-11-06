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
