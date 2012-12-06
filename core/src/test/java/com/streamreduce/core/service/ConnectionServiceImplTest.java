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

package com.streamreduce.core.service;

import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.dao.ConnectionDAO;
import com.streamreduce.core.model.*;
import com.streamreduce.core.service.exception.ConnectionExistsException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConnectionServiceImplTest {

    private static final String SAMPLE_EXTERNAL_ID = "ABCD-EF-123456789";
    private static final String OTHER_EXTERNAL_ID = "134234234235234";

    User sampleUser;
    ConnectionServiceImpl connectionServiceImpl = new ConnectionServiceImpl();
    ConnectionDAO connectionDAO;

     
    
    @Before
    @SuppressWarnings("unused")
    public void setUp() {
    sampleUser = new User.Builder().account(new Account.Builder().name("ABC").build()).username("sampleUser").build();
        List<Connection> feedConnections = Arrays.asList(
                new Connection.Builder()
                        .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                        .url("http://foo.url1.com/rss")
                        .credentials(new ConnectionCredentials("ident", "pass"))
                        .alias("connection1")
                        .user(sampleUser)
                        .authType(AuthType.NONE)
                        .build(),
                new Connection.Builder()
                        .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                        .url("http://foo.url2.com/nocreds")
                        .alias("connection2_nocreds")
                        .user(sampleUser)
                        .authType(AuthType.NONE)
                        .build(),
                new Connection.Builder()
                        .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                        .url("http://feeds.venturebeat.com/Venturebeat?format=xml")
                        .authType(AuthType.NONE)
                        .alias("VentureBeat")
                        .user(sampleUser)
                        .visibility(SobaObject.Visibility.ACCOUNT)
                        .hashtag("rss")
                        .build()
        );

        List<Connection> cloudConnections = Arrays.asList(
                new Connection.Builder()
                        .credentials(new ConnectionCredentials("AWSACCESSKEY", "foobar"))
                        .alias("testAWS")
                        .user(sampleUser)
                        .provider(ConnectionProvidersForTests.AWS_CLOUD_PROVIDER)
                        .authType(AuthType.USERNAME_PASSWORD)
                        .build()
        );

        List<Connection> projHostingConnections = Arrays.asList(
                new Connection.Builder()
                        .credentials(new ConnectionCredentials("user", "password"))
                        .alias("Github U/P")
                        .user(sampleUser)
                        .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                        .authType(AuthType.USERNAME_PASSWORD)
                        .build(),
                new Connection.Builder()
                        .credentials(new ConnectionCredentials("temp_code", "secret_code"))
                        .alias("Github OAuth")
                        .user(sampleUser)
                        .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                        .authType(AuthType.OAUTH)
                        .build(),
                new Connection.Builder()
                        .credentials(new ConnectionCredentials("temp_code", "secret_code"))
                        .alias("Github OAuth Another Account User")
                        .user(new User.Builder().username("Jimi").account(sampleUser.getAccount()).build())
                        .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                        .authType(AuthType.OAUTH)
                        .build()
        );



        //Stub out ConnectionDAO in ConnectionServiceImpl
        connectionDAO = mock(ConnectionDAO.class);
        when(connectionDAO.forTypeAndUser(ConnectionTypeConstants.FEED_TYPE, sampleUser)).thenReturn(feedConnections);
        when(connectionDAO.forTypeAndUser(ConnectionTypeConstants.CLOUD_TYPE, sampleUser)).thenReturn(cloudConnections);
        when(connectionDAO.forTypeAndUser(eq(ConnectionTypeConstants.PROJECT_HOSTING_TYPE), any(User.class))).thenReturn(projHostingConnections);

        when(connectionDAO.getByExternalId(SAMPLE_EXTERNAL_ID)).thenReturn(cloudConnections);
        when(connectionDAO.getByExternalId(OTHER_EXTERNAL_ID)).thenReturn(new ArrayList<Connection>());

        ReflectionTestUtils.setField(connectionServiceImpl, "connectionDAO", connectionDAO);
    }

    @Test
    public void testCheckForDuplicateUniqueAliasWithinAccount() throws Exception {
        //Tests for a ConnectionExistsException when checkForDuplicate is called with a Connection with the same
        //alias as a previously existing connection
        Connection conn = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .alias("connection1")
                .authType(AuthType.NONE)
                .user(sampleUser)
                .url("http://someurl").build();
        try {
            connectionServiceImpl.checkForDuplicate(conn);
            Assert.fail("should fail with ConnectionExistsException");
        } catch (ConnectionExistsException e) {
            Assert.assertTrue(e.getMessage().contains("provider already exists with the connection name"));
        } catch (Exception e) {
            Assert.fail("should fail with ConnectionExistsException");
        }


    }
    
    @Test
    public void testCheckForDuplicateSameUrlWithWhitespace() throws Exception {
        Connection c = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .url(" http://feeds.venturebeat.com/Venturebeat?format=xml ")
                .authType(AuthType.NONE)
                .alias("AnotherVentureBeat")
                .user(sampleUser)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .hashtag("rss")
                .build();

        try {
            connectionServiceImpl.checkForDuplicate(c);
            Assert.fail("should fail with ConnectionExistsException");
        } catch (ConnectionExistsException e) {
            Assert.assertTrue(e.getMessage().endsWith("provider already exists using the same credentials, URL, or both."));
        } catch (Exception e) {
            Assert.fail("should fail with ConnectionExistsException");
        }
    }

    @Test
    public void testCheckForDuplicateSameUrlWithDifferentCase() throws Exception {
        Connection c = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .url("http://feeds.venturebeat.com/VENTUREBEAT?format=xml")
                .authType(AuthType.NONE)
                .alias("AnotherVentureBeat")
                .user(sampleUser)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .hashtag("rss")
                .build();

        try {
            connectionServiceImpl.checkForDuplicate(c);
            Assert.fail("should fail with ConnectionExistsException");
        } catch (ConnectionExistsException e) {
            Assert.assertTrue(e.getMessage().endsWith("provider already exists using the same credentials, URL, or both."));
        } catch (Exception e) {
            Assert.fail("should fail with ConnectionExistsException");
        }
    }

    @Test
    public void testCheckForDuplicateNoCredentialsSupplied() throws Exception {
        //Test that ConnectionExistsException is thrown when checkForDuplicate is called with a Connection that
        //does not have credentials set.  If credentials aren't set on the passed in Connection, then duplicates
        //are detected solely by URL (after verifying that the alias is unique in the account).

        Connection conn = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .alias("somethingelse")
                .url("http://foo.url2.com/nocreds")
                .authType(AuthType.NONE)
                .user(sampleUser).build();

        try {
            connectionServiceImpl.checkForDuplicate(conn);
            Assert.fail("should fail with ConnectionExistsException");
        } catch (ConnectionExistsException e) {
            Assert.assertTrue(e.getMessage().endsWith("provider already exists using the same credentials, URL, or both."));
        } catch (Exception e) {
            Assert.fail("should fail with ConnectionExistsException");
        }
    }


    @Test()
    public void testCheckForDuplicateSameCredentialsAndUrl() throws Exception {
        //Tests that a ConnectionExistsException is thrown when both the url and set credentials in a Connection passed
        //to checkForDuplicate match the url and credentials on an existing Connection
        Connection conn = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .alias("somethingelse")
                .url("http://foo.url1.com/rss")
                .credentials(new ConnectionCredentials("ident", "pass"))
                .authType(AuthType.NONE)
                .user(sampleUser)
                .build();

        try {
            connectionServiceImpl.checkForDuplicate(conn);
            Assert.fail("should fail with ConnectionExistsException");
        } catch (ConnectionExistsException e) {
            Assert.assertTrue(e.getMessage().endsWith("provider already exists using the same credentials, URL, or both."));
        } catch (Exception e) {
            Assert.fail("should fail with ConnectionExistsException");
        }
    }

    @Test
    public void testCheckForDuplicateSameCredentialsDifferentUrl() throws Exception {
        //Tests that checkForDuplicate doesn't throw an exception if a connection is passed that has matching
        //credentials with an existing connection but mismatched urls.
        Connection conn = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .alias("somethingelse")
                .url("http://foo.url1.com/anotherUrl")
                .credentials(new ConnectionCredentials("ident", "pass"))
                .authType(AuthType.NONE)
                .user(sampleUser)
                .build();

        connectionServiceImpl.checkForDuplicate(conn); //should pass, not duplicate
    }

    @Test
    public void testCheckForDuplicateDifferentCredentialsSameUrl() throws Exception {
        //Tests that checkForDuplicate doesn't throw an exception if a connection is passed that has matching
        //urls but different credentials
        Connection conn = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .alias("somethingelse")
                .url("http://foo.url2.com/rss")
                .credentials(new ConnectionCredentials("anotherIdent", "anotherPass"))
                .authType(AuthType.NONE)
                .user(sampleUser)
                .build();
        connectionServiceImpl.checkForDuplicate(conn); //should pass, not duplicate
    }

    @Test
    public void testCheckForDuplicateWithNullUrl() throws Exception {
        //Checks for duplicates in the case of a null URL and only credentials can be used e.g. AWS Connections

        Connection conn = new Connection.Builder()
                .credentials(new ConnectionCredentials("AWSACCESSKEY", "foobar"))
                .alias("testAWS1")
                .user(sampleUser)
                .authType(AuthType.USERNAME_PASSWORD)
                .provider(ConnectionProvidersForTests.AWS_CLOUD_PROVIDER).build();
        try {
            connectionServiceImpl.checkForDuplicate(conn);
            Assert.fail("should fail with ConnectionExistsException");
        } catch (ConnectionExistsException e) {
            Assert.assertTrue(e.getMessage().endsWith("provider already exists using the same credentials, URL, or both."));
        } catch (Exception e) {
            Assert.fail("should fail with ConnectionExistsException");
        }
    }

    @Test
    public void testCheckForDuplicateOAuthConnection() throws Exception {
        ///Tests that duplication checking does not reject a brand new connection for the account that uses OAuth
        
        Connection conn = new Connection.Builder()
                .credentials(new ConnectionCredentials("access_code","secret_key"))
                .alias("Jira With MakeBelieve OAuth")
                .user(sampleUser)
                .authType(AuthType.OAUTH)
                .provider(ConnectionProvidersForTests.JIRA_PROVIDER).build();
        
        connectionServiceImpl.checkForDuplicate(conn);
    }

    @Test
    public void testCheckForDuplicateOAuthConnectionDifferentUser() throws Exception {
        ///Tests that duplication checking does not reject an Oauth connection for a different user

        Connection conn = new Connection.Builder()
                .credentials(new ConnectionCredentials("access_code","secret_key"))
                .alias("Github Different User")
                .user(new User.Builder().username("Ozzy").account(sampleUser.getAccount()).build())
                .authType(AuthType.OAUTH)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER).build();

        connectionServiceImpl.checkForDuplicate(conn);
    }

    @Test
    public void testGetConnectionByExternalId() throws ConnectionNotFoundException {
        List<Connection> connectionsByExternalId = connectionServiceImpl.getConnectionsByExternalId(SAMPLE_EXTERNAL_ID,sampleUser);
        assertEquals(1,connectionsByExternalId.size());
    }

    @Test
    public void testGetConnectionByExternalIdNoMatches() throws ConnectionNotFoundException {
        List<Connection> connectionsByExternalId = connectionServiceImpl.getConnectionsByExternalId(OTHER_EXTERNAL_ID,sampleUser);
        assertEquals(0,connectionsByExternalId.size());
    }
}
