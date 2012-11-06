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

package com.streamreduce.rest.resource.api;

import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.OutboundConfigurationResponseDTO;
import com.streamreduce.rest.resource.ErrorMessage;
import com.streamreduce.util.JSONObjectBuilder;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;


/**
 * Integration test exercising the creation and updating of connections in {@link ConnectionResource} that hit
 * external systems that are connection providers or are hit through outbound configurations.
 */
public class ConnectionResource_CreateUpdateExternalConnectionsIT extends BaseConnectionResourceITCase {

    @Autowired
    ConnectionResource connectionResource;

    @Test
    public void testCreateConnection_pingdom() throws Exception {
        JSONObject connectionObject = new JSONObjectBuilder()
                .add("alias", "Test Pingdom Connection")
                .add("description", "Test Pingdom Connection")
                .add("visibility", "ACCOUNT")
                .add("providerId", "pingdom")
                .add("type", "feed")
                .add("authType", "USERNAME_PASSWORD_WITH_API_KEY")
                .add("credentials", new JSONObjectBuilder()
                        .add("identity", "integrations@nodeable.com")
                        .add("credential", "n0debelly!")
                        .add("api_key", "uchthbctjlcfjp1msah1u596usepm57p")
                        .build())
                .add("inbound", true)
                .build();

        Response createResponse = connectionResource.createConnection(connectionObject);
        ConnectionResponseDTO createConnectionResponseDTO = (ConnectionResponseDTO) createResponse.getEntity();
        Assert.assertNotNull(createConnectionResponseDTO);
    }

    @Test
    public void testCreateConnection_WebHDFS_invalidUrl() throws Exception {
        JSONObject connectionObject = new JSONObjectBuilder()
                .add("providerId", "custom")
                .add("alias", "addfsfsf")
                .add("type", "gateway")
                .add("visibility", "ACCOUNT")
                .add("credentials", new JSONObjectBuilder().build())
                .add("authType", "API_KEY")
                .array("outboundConfigurations", new JSONObjectBuilder()
                        .add("credentials", new JSONObjectBuilder()
                                .add("username", "df")
                                .build())
                        .add("protocol", "webhdfs")
                        .add("destination", "as")
                        .array("dataTypes", "INSIGHT")
                        .build())
                .build();

        Response createResponse = connectionResource.createConnection(connectionObject);
        Assert.assertEquals(createResponse.getStatus(), 400);
        ErrorMessage errorMessage = (ErrorMessage) createResponse.getEntity();
        Assert.assertEquals(errorMessage.getErrorMessage(), "The destination URL was invalid: no protocol: as/");
    }

    @Test
    public void testUpdateConnection_WebHDFS_invalidUrl() throws Exception {
        JSONObject connectionObject = new JSONObjectBuilder()
                .add("providerId", "rss")
                .add("alias", "Test Bad RSS Feed 1")
                .add("type", "feed")
                .add("url", ConnectionResourceITCase.class.getResource("/com/nodeable/rss/sample_EC2.rss").toString())
                .add("visibility", "ACCOUNT")
                .add("credentials", new JSONObjectBuilder().build())
                .add("authType", "USERNAME_PASSWORD")
                .array("outboundConfigurations", new JSONObjectBuilder()
                        .add("credentials", new JSONObjectBuilder()
                                .add("username", "hadoop")
                                .build())
                        .add("protocol", "webhdfs")
                        .add("destination", String.format("http://%s:%s/webhdfs/v1/", webhdfsProperties.getString("webhdfs.host"), webhdfsProperties.getString("webhdfs.port")))
                        .array("dataTypes", "INSIGHT")
                        .build())
                .build();

        JSONObject updateObject = new JSONObjectBuilder()
                .add("alias", "Test Bad RSS Feed 1")
                .array("outboundConfigurations", new JSONObjectBuilder()
                        .add("credentials", new JSONObjectBuilder()
                                .add("username", "df")
                                .build())
                        .add("protocol", "webhdfs")
                        .add("destination", "as")
                        .array("dataTypes", "INSIGHT")
                        .build())
                .build();

        Response createResponse = connectionResource.createConnection(connectionObject);
        ConnectionResponseDTO createConnectionResponseDTO = (ConnectionResponseDTO) createResponse.getEntity();
        Assert.assertEquals(200, createResponse.getStatus());
        Response updateResponse = connectionResource.updateConnection(createConnectionResponseDTO.getId(), updateObject);
        ErrorMessage errorMessage = (ErrorMessage) updateResponse.getEntity();
        Assert.assertEquals(errorMessage.getErrorMessage(), "The destination URL was invalid: no protocol: as/");
    }

