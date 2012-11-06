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
        testResource = new AbstractOwnableResource() {
            @Override
            protected Response addTag(ObjectId id, JSONObject hashtag) {
                return null;
            }

            @Override
            protected Response getTags(ObjectId id) {
                return null;
            }

            @Override
            protected Response removeTag(ObjectId id, String hashtag) {
                return null;
            }
        };

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
