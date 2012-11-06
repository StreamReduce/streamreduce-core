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
