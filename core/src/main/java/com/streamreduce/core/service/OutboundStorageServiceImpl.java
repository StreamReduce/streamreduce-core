package com.streamreduce.core.service;


import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.OutboundStorageException;
import com.streamreduce.client.outbound.OutboundClientFactory;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.dto.OutboundConfigurationWithPayloadDTO;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;

import java.io.IOException;

import net.sf.json.JSONObject;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections.MapUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class OutboundStorageServiceImpl extends AbstractService implements OutboundStorageService, CamelContextAware {

    @Autowired
    OutboundClientFactory outboundClientFactory;
    @Autowired
    ConnectionService connectionService;

    ProducerTemplate outboundStorageMessageProducer;

    @Override
    public int sendRawMessage(JSONObject jsonObject, Connection connection) throws OutboundStorageException {
        if (MapUtils.isEmpty(jsonObject) || CollectionUtils.isEmpty(connection.getOutboundConfigurations())) {
            return 0;
        }
        int numberOfMessagesRoutedOutbound = 0;
        for (OutboundConfiguration outboundConfiguration : connection.getOutboundConfigurations()) {
            if (outboundConfiguration.getDataTypes().contains(OutboundDataType.RAW)) {
                try {
                    routePayloadOutbound(outboundConfiguration, jsonObject.toString(), OutboundDataType.RAW);
                    numberOfMessagesRoutedOutbound++;
                } catch (Exception e) {
                    logger.error("Unable to route message outbound",e);
                }
            }
        }
        return numberOfMessagesRoutedOutbound;
    }

    @Override
    public int sendSobaMessage(SobaMessage sobaMessage) throws OutboundStorageException {
        try {
            Connection c = connectionService.getConnection(sobaMessage.getConnectionId());
            return sendSobaMessage(sobaMessage, c);
        } catch (ConnectionNotFoundException e) {
            throw new OutboundStorageException("Unable to retrieve Connection for sobaMessage with Id of "
                    + sobaMessage.getId(), e);
        }
    }

    @Override
    public int sendSobaMessage(SobaMessage sobaMessage, Connection connection) throws OutboundStorageException {
        OutboundDataType dataType = MessageType.NODEBELLY.equals(sobaMessage.getType()) ?
                OutboundDataType.INSIGHT : OutboundDataType.PROCESSED;
        return sendSobaMessageOutbound(sobaMessage, connection, dataType);
    }

    int sendSobaMessageOutbound(SobaMessage sobaMessage, Connection c, OutboundDataType outboundDataType)
            throws OutboundStorageException {

        int numberOfMessagesRoutedOutbound = 0;
        for (OutboundConfiguration outboundConfiguration : c.getOutboundConfigurations()) {
            if (outboundConfiguration.getDataTypes().contains(outboundDataType)) {
                try {
                    String sobaMessageJSONString = SobaMessageResponseDTO.fromSobaMessage(sobaMessage, true).toString();
                    routePayloadOutbound(outboundConfiguration, sobaMessageJSONString, outboundDataType);
                    numberOfMessagesRoutedOutbound++;
                } catch (Exception e) {
                    logger.error("Unable to route message outbound",e);
                }
            }
        }
        return numberOfMessagesRoutedOutbound;
    }

    void routePayloadOutbound(OutboundConfiguration outboundConfiguration, String payload,
                              OutboundDataType dataTypeForPayload) throws IOException {
        OutboundConfigurationWithPayloadDTO dto = new OutboundConfigurationWithPayloadDTO(outboundConfiguration,payload,dataTypeForPayload);
        String dtoAsJsonString = new ObjectMapper().writeValueAsString(dto);
        outboundStorageMessageProducer.sendBody(dtoAsJsonString);

    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        ProducerTemplate template = camelContext.createProducerTemplate();
        template.setDefaultEndpoint(camelContext.getEndpoint("direct:outbound-messages"));
        outboundStorageMessageProducer = template;
    }

    @Override
    @Deprecated
    /**
     * Do not call this method.  This CamelContextAware does not expose the CamelContext to other classes.  It is
     * CamelContextAware only to have the context injected in to it.
     */
    public CamelContext getCamelContext() {
        return null;
    }
}
