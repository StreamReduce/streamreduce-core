package com.streamreduce.client.outbound;

import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import net.sf.json.JSONObject;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for WebHDFSOutboundClient. There isn't much to test here since the majority of the logic is in WebHDFSClient.
 *
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/5/12 3:19 PM</p>
 */
public class WebHDFSOutboundClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void testPutRawMessage_emptyPayload() throws Exception {
        OutboundConfiguration outboundConfiguration = mock(OutboundConfiguration.class);
        when(outboundConfiguration.getDestination()).thenReturn("");
        new WebHDFSOutboundClient(outboundConfiguration).putRawMessage(new JSONObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutRawMessage_nullPayload() throws Exception {
        OutboundConfiguration outboundConfiguration = mock(OutboundConfiguration.class);
        when(outboundConfiguration.getDestination()).thenReturn("");
        new WebHDFSOutboundClient(outboundConfiguration).putRawMessage(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutProcessedMessage_nullSobaMessage() throws Exception {
        OutboundConfiguration outboundConfiguration = mock(OutboundConfiguration.class);
        when(outboundConfiguration.getDestination()).thenReturn("");
        new WebHDFSOutboundClient(outboundConfiguration).putProcessedMessage((SobaMessage)null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutProcessedMessage_nullSobaMessageResponseDTO() throws Exception {
        OutboundConfiguration outboundConfiguration = mock(OutboundConfiguration.class);
        when(outboundConfiguration.getDestination()).thenReturn("");
        new WebHDFSOutboundClient(outboundConfiguration).putProcessedMessage((SobaMessageResponseDTO)null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutInsightMessage_nullSobaMessage() throws Exception {
        OutboundConfiguration outboundConfiguration = mock(OutboundConfiguration.class);
        when(outboundConfiguration.getDestination()).thenReturn("");
        new WebHDFSOutboundClient(outboundConfiguration).putInsightMessage((SobaMessage) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutInsightMessage_nullSobaMessageResponseDTO() throws Exception {
        OutboundConfiguration outboundConfiguration = mock(OutboundConfiguration.class);
        when(outboundConfiguration.getDestination()).thenReturn("");
        new WebHDFSOutboundClient(outboundConfiguration).putInsightMessage((SobaMessageResponseDTO) null);
    }
}
