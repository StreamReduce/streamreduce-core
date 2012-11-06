package com.streamreduce.client.outbound;


import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import junit.framework.Assert;
import org.junit.Test;

public class OutboundClientFactoryTest {

    @Test
    public void testOutboundConnectionForS3ReturnsS3OutboundClient() {
        OutboundConfiguration outboundAWSConnection = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .dataTypes(OutboundDataType.RAW)
                .protocol("s3")
                .build();

        OutboundClient outboundClient = new OutboundClientFactory()
                .createOutboundClientForOutboundConfiguration(outboundAWSConnection);

        Assert.assertEquals(S3OutboundClient.class,outboundClient.getClass());
    }

    @Test
    public void testOutboundConnectionForWebHDFSReturnsWebHDFSOutboundClient() {
        OutboundConfiguration outboundAWSConnection = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .dataTypes(OutboundDataType.RAW)
                .destination("http://www.test.com/webhdfs/v1/")
                .protocol("webhdfs")
                .build();

        OutboundClient outboundClient = new OutboundClientFactory()
                .createOutboundClientForOutboundConfiguration(outboundAWSConnection);

        Assert.assertEquals(WebHDFSOutboundClient.class,outboundClient.getClass());
    }
}
