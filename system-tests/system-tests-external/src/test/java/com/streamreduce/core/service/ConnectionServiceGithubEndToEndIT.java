package com.streamreduce.core.service;


import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.connections.ProjectHostingProvider;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.util.GitHubClient;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ConnectionServiceGithubEndToEndIT extends AbstractServiceTestCase {

    @Autowired
    ConnectionService connectionService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    @Test
    public void testGitHubConnectionCRUD() throws Exception {
        Connection tmpConnection = new Connection.Builder()
                .alias("Nodeable GitHub")
                .description("Nodeable's GitHub instance.")
                .credentials(new ConnectionCredentials("fakeusername", "fakepassword"))
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.GITHUB_PROVIDER_ID))
                .user(testUser)
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        /* Test creating an invalid connection (invalid credentials) */
        try {
            connectionService.createConnection(tmpConnection);
            fail("Should not be able to create a Jira connection with invalid credentials.");
        } catch (InvalidCredentialsException e) {
            // Expected
        }

        tmpConnection.setCredentials(new ConnectionCredentials(gitHubProperties.getString("nodeable.github.username"),
                gitHubProperties.getString("nodeable.github.password")));

        /* Test creating a valid connection */
        Connection connection = connectionService.createConnection(tmpConnection);

        assertNotNull(connection.getId());

        /* Verify the projects returned look like what we expect.  (This is fragile and depends on us knowing
           the projects the GitHub user we're testing with watches, owns and has access to as part of an org.
         */
        validateGitHubProjectHostingInventoryItems(connection);
        pollForProjectHostingActivity(connection);

        /* Test reading a connection (invalid id) */
        try {
            connectionService.getConnection(new ObjectId());
            fail("Should not be able to find a GitHub connection with an invalid id.");
        } catch (ConnectionNotFoundException e) {
            // Expected
        }

        /* Test reading a connection */
        connection = connectionService.getConnection(connection.getId());

        assertNotNull(connection);

        /* Test reading all GitHub connections */
        assertEquals(1, connectionService.getConnections(ProjectHostingProvider.TYPE, testUser).size());

        /* Test updating a connection (invalid credentials) */
        connection.setCredentials(new ConnectionCredentials("fakeusername", "fakepassword"));

        try {
            connectionService.createConnection(connection);
            fail("Should not be able to create a GitHub connection with invalid credentials.");
        } catch (InvalidCredentialsException e) {
            // Expected
        }

        /* Test updating a connection */
        connection.setCredentials(new ConnectionCredentials(gitHubProperties.getString("nodeable.github.username"),
                gitHubProperties.getString("nodeable.github.password")));
        connection.setDescription("Updated description.");

        connection = connectionService.updateConnection(connection);

        assertEquals("Updated description.", connectionService.getConnection(connection.getId()).getDescription());

        /* Test deleting a connection */
        connectionService.deleteConnection(connection);

        try {
            connectionService.getConnection(connection.getId());
            fail("Should not be able to find a GitHub connection with a deleted id.");
        } catch (ConnectionNotFoundException e) {
            // Expected
        }
    }

    private void validateGitHubProjectHostingInventoryItems(Connection connection) throws Exception {
        // Let the project inventory refresh process finish
        Thread.sleep(60000);

        final GitHubClient gitHubClient = (GitHubClient)
                connectionProviderFactory.externalIntegrationConnectionProviderFromId(
                        connection.getProviderId()).getClient(connection);

        final List<JSONObject> rawGitHubProjects = gitHubClient.getRepositories();
        final Map<String, JSONObject> rawGitHubProjectsMap = new HashMap<String, JSONObject>();

        /* Create a map for easier project retrieval */
        for (JSONObject project : rawGitHubProjects) {
            String key = project.getJSONObject("owner").getString("login") + "/" + project.getString("name");
            rawGitHubProjectsMap.put(key, project);
        }

        final List<InventoryItem> cachedGitHubProjects = inventoryService.getInventoryItems(connection);

        assertEquals(rawGitHubProjectsMap.size(), cachedGitHubProjects.size());

        for (InventoryItem inventoryItem : cachedGitHubProjects) {
            String key = inventoryItem.getExternalId();
            JSONObject rawProject = rawGitHubProjectsMap.get(key);

            assertNotNull("Unable to find a project with a key of: " + key, rawProject);
            assertEquals(testAccount.getId(), inventoryItem.getAccount().getId());
            assertEquals(key, inventoryItem.getAlias());
            // since the rawProject description may be longer than the inventory item due
            //  a size cap on InventoryItem#description, we'll just make sure it .startsWith()
            assertTrue(rawProject.getString("description").startsWith(inventoryItem.getDescription()));
            assertEquals(connection.getId(), inventoryItem.getConnection().getId());
            assertNotNull(inventoryItem.getId());
            assertNotNull(inventoryItem.getMetadataId());

            if (rawProject.getBoolean("private")) {
                assertEquals(SobaObject.Visibility.SELF, inventoryItem.getVisibility());
            } else {
                assertEquals(SobaObject.Visibility.ACCOUNT, inventoryItem.getVisibility());
            }

            assertTrue(inventoryItem.getHashtags().contains("#github"));
        }
    }

    private void pollForProjectHostingActivity(Connection connection) throws Exception {
        // Attempt to get the latest activity by updating the connection's last poll date to 12 hours ago
        // and then retrieve connection activity.
        connection.getMetadata().put("last_activity_poll",
                Long.toString(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12)));

        connectionService.fireOneTimeHighPriorityJobForConnection(connection);
        Thread.sleep(TimeUnit.SECONDS.toMillis(20));
    }
}
