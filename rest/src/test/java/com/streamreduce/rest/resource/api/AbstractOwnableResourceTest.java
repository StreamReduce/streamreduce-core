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

import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.GitHubProjectHostingProvider;
import com.streamreduce.connections.ProjectHostingProvider;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.OutboundConfigurationResponseDTO;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class to exercise the dto methods
 */
public class AbstractOwnableResourceTest {
    AbstractOwnableResource testResource;
    User testUser;

    @Before
    public void setUp() {
        testResource = mock(AbstractOwnableResource.class);

        testUser = new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build();
        testUser.setId(new ObjectId());
        testResource.securityService = mock(SecurityService.class);
        when(testResource.securityService.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    public void testConnectionToDTO() {
        GitHubProjectHostingProvider mockProvider = Mockito.mock(GitHubProjectHostingProvider.class);
        when(mockProvider.getId()).thenReturn(ProviderIdConstants.GITHUB_PROVIDER_ID);
        when(mockProvider.getType()).thenReturn(ProjectHostingProvider.TYPE);

        Connection c = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(mockProvider)
                .user(testUser)
                .url("http://someUrl" )
                .authType(AuthType.NONE)
                .outboundConfigurations(
                        new OutboundConfiguration.Builder()
                                .credentials(new ConnectionCredentials("accessKey", "secretKey" ))
                                .protocol("s3" )
                                .destination("my.bucket.name" )
                                .namespace("/key/prefix/here/" )
                                .dataTypes(OutboundDataType.PROCESSED)
                                .build()
                )
                .build();

        when(testResource.toFullDTO(any(Connection.class))).thenCallRealMethod();
        when(testResource.toOwnerDTO(any(Connection.class),any(ConnectionResponseDTO.class))).thenCallRealMethod();
        ConnectionResponseDTO dto = testResource.toFullDTO(c);

        //Test some fields
        Assert.assertEquals(AuthType.NONE, dto.getAuthType());
        Assert.assertEquals(c.getAlias(), dto.getAlias());
        Assert.assertEquals(c.getUrl(), dto.getUrl());

        List<OutboundConfigurationResponseDTO> outboundDTOs = dto.getOutboundConfigurations();
        Assert.assertEquals(1, outboundDTOs.size());
        Assert.assertEquals("s3", outboundDTOs.get(0).getProtocol());
        Assert.assertEquals("my.bucket.name", outboundDTOs.get(0).getDestination());
        Assert.assertEquals("/key/prefix/here/", outboundDTOs.get(0).getNamespace());
        Assert.assertEquals("PROCESSED", outboundDTOs.get(0).getDataTypes().get(0).toString());
    }

}
