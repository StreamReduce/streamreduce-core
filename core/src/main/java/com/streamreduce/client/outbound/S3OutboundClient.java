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

package com.streamreduce.client.outbound;

import com.streamreduce.OutboundStorageException;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import com.streamreduce.util.AWSClient;
import net.sf.json.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3OutboundClient extends AbstractOutboundClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3OutboundClient.class);

    S3OutboundClient(OutboundConfiguration outboundConfiguration) {
        super(outboundConfiguration);
    }

    @Override
    public void putRawMessage(JSONObject payload) throws OutboundStorageException {
        if (MapUtils.isEmpty(payload)) {
            throw new IllegalArgumentException("payload must be non-empty");
        }
        try {
            String key = createRawPayloadKey(payload);
            byte[] payloadAsBytes = payload.toString().getBytes();
            sendPayload(key,payloadAsBytes);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putProcessedMessage(SobaMessage sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }

        try {
            String key = createProcessedMessagePath(sobaMessage);
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            sendPayload(key, payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putProcessedMessage(SobaMessageResponseDTO sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }
        try {
            String key = createProcessedMessagePath(sobaMessage);
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            sendPayload(key, payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putInsightMessage(SobaMessage sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }

        try {
            String key = createInsightMessagePath(sobaMessage);
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            sendPayload(key, payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putInsightMessage(SobaMessageResponseDTO sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }

        try {
            String key = createInsightMessagePath(sobaMessage);
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            sendPayload(key,payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    private void sendPayload(String key, byte[] payload) throws OutboundStorageException {
        try {
            AWSClient awsClient = new AWSClient(outboundConfiguration);
            String result = awsClient.pushToS3(outboundConfiguration, key, payload);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("rawMessage sent to S3 identified by: " + result);
            }
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }
}
