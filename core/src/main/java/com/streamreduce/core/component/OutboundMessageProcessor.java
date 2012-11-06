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

package com.streamreduce.core.component;

import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.OutboundStorageException;
import com.streamreduce.client.outbound.OutboundClient;
import com.streamreduce.client.outbound.OutboundClientFactory;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.dto.OutboundConfigurationServiceDTO;
import com.streamreduce.core.model.dto.OutboundConfigurationWithPayloadDTO;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import net.sf.json.JSONObject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OutboundMessageProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(OutboundMessageProcessor.class);

    @Autowired
    ConnectionService connectionService;

    @Autowired
    OutboundClientFactory outboundClientFactory;

    private ObjectMapper objectMapper;

    OutboundMessageProcessor() {
        objectMapper = new ObjectMapper();
    }



    @Override
    public void process(Exchange exchange) throws Exception {
        try {

            String dtoAsString = exchange.getIn().getBody(String.class);
            OutboundConfigurationWithPayloadDTO dto = objectMapper.readValue(dtoAsString,
                    OutboundConfigurationWithPayloadDTO.class);
            OutboundConfiguration outboundConfiguration =
                    createOutboundConfigurationFromDto(dto.getOutboundConfiguration());
            OutboundClient outboundClient =
                    outboundClientFactory.createOutboundClientForOutboundConfiguration(outboundConfiguration);

            if (dto.getDataType() == OutboundDataType.RAW) {
                outboundClient.putRawMessage(JSONObject.fromObject(dto.getPayload()));
            } else if (dto.getDataType() == OutboundDataType.PROCESSED) {
                SobaMessageResponseDTO responseDTO =
                        objectMapper.readValue(dto.getPayload(), SobaMessageResponseDTO.class);
                outboundClient.putProcessedMessage(responseDTO);
            } else if (dto.getDataType() == OutboundDataType.INSIGHT) {
                SobaMessageResponseDTO responseDTO =
                        objectMapper.readValue(dto.getPayload(), SobaMessageResponseDTO.class);
                outboundClient.putInsightMessage(responseDTO);
            } else {
                throw new OutboundStorageException("The received OutboundConfigurationWithPayloadDTO does not " +
                        "specify a valid dataType.  Was " + dto.getDataType() +
                        " but was expecting one of [RAW, PROCESSED, or Insight]");
            }
        } catch (Exception e) {
            logger.error("Unable to send payload outbound", e);
        }
    }

    private OutboundConfiguration createOutboundConfigurationFromDto(OutboundConfigurationServiceDTO dto) {
        try {
            Connection originatingConnection = connectionService.getConnection(dto.getOriginatingConnectionId());
            return new OutboundConfiguration.Builder()
                    .protocol(dto.getProtocol())
                    .destination(dto.getDestination())
                    .namespace(dto.getNamespace())
                    .dataTypes(dto.getDataTypes().toArray(new OutboundDataType[dto.getDataTypes().size()]))
                    .credentials(dto.getCredentials())
                    .originatingConnection(originatingConnection)
                    .build();
        } catch (ConnectionNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
