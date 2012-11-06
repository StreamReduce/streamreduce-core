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

package com.streamreduce.rest;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ProjectHostingProvider;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.rest.dto.response.AuthTypeResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionProviderResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionProvidersResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.sf.json.JSONObject;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConnectionResourceITCase extends AbstractInContainerTestCase {

    private String authnToken = null;
    private ConnectionResponseDTO cloud;
    private ConnectionResponseDTO jira;
    private ConnectionResponseDTO gitHub;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        authnToken = login(testUsername, testUsername);
    }


    @Test
    @Ignore
    public void testAllProviders() throws Exception {
        ConnectionProvidersResponseDTO responseDTO = jsonToObject(makeRequest(connectionsBaseUrl + "/providers",
                "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(
                        ConnectionProvidersResponseDTO.class));
        //AWS, Github, Jira, RSS Feed, Generic Gateway, Pingdom and Twitter
        assertEquals(7, responseDTO.getProviders().size());
    }

    @Test
    @Ignore
    public void testAllProvidersNoShowAuthTypeParam() throws Exception {
        //Test that verifies authType information is left out of ConnectionProviderResponseDTO unless specified
        ConnectionProvidersResponseDTO responseDTO = jsonToObject(makeRequest(connectionsBaseUrl + "/providers",
                "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(
                        ConnectionProvidersResponseDTO.class));
        for (ConnectionProviderResponseDTO provider : responseDTO.getProviders()) {
            assertTrue(provider.getAuthTypes() == null);
        }
    }

    @Test
    @Ignore
    public void testAllProvidersShowAuthTypeParam() throws Exception {
        //Test that verifies authType information is included in ConnectionProviderResponseDTO when showAuthTypes
        //is set to true
        String rawResponse = makeRequest(connectionsBaseUrl + "/providers?showAuthTypes=true", "GET", null, authnToken);
        ConnectionProvidersResponseDTO responseDTO = jsonToObject(rawResponse,
                                                                  TypeFactory.defaultInstance().constructType(
                                                                          ConnectionProvidersResponseDTO.class));
        for (ConnectionProviderResponseDTO provider : responseDTO.getProviders()) {
            assertTrue(provider.getAuthTypes() != null);
            assertTrue(provider.getAuthTypes().getAuthTypes().size() != 0);
        }
    }

    @Test
    @Ignore
    public void testCloudAwsProviderWithAuthTypeParam() throws Exception {
        //Test that a proper response is returned for /api/connections/providers/cloud
        //
        ConnectionProvidersResponseDTO responseDTO = jsonToObject(makeRequest(connectionsBaseUrl + "/providers/cloud?showAuthTypes=true",
                "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(
                        ConnectionProvidersResponseDTO.class));
        assertTrue(responseDTO.getProviders().size() == 1);

        ConnectionProviderResponseDTO awsProvider = responseDTO.getProviders().get(0);
        assertEquals("aws", awsProvider.getId());

        List<AuthTypeResponseDTO> authTypes = awsProvider.getAuthTypes().getAuthTypes();
        assertEquals(1, authTypes.size());
        assertEquals(AuthType.USERNAME_PASSWORD.toString(), authTypes.get(0).getType());
    }

    @Test
    @Ignore
    public void testFeedRssProviderWithAuthTypeParam() throws Exception {
        //Test that a proper response is returned for /api/connections/providers/cloud
        ConnectionProvidersResponseDTO responseDTO = jsonToObject(makeRequest(connectionsBaseUrl + "/providers/feed?showAuthTypes=true",
                "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(
                        ConnectionProvidersResponseDTO.class));
        assertTrue(responseDTO.getProviders().size() == 1);

        ConnectionProviderResponseDTO feedProviders = responseDTO.getProviders().get(0);
        assertEquals("rss", feedProviders.getId());

        List<AuthTypeResponseDTO> authTypes = feedProviders.getAuthTypes().getAuthTypes();

        //These will fail with NoSuchElementExceptions if the criteria aren't met
        Iterables.find(authTypes, new Predicate<AuthTypeResponseDTO>() {
            @Override
            public boolean apply(@Nullable AuthTypeResponseDTO input) {
                return input != null && input.getType().equals(AuthType.NONE.toString())
                        && input.getUsernameLabel() == null
                        && input.getPasswordLabel() == null;
            }
        });
        Iterables.find(authTypes, new Predicate<AuthTypeResponseDTO>() {
            @Override
            public boolean apply(@Nullable AuthTypeResponseDTO input) {
                return input != null && input.getType().equals(AuthType.USERNAME_PASSWORD.toString())
                        && input.getUsernameLabel().equalsIgnoreCase("UserName")
                        && input.getPasswordLabel().equalsIgnoreCase("Password");
            }
        });
    }

    @Test
    @Ignore
    public void testGithubProviderWithAuthTypeParam() throws Exception {
        //Test that a proper response is returned for /api/connections/providers/projecthosting for the the github id
        ConnectionProvidersResponseDTO responseDTO = jsonToObject(makeRequest(connectionsBaseUrl + "/providers/projecthosting?showAuthTypes=true",
                "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(
                        ConnectionProvidersResponseDTO.class));
        assertTrue(responseDTO.getProviders().size() == 2);

        ConnectionProviderResponseDTO githubProviderDTO = Iterables.find(responseDTO.getProviders(), new Predicate<ConnectionProviderResponseDTO>() {
            @Override
            public boolean apply(@Nullable ConnectionProviderResponseDTO input) {
                return input != null && input.getId().equals("github");
            }
        });

        List<AuthTypeResponseDTO> authTypes = githubProviderDTO.getAuthTypes().getAuthTypes();

        //These will fail with NoSuchElementExceptions if the criteria aren't met
        Iterables.find(authTypes, new Predicate<AuthTypeResponseDTO>() {
            @Override
            public boolean apply(@Nullable AuthTypeResponseDTO input) {
                return input != null && input.getType().equals(AuthType.USERNAME_PASSWORD.toString())
                        && input.getUsernameLabel().equalsIgnoreCase("UserName")
                        && input.getPasswordLabel().equalsIgnoreCase("Password");
            }
        });
        Iterables.find(authTypes, new Predicate<AuthTypeResponseDTO>() {
            @Override
            public boolean apply(@Nullable AuthTypeResponseDTO input) {
                return input != null && input.getType().equals(AuthType.OAUTH.toString())
                        && input.getUsernameLabel() == null
                        && input.getPasswordLabel() == null
                        && input.getOauthEndpoint().equals("/api/oauth/providers/github")
                        && input.getCommandLabel() != null;
            }
        });
    }

    @Test
    @Ignore
    public void testAllConnections() throws Exception {
        String req = makeRequest(connectionsBaseUrl, "GET", null, authnToken);

        List<ConnectionResponseDTO> allConnections = jsonToObject(req,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ConnectionResponseDTO.class));

        assertEquals(25, allConnections.size()); // The 21 RSS feed connections, 1 AWS, 1 GitHub and 1 Jira

        cloud = createConnection(authnToken, "AWS Cloud Connection", "This is a test cloud connection to AWS.",
                new ConnectionCredentials(cloudProperties.getString("nodeable.aws.accessKeyId"),
                        cloudProperties.getString("nodeable.aws.secretKey")),
                ProviderIdConstants.AWS_PROVIDER_ID, null, ConnectionTypeConstants.CLOUD_TYPE, AuthType.USERNAME_PASSWORD);

        allConnections = jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructCollectionType(List.class, ConnectionResponseDTO.class));

        assertEquals(26, allConnections.size());

        jira = createConnection(authnToken, "Jira Connection", "This is a test project hosting connection to Jira.",
                new ConnectionCredentials(jiraProperties.getString("nodeable.jira.username"),
                        jiraProperties.getString("nodeable.jira.password")),
                ProviderIdConstants.JIRA_PROVIDER_ID, jiraProperties.getString("nodeable.jira.url"),
                ConnectionTypeConstants.PROJECT_HOSTING_TYPE, AuthType.USERNAME_PASSWORD);
        allConnections = jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance()
                        .constructCollectionType(List.class, ConnectionResponseDTO.class));

        assertEquals(27, allConnections.size());

        gitHub = createConnection(authnToken, "GitHub Connection",
                "This is a test project hosting connection to GitHub.",
                new ConnectionCredentials(gitHubProperties.getString("nodeable.github.username"),
                        gitHubProperties.getString("nodeable.github.password")),
                ProviderIdConstants.GITHUB_PROVIDER_ID, null,
                ConnectionTypeConstants.PROJECT_HOSTING_TYPE, AuthType.USERNAME_PASSWORD);
        allConnections = jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance()
                        .constructCollectionType(List.class, ConnectionResponseDTO.class));

        assertEquals(28, allConnections.size());
    }

    /**
     * SOBA-1034
     *
     * @throws Exception if anything goes wrong.
     */
    @Test
    @Ignore
    public void testUpdatingCredentials() throws Exception {
        gitHub = createConnection(authnToken, "GitHub Connection",
                "This is a test project hosting connection to GitHub.",
                new ConnectionCredentials(gitHubProperties.getString("nodeable.github.username"),
                        gitHubProperties.getString("nodeable.github.password")),
                ProviderIdConstants.GITHUB_PROVIDER_ID, null,
                ConnectionTypeConstants.PROJECT_HOSTING_TYPE, AuthType.USERNAME_PASSWORD);

        // To test that we can successfully change connection credentials via REST, we will change the credentials
        // to known bad values and expect to return an exception response that says the credentials were invalid.
        JSONObject json = new JSONObject();
        JSONObject credentialsJSON = new JSONObject();

        credentialsJSON.put("credential", "fake");
        credentialsJSON.put("identity", "fake");

        json.put("credentials", credentialsJSON);

        try {
            cloud = jsonToObject(makeRequest(connectionsBaseUrl + "/" + gitHub.getId(), "PUT", json, authnToken),
                    TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("credentials") && e.getMessage().contains("are invalid"));
        }
    }

    /**
     * SOBA-1080
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore
    public void testCannotUpdateVisibilityToPublic() throws Exception {
        gitHub = createConnection(authnToken, "GitHub Connection",
                "This is a test project hosting connection to GitHub.",
                new ConnectionCredentials(gitHubProperties.getString("nodeable.github.username"),
                        gitHubProperties.getString("nodeable.github.password")),
                ProviderIdConstants.GITHUB_PROVIDER_ID, null,
                ConnectionTypeConstants.PROJECT_HOSTING_TYPE, AuthType.USERNAME_PASSWORD);

        JSONObject json = new JSONObject();

        json.put("visibility", "PUBLIC");

        ConstraintViolationExceptionResponseDTO response =
                jsonToObject(makeRequest(connectionsBaseUrl + "/" + gitHub.getId(), "PUT", json, authnToken),
                        TypeFactory.defaultInstance().constructType(ConstraintViolationExceptionResponseDTO.class));
        Map<String, String> violations = response.getViolations();
        boolean violationFound = false;

        for (String violationKey : violations.keySet()) {
            if (violationKey.equals("visibility")) {
                violationFound = true;

                break;
            }
        }

        assertTrue(violationFound);

        // Just to be sure, let's change the visibility to something valid
        json.put("visibility", SobaObject.Visibility.SELF);

        gitHub = jsonToObject(makeRequest(connectionsBaseUrl + "/" + gitHub.getId(), "PUT", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        assertEquals(SobaObject.Visibility.SELF, gitHub.getVisibility());
    }

    /**
     * Verify that tagging a connection tags the inventory items too
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testHashtagOnConnectionAndChildren() throws Exception {

        // random tag to connection...
        String random = UUID.randomUUID().toString().substring(0, 4);

        User user = applicationManager.getUserService().getUser(testUser.getUsername());
        String authnToken = login(user.getUsername(), testUser.getUsername()); // ugh

        // just get the first random one
        List<Connection> connections = applicationManager.getConnectionService().getConnections(ProjectHostingProvider.TYPE, user);
        Connection connection = connections.get(0);

        JSONObject json = new JSONObject();
        // Just to be sure, let's change the visibility to something valid
        json.put("hashtag", random);

        makeRequest(connectionsBaseUrl + "/" + connection.getId() + "/hashtag", "POST", json, authnToken);

        /// verify the connection is tagged
        connection = applicationManager.getConnectionService().getConnection(connection.getId());

        assertNotNull(connection.getHashtags());
        assertTrue(connection.getHashtags().size() > 0);
        assertTrue(connection.getHashtags().contains("#" + random));

        // now check the child items
        List<InventoryItem> items = applicationManager.getInventoryService().getInventoryItems(connection, user);
        assertNotNull(items);
        assertTrue(items.size() > 0);

        for (InventoryItem item : items) {
            assertTrue(item.getHashtags().contains("#" + random));
        }

        // now delete
        makeRequest(connectionsBaseUrl + "/" + connection.getId() + "/hashtag/" + random, "DELETE", null, authnToken);

        // get a fresh copy from the db
        connection = applicationManager.getConnectionService().getConnection(connection.getId());

        assertFalse(connection.getHashtags().contains("#" + random));

        // now check the child items
        items = applicationManager.getInventoryService().getInventoryItems(connection, user);
        for (InventoryItem item : items) {
            assertFalse(item.getHashtags().contains("#" + random));
        }
    }

    @Test
    @Ignore
    public void testCanBlacklistPublicConnection() throws Exception {

        List<Connection> connectionList = applicationManager.getConnectionService().getPublicConnections("feed");
        // get an RSS connection bootstrapped by nodeable
        Connection connection = connectionList.get(0);
        assertNotNull(connection);

        // try and delete the connection.
        String response = makeRequest(connectionsBaseUrl + "/" + connection.getId(), "DELETE", null, authnToken);

        Assert.assertEquals("200", response);

        // make sure it's still exists
        connection = applicationManager.getConnectionService().getConnection(connection.getId());
        assertNotNull(connection);

        // make sure you account object has it in the blacklisted list
        //boolean exists =
        Account account = applicationManager.getUserService().getAccount(testUser.getAccount().getId());
        assertFalse(!account.getPublicConnectionBlacklist().contains(connection.getId()));

        // should not show up in your connection list now.
        String req = makeRequest(connectionsBaseUrl, "GET", null, authnToken);

        List<ConnectionResponseDTO> allConnections = jsonToObject(req,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ConnectionResponseDTO.class));

        for(ConnectionResponseDTO connectionResponseDTO : allConnections) {
            assertFalse(connectionResponseDTO.getId().equals(connection.getId()));
        }

    }


}
