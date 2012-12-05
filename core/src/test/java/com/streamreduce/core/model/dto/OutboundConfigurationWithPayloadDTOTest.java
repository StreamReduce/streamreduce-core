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

package com.streamreduce.core.model.dto;

import com.google.common.collect.Lists;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.test.service.TestUtils;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class OutboundConfigurationWithPayloadDTOTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInstantiateOutboundConfigurationWithPayload_NullOutboundConfiguration() {
        new OutboundConfigurationWithPayloadDTO((OutboundConfiguration)null,"{}",OutboundDataType.INSIGHT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstantiateOutboundConfigurationWithPayload_NullOutboundConfigurationServiceDTO() {
        new OutboundConfigurationWithPayloadDTO((OutboundConfigurationServiceDTO)null,"{}",OutboundDataType.INSIGHT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstantiateOutboundConfigurationWithPayload_NullPayload() {
        new OutboundConfigurationWithPayloadDTO(mock(OutboundConfiguration.class),null,OutboundDataType.INSIGHT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstantiateOutboundConfigurationWithPayload_NonJSONObjectPayload() {
        new OutboundConfigurationWithPayloadDTO(mock(OutboundConfiguration.class),"[what]",OutboundDataType.INSIGHT);
    }

    @Test
    public void testValidInstantiation() {
        Connection c = TestUtils.createTestFeedConnection(OutboundDataType.PROCESSED);
        ObjectId testObjectId = new ObjectId();
        c.setId(testObjectId);
        OutboundConfiguration outboundConfiguration = Lists.newArrayList(c.getOutboundConfigurations()).get(0);
        OutboundConfigurationWithPayloadDTO dto = new OutboundConfigurationWithPayloadDTO(
                outboundConfiguration,"{\"foo\":\"bar\"}", OutboundDataType.PROCESSED);

        Assert.assertEquals(OutboundDataType.PROCESSED,dto.getDataType());
        Assert.assertEquals(new OutboundConfigurationServiceDTO(outboundConfiguration),dto.getOutboundConfiguration());
        Assert.assertEquals("{\"foo\":\"bar\"}",dto.getPayload());
    }

    @Test
    public void testSerializeDeserialize() throws Exception{
        Connection c = TestUtils.createTestFeedConnection(OutboundDataType.PROCESSED);
        ObjectId testObjectId = new ObjectId();
        c.setId(testObjectId);
        OutboundConfiguration outboundConfiguration = Lists.newArrayList(c.getOutboundConfigurations()).get(0);
        OutboundConfigurationWithPayloadDTO expected = new OutboundConfigurationWithPayloadDTO(
                outboundConfiguration,"{\"foo\":\"bar\"}", OutboundDataType.PROCESSED);

        ObjectMapper om = new ObjectMapper();
        String s = om.writeValueAsString(expected);
        OutboundConfigurationWithPayloadDTO deserialized = om.readValue(s,OutboundConfigurationWithPayloadDTO.class);
        Assert.assertEquals(expected,deserialized);
    }

    @Test
    public void testSerializeDeserializeEncryptsCredentials() throws Exception{
        String beforeEncryption = "k32hgr23y4uiofg32qiofygq3oyfiqgf4gt3qggq3g34gqagegeqg";

        ConnectionCredentials connectionCredentials = new ConnectionCredentials();
        connectionCredentials.setIdentity("foo");
        connectionCredentials.setApiKey(beforeEncryption);
        connectionCredentials.setCredential(beforeEncryption);
        connectionCredentials.setOauthToken(beforeEncryption);
        connectionCredentials.setOauthTokenSecret(beforeEncryption);

        OutboundConfiguration outboundConfiguration =  new OutboundConfiguration.Builder()
                .dataTypes(OutboundDataType.PROCESSED)
                .protocol("s3")
                .credentials(connectionCredentials)
                .build();

        OutboundConfigurationWithPayloadDTO dto = new OutboundConfigurationWithPayloadDTO(
                outboundConfiguration,"{\"foo\":\"bar\"}", OutboundDataType.PROCESSED);

        ObjectMapper om = new ObjectMapper();
        String s = om.writeValueAsString(dto);


        JSONObject credentialsAsJSON = JSONObject.fromObject(s).getJSONObject("outboundConfiguration").getJSONObject("credentials");
        Assert.assertFalse(credentialsAsJSON.getString("credential").contains(beforeEncryption));
        Assert.assertFalse(credentialsAsJSON.getString("apiKey").contains(beforeEncryption));
        Assert.assertFalse(credentialsAsJSON.getString("oauthToken").contains(beforeEncryption));
        Assert.assertFalse(credentialsAsJSON.getString("oauthTokenSecret").contains(beforeEncryption));

    }




}
