package com.streamreduce.core.service;

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.dao.ConnectionDAO;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/30/12 16:43</p>
 */
public class ConnectionManagementServiceImplITCase {

    @Mock
    private ConnectionDAO mockConnectionDAO;

    private User testUser;
    private List<Connection> feedConnections;

    private ConnectionManagementServiceImpl connectionManagementService = new ConnectionManagementServiceImpl();

    @Before
    public void setUp() throws Exception {
        testUser = new User.Builder().account(new Account.Builder().name("ABC").build()).username("sampleUser").build();
        feedConnections = Arrays.asList(
                new Connection.Builder()
                        .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                        .url("http://foo.url1.com/rss")
                        .credentials(new ConnectionCredentials("ident", "pass"))
                        .alias("connection1")
                        .user(testUser)
                        .authType(AuthType.NONE)
                        .build(),
                new Connection.Builder()
                        .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                        .url("http://foo.url2.com/nocreds")
                        .alias("connection2_nocreds")
                        .user(testUser)
                        .authType(AuthType.NONE)
                        .build(),
                new Connection.Builder()
                        .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                        .url("http://feeds.venturebeat.com/Venturebeat?format=xml")
                        .authType(AuthType.NONE)
                        .alias("VentureBeat")
                        .user(testUser)
                        .visibility(SobaObject.Visibility.ACCOUNT)
                        .hashtag("rss")
                        .build()
        );
        for (Connection connection : feedConnections) {
            connection.setId(new ObjectId());
            connection.setCreated(new Date().getTime());
        }

        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(connectionManagementService, "connectionDAO", mockConnectionDAO);
    }

    @Test
    public void testGetAllConnections() throws Exception {
        Mockito.when(mockConnectionDAO.allConnectionsOfType(null)).thenReturn(feedConnections);
                String s = connectionManagementService.getAllConnections(null, true);
        Assert.assertNotNull(s);
    }
}
