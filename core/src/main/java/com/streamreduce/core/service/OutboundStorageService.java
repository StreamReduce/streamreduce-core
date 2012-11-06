package com.streamreduce.core.service;

import com.streamreduce.OutboundStorageException;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.messages.SobaMessage;
import net.sf.json.JSONObject;


public interface OutboundStorageService {

    /**
     * <p>Routes a raw message to any outbound configurations defined on the passed in connection whose datatypes
     * include {@link com.streamreduce.core.model.OutboundDataType#RAW} and if the passed in connection belongs to an
     * account that has an active subscription or is otherwise allowed to send outbound messages. </p>
     *
     * <p>If a connection does not have a subscription, is not configured to send outbound, or contains no
     * outboundConfigurations whose datatypes include {@link com.streamreduce.core.model.OutboundDataType#RAW} then
     * this method is a noop.</p>
     *
     * <p>This method does not imply synchronous sending of the payload and may delay sending the payload.</p>
     *
     * @param jsonObject The payload to send outbound.
     * @param connection The connection the passed in jsonObject should be processed outbound for.
     * @return The number of times the passed in jsonObject was processed to be sent outbound.
     * @throws OutboundStorageException if there was a fatal error that kept the payload from being processed to send
     * outbound.
     */
    int sendRawMessage(JSONObject jsonObject, Connection connection) throws OutboundStorageException;

    /**
     * <p>Routes a {@link SobaMessage} to any outbound configurations defined on the passed in connection whose
     * datatypes match the type of SobaMessage passed in and if the passed in connection belongs to an
     * account that has an active subscription or is otherwise allowed to send outbound messages. </p>
     *
     * <p>If a connection does not have a subscription, is not configured to send outbound, or contains no
     * outboundConfigurations whose datatypes that are appropriate to the type of SobaMessage passed in, this method
     * is a noop.</p>
     *
     * <p>A SobaMessage that returns {@link com.streamreduce.core.model.messages.MessageType#NODEBELLY} from
     * {@link com.streamreduce.core.model.messages.SobaMessage#getType()} will be processed to send outbound for any
     * OutboundConfigurations whose whose datatypes include {@link com.streamreduce.core.model.OutboundDataType#INSIGHT}.
     * All other MessageTypes will be processed to send outbound for OutboundConfigurations whose whose datatypes
     * include {@link com.streamreduce.core.model.OutboundDataType#PROCESSED}.</p>
     *
     * <p>This method does not imply synchronous sending of the payload and may delay sending the payload.</p>
     *
     * @param sobaMessage The SobaMessage to send outbound.
     * @param connection The connection the passed in sobaMessage should be processed outbound for.
     * @return The number of times the passed in sobaMessage was processed to be sent outbound.
     * @throws OutboundStorageException if there was a fatal error that kept the payload from being processed to send
     * outbound.
     */
    int sendSobaMessage(SobaMessage sobaMessage, Connection connection) throws OutboundStorageException;

    /**
     * Overloaded version of
     * {@link OutboundStorageService#sendSobaMessage(com.streamreduce.core.model.messages.SobaMessage, com.streamreduce.core.model.Connection)}
     * that looks up the Connection to be used by inspecting the passed in sobaMessage.
     *
     * @param sobaMessage The SobaMessage to send outbound.
     * @return The number of times the passed in sobaMessage was processed to be sent outbound.
     * @throws OutboundStorageException if there was a fatal error that kept the payload from being processed to send
     * outbound.
     */
    int sendSobaMessage(SobaMessage sobaMessage) throws OutboundStorageException;
}
