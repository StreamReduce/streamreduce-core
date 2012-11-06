package com.streamreduce.client.outbound;

import com.streamreduce.OutboundStorageException;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import net.sf.json.JSONObject;

/**
 * Contract to be implemented for any class that sends messages/payloads to an
 * {@link com.streamreduce.core.model.OutboundConfiguration}.
 *
 * All OutboundClients should support sending raw json payloads, and SobaMessages.  A SobaMessage classification is
 * determined by whether the putInsightMessage or putProcessedMessage is called.
 *
 * It is up to the OutboundClient implementation to determine where on the outbound provider any payloads are
 * sent to based on the method called and what is contained in the OutboundConfiguration used to instantiate the
 * OutboundClient.
 */
public interface OutboundClient {

    /**
     * Sends a raw JSONObject payload outbound.
     *
     * @param payload The payload to be sent outbound.
     * @throws OutboundStorageException if there was an error sending the payload outbound.
     * @throws IllegalArgumentException if payload is null.
     */
    void putRawMessage(JSONObject payload) throws OutboundStorageException;

    /**
     * Sends a SobaMessage outbound as a PROCESSED Message.
     *
     * @param sobaMessage The payload to be sent outbound.
     * @throws OutboundStorageException if there was an error sending the payload outbound.
     * @throws IllegalArgumentException if sobaMessage was null.
     */
    void putProcessedMessage(SobaMessage sobaMessage) throws OutboundStorageException;

    /**
     * Sends a SobaMessage outbound as an INSIGHT Message.
     *
     * @param sobaMessage The payload to be sent outbound.
     * @throws OutboundStorageException if there was an error sending the payload outbound.
     * @throws IllegalArgumentException if payload was null.
     */
    void putInsightMessage(SobaMessage sobaMessage) throws OutboundStorageException;

    /**
     * Sends a {@link SobaMessageResponseDTO} outbound as a PROCESSED Message.  This method should be called if
     * the caller already has access to a SobaMessageResponseDTO instance but not a {@link SobaMessage} instance.
     *
     * @param sobaMessage The payload to be sent outbound.
     * @throws OutboundStorageException if there was an error sending the payload outbound.
     * @throws IllegalArgumentException if payload was null.
     */
    void putProcessedMessage(SobaMessageResponseDTO sobaMessage) throws OutboundStorageException;

    /**
     * Sends a {@link SobaMessageResponseDTO} outbound as an INSIGHT Message.  This method should be called if
     * the caller already has access to a SobaMessageResponseDTO instance but not a {@link SobaMessage} instance.
     *
     * @param sobaMessage The payload to be sent outbound.
     * @throws OutboundStorageException if there was an error sending the payload outbound.
     * @throws IllegalArgumentException if payload was null.
     */
    void putInsightMessage(SobaMessageResponseDTO sobaMessage) throws OutboundStorageException;
}
