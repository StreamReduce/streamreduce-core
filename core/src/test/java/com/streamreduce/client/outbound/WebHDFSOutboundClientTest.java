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