    @Test
    public void testAddConnectionWithOutboundConfiguration() throws Exception {
        String testBucketName = getTestBucketName();
        JSONObject connectionObject = new JSONObjectBuilder()
                .add("alias", "Feed Connection, Ya'll")
                .add("description", "Feed Connection, Ya'll")
                .add("visibility", "ACCOUNT")
                .add("providerId", "rss")
                .add("type", "feed")
                .add("authType", "NONE")
                .add("url", ConnectionResourceITCase.class.getResource("/com/nodeable/rss/sample_EC2.rss").toString())
                .add("inbound", true)
                .array("outboundConfigurations", new JSONObjectBuilder()
                        .add("protocol", "s3")
                        .add("credentials", new JSONObjectBuilder()
                                .add("username", cloudProperties.getString("nodeable.aws.accessKeyId"))
                                .add("password", cloudProperties.getString("nodeable.aws.secretKey"))
                                .build())
                        .add("destination", "us-west-2")
                        .add("namespace", testBucketName)
                        .array("dataTypes", "PROCESSED")
                        .build()
                )
                .build();

        Response createResponse = connectionResource.createConnection(connectionObject);

        Assert.assertEquals(200, createResponse.getStatus());
        ConnectionResponseDTO updateConnectionResponseDTO = (ConnectionResponseDTO) createResponse.getEntity();
        Assert.assertEquals(1, updateConnectionResponseDTO.getOutboundConfigurations().size());
        OutboundConfigurationResponseDTO outboundConfigurationResponseDTO =
                updateConnectionResponseDTO.getOutboundConfigurations().get(0);
        Assert.assertEquals(testBucketName, outboundConfigurationResponseDTO.getNamespace());
        Assert.assertEquals("us-west-2", outboundConfigurationResponseDTO.getDestination());
    }

    @Test
    public void testUpdateConnectionWithOutboundConfiguration() throws Exception {
        //Create a connection to be used as an outbound connection first so we can re-use its id.
        JSONObject createConnectionObject = new JSONObjectBuilder()
                .add("alias", "Feed Connection, Ya'll")
                .add("description", "Feed Connection, Ya'll")
                .add("visibility", "ACCOUNT")
                .add("providerId", "rss")
                .add("type", "feed")
                .add("authType", "NONE")
                .add("url", ConnectionResourceITCase.class.getResource("/com/nodeable/rss/sample_EC2.rss").toString())
                .add("inbound", true)
                .build();

        String testBucketName = getTestBucketName();
        JSONObject updateConnectionObject = new JSONObjectBuilder()
                .add("alias", "Feed Connection, Ya'll")
                .array("outboundConfigurations", new JSONObjectBuilder()
                        .add("credentials", new JSONObjectBuilder()
                                .add("username", cloudProperties.getString("nodeable.aws.accessKeyId"))
                                .add("password", cloudProperties.getString("nodeable.aws.secretKey"))
                                .build())
                        .add("protocol", "s3")
                        .add("destination", "us-west-2")
                        .add("namespace", testBucketName)
                        .array("dataTypes", "PROCESSED")
                        .build())
                .build();


        Response createResponse = connectionResource.createConnection(createConnectionObject);
        ConnectionResponseDTO createConnectionResponseDTO = (ConnectionResponseDTO) createResponse.getEntity();

        Response updateResponse = connectionResource.updateConnection(createConnectionResponseDTO.getId(), updateConnectionObject);
        Assert.assertEquals(200, updateResponse.getStatus());
        ConnectionResponseDTO updateConnectionResponseDTO = (ConnectionResponseDTO) updateResponse.getEntity();
        Assert.assertEquals(1, updateConnectionResponseDTO.getOutboundConfigurations().size());
        OutboundConfigurationResponseDTO outboundConfigurationResponseDTO =
                updateConnectionResponseDTO.getOutboundConfigurations().get(0);
        Assert.assertEquals(testBucketName, outboundConfigurationResponseDTO.getNamespace());
        Assert.assertEquals("us-west-2", outboundConfigurationResponseDTO.getDestination());
    }

}
