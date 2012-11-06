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

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.connections.ProjectHostingProvider;
import com.streamreduce.rest.dto.response.ConnectionProviderResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionProvidersResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.resource.ErrorMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.json.JSONObject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProjectHostingResourceITCase extends AbstractInContainerTestCase {

    private ConnectionResponseDTO gitHubConnection = null;
    private ConnectionResponseDTO jiraConnection = null;
    private String authnToken = null;
    private String jiraUrl = null;
    private String jiraUsername = null;
    private String jiraPassword = null;
    private String gitHubUsername = null;
    private String gitHubPassword = null;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        authnToken = login(testUsername, testUsername);
        jiraUrl = jiraProperties.getString("jira.url");
        jiraUsername = jiraProperties.getString("jira.username");
        jiraPassword = jiraProperties.getString("jira.password");
        gitHubUsername = gitHubProperties.getString("github.username");
        gitHubPassword = gitHubProperties.getString("github.password");
    }

    /**
     * Tests the retrieval of supported project hosting providers.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    @Ignore
    public void testProjectHostingProvidersList() throws Exception {
        String projectHostingProvidersUrl = connectionsBaseUrl + "/providers/projecthosting";
        String authnToken = login(testUsername, testUsername);
        String response = makeRequest(projectHostingProvidersUrl, "GET", null, authnToken);
        ConnectionProvidersResponseDTO responseDTO = new ObjectMapper().readValue(response,
                ConnectionProvidersResponseDTO.class);
        List<String> expectedIds = new ArrayList<String>(Arrays.asList("github","jira"));


        for (ConnectionProviderResponseDTO cprd : responseDTO.getProviders()) {
            if (cprd != null) {
                if (!expectedIds.contains(cprd.getId())) {
                    fail("Unexpected project hosting provider: " + cprd.getId());
                }
            }
        }
    }

    /**
     * Test the CRUD functionality for GitHub project hosting connections.
     * 
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore
    public void testGitHubProjectHostingCRUD() throws Exception {
        JSONObject json = new JSONObject();
        JSONObject credentialsObject = new JSONObject();

        credentialsObject.put("identity", "FAKEUSERNAME");
        credentialsObject.put("credential", "FAKEPASSWORD");

        json.put("description", "This GitHub connection is for Nodeable.");
        json.put("alias", "Nodeable GitHub");
        json.put("credentials", credentialsObject);
        json.put("providerId", ProviderIdConstants.GITHUB_PROVIDER_ID);
        json.put("type", ProjectHostingProvider.TYPE);

        // Should fail due to invalid cloud credentials
        assertNotNull(jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ErrorMessage.class)));

        credentialsObject.put("identity", gitHubUsername);
        credentialsObject.put("credential", gitHubPassword);

        json.put("credentials", credentialsObject);

        // Create a valid cloud
        gitHubConnection = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        assertNotNull(gitHubConnection.getId());
        assertEquals(gitHubConnection.getAlias(), json.getString("alias"));
        assertEquals(gitHubConnection.getDescription(), json.getString("description"));
        assertEquals(gitHubConnection.getProviderId(), json.getString("providerId"));
        assertTrue(gitHubConnection.isOwner());

        // Should be a duplicate due to the credentials
        assertNotNull(jsonToObject(makeRequest(connectionsBaseUrl + "/", "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ErrorMessage.class)));

        // Set some different credentials so validation fails for the duplicate alias instead of duplicate credentials
        json.put("credentials", new ConnectionCredentials("FAKEUSERNAME", "FAKEPASSWORD"));

        // Should be a duplicate due to the name
        assertNotNull(jsonToObject(makeRequest(connectionsBaseUrl + "/", "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ErrorMessage.class)));

        // Put back the valid credentials, just in case
        json.put("credentials", credentialsObject);

        gitHubConnection = jsonToObject(makeRequest(connectionsBaseUrl + "/" + gitHubConnection.getId(), "GET", null,
                authnToken), TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        assertNotNull(gitHubConnection.getId());
        assertEquals(gitHubConnection.getAlias(), json.getString("alias"));
        assertEquals(gitHubConnection.getDescription(), json.getString("description"));
        assertEquals(gitHubConnection.getProviderId(), json.getString("providerId"));
        assertTrue(gitHubConnection.isOwner());

        List<ConnectionResponseDTO> connections =
                jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructCollectionType(List.class, ConnectionResponseDTO.class));

        assertEquals(1, connections.size());

        // Update the connection
        json = new JSONObject();

        json.put("alias", "Updated Nodeable GitHub");
        json.put("description", "Updated description.");

        gitHubConnection = jsonToObject(makeRequest(connectionsBaseUrl + "/" + gitHubConnection.getId(), "PUT", json,
                authnToken), TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        gitHubConnection = jsonToObject(makeRequest(connectionsBaseUrl + "/" + gitHubConnection.getId(), "GET", null,
                authnToken), TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        assertEquals(json.getString("alias"), gitHubConnection.getAlias());
        assertEquals(json.getString("description"), gitHubConnection.getDescription());

        makeRequest(connectionsBaseUrl + "/" + gitHubConnection.getId(), "DELETE", null, authnToken);

        connections = jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructCollectionType(List.class, ConnectionResponseDTO.class));
        assertEquals(0, connections.size());

        gitHubConnection = null;
    }

    /**
     * Test the CRUD functionality for Jira project hosting connections.
     * 
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore
    public void testJiraProjectHostingCRUD() throws Exception {
        JSONObject json = new JSONObject();
        JSONObject credentialsObject = new JSONObject();

        credentialsObject.put("identity", "FAKEUSERNAME");
        credentialsObject.put("credential", "FAKEPASSWORD");

        json.put("description", "This GitHub connection is for Nodeable.");
        json.put("alias", "Nodeable GitHub");
        json.put("credentials", credentialsObject);
        json.put("providerId", ProviderIdConstants.JIRA_PROVIDER_ID);
        json.put("url", jiraUrl);
        json.put("type", ProjectHostingProvider.TYPE);

        // Should fail due to invalid cloud credentials
        assertNotNull(jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ErrorMessage.class)));

        credentialsObject.put("identity", jiraUsername);
        credentialsObject.put("credential", jiraPassword);

        json.put("credentials", credentialsObject);

        // Create a valid cloud
        jiraConnection = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        assertNotNull(jiraConnection.getId());
        assertEquals(jiraConnection.getAlias(), json.getString("alias"));
        assertEquals(jiraConnection.getDescription(), json.getString("description"));
        assertEquals(jiraConnection.getProviderId(), json.getString("providerId"));
        assertEquals(jiraConnection.getUrl(), jiraUrl);
        assertTrue(jiraConnection.isOwner());

        // Should be a duplicate due to the credentials
        assertNotNull(jsonToObject(makeRequest(connectionsBaseUrl + "/", "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ErrorMessage.class)));

        // Set some different credentials so validation fails for the duplicate alias instead of duplicate credentials
        json.put("credentials", new ConnectionCredentials("FAKEUSERNAME", "FAKEPASSWORD"));

        // Should be a duplicate due to the name
        assertNotNull(jsonToObject(makeRequest(connectionsBaseUrl + "/", "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ErrorMessage.class)));

        // Put back the valid credentials, just in case
        json.put("credentials", credentialsObject);

        jiraConnection = jsonToObject(makeRequest(connectionsBaseUrl + "/" + jiraConnection.getId(), "GET", null,
                authnToken), TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        assertNotNull(jiraConnection.getId());
        assertEquals(jiraConnection.getAlias(), json.getString("alias"));
        assertEquals(jiraConnection.getDescription(), json.getString("description"));
        assertEquals(jiraConnection.getProviderId(), json.getString("providerId"));
        assertEquals(jiraConnection.getUrl(), jiraUrl);
        assertTrue(jiraConnection.isOwner());

        List<ConnectionResponseDTO> connections =
                jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructCollectionType(List.class, ConnectionResponseDTO.class));

        assertEquals(1, connections.size());

        // Update the connection
        json = new JSONObject();

        json.put("alias", "Updated Nodeable Jira");
        json.put("description", "Updated description.");

        jiraConnection =
                jsonToObject(makeRequest(connectionsBaseUrl + "/" + jiraConnection.getId(), "PUT", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        jiraConnection = jsonToObject(makeRequest(connectionsBaseUrl + "/" + jiraConnection.getId(), "GET", null,
                authnToken), TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

        assertEquals(json.getString("alias"), jiraConnection.getAlias());
        assertEquals(json.getString("description"), jiraConnection.getDescription());

        makeRequest(connectionsBaseUrl + "/" + jiraConnection.getId(), "DELETE", null, authnToken);

        connections = jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructCollectionType(List.class, ConnectionResponseDTO.class));
        assertEquals(0, connections.size());

        jiraConnection = null;
    }

    // TODO: test toggle visibility...

}
