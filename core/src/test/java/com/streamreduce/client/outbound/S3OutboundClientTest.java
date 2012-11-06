package com.streamreduce.client.outbound;

import com.streamreduce.core.model.OutboundConfiguration;
import net.sf.json.JSONObject;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class S3OutboundClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void testPutRawMessageWithNullPayload() throws Exception {
        new S3OutboundClient(mock(OutboundConfiguration.class)).putRawMessage(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutRawMessageWithBlankPayload() throws Exception {
        new S3OutboundClient(mock(OutboundConfiguration.class)).putRawMessage(new JSONObject());
    }
}
