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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import com.streamreduce.util.JiraClient;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ConnectionServiceJiraEndToEndIT extends AbstractServiceTestCase {

    @Autowired
    ConnectionService connectionService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    @Test
    public void testJiraConnectionCRUD() throws Exception {
        Connection tmpConnection = new Connection.Builder()
                .alias("Nodeable Jira")
                .description("Nodeable's Jira instance.")
                .credentials(new ConnectionCredentials(jiraProperties.getString("nodeable.jira.username"),
                        jiraProperties.getString("nodeable.jira.password")))
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.JIRA_PROVIDER_ID))
                .url(jiraProperties.getString("nodeable.jira.url"))
                .authType(AuthType.USERNAME_PASSWORD)
                .user(testUser)
                .build();

        Connection connection = connectionService.createConnection(tmpConnection);

        // Set lastActivity
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put("last_activity_poll", Long.toString(System.currentTimeMillis()));
        connection.setMetadata(metadata);

        assertNotNull(connection.getId());

        /* Verify the projects returned look like what we expect.  (This is fragile and depends on us knowing
           the projects the Jira user we're testing with has access to.
         */
        validateJiraProjectHostingInventoryItems(connection);
        pollForProjectHostingActivity(connection);

        /* Test reading back the connection */
        connection = connectionService.getConnection(connection.getId());
        assertNotNull(connection);

        /* Test reading all Jira connections */
        assertEquals(1, connectionService.getConnections(ProjectHostingProvider.TYPE, testUser).size());

        /* Test updating a connection */
        connection.setCredentials(new ConnectionCredentials(jiraProperties.getString("nodeable.jira.username"),
                jiraProperties.getString("nodeable.jira.password")));
        connection.setDescription("Updated description.");
        connection = connectionService.updateConnection(connection);

        assertEquals("Updated description.", connectionService.getConnection(connection.getId()).getDescription());

        /* Test deleting a connection */
        connectionService.deleteConnection(connection);

        try {
            connectionService.getConnection(connection.getId());
            fail("Should not be able to find a Jira connection with a deleted id.");
        } catch (ConnectionNotFoundException e) {
            // Expected
        }
    }

    @SuppressWarnings("unchecked")
    private void validateJiraProjectHostingInventoryItems(Connection connection) throws Exception {
        // Let the project inventory refresh process finish
        Thread.sleep(60000);

        /* As of 2011-11-04, the Jira projects for the hudson user are as follows:
             OPS (levine)
             PRIBETA (dave)
             SOBADESGN (levine)
             SOBA (mark)
             UX (me)
         */

        final JiraClient jiraClient = new JiraClient(connection);
        final List<JSONObject> rawJiraProjects = jiraClient.getProjects(false);
        final List<JSONObject> rawPublicJiraProjects = jiraClient.getProjects(true);
        final Map<String, JSONObject> rawJiraProjectsMap = new HashMap<String, JSONObject>();

        /* Create a simple set of project keys for all public projects */
        ImmutableSet<String> publicProjectKeys =
                ImmutableSet.copyOf(Iterables.transform(rawPublicJiraProjects,
                        new Function<Object, String>() {
                            /**
                             * {@inheritDoc}
                             */
                            @Override
                            public String apply(Object project) {
                                return ((JSONObject) project).getString("key");
                            }
                        }));

        /* Retrieve the list of projects from Jira and get their details */
        for (JSONObject project : rawJiraProjects) {
            String projectKey = project.getString("key");
            rawJiraProjectsMap.put(projectKey, jiraClient.getProjectDetails(projectKey));
        }

        final List<InventoryItem> cachedJiraProjects = inventoryService.getInventoryItems(connection);

        assertEquals(rawJiraProjects.size(), cachedJiraProjects.size());

        for (InventoryItem inventoryItem : cachedJiraProjects) {
            String key = inventoryItem.getExternalId();
            JSONObject rawProject = rawJiraProjectsMap.get(key);

            assertNotNull("Unable to find a project with a key of: " + key, rawProject);

            String projectDesc = rawProject.has("description") ? rawProject.getString(("description")) : null;

            assertEquals(testAccount.getId(), inventoryItem.getAccount().getId());
            assertEquals(rawProject.getString("name"), inventoryItem.getAlias());

            if (projectDesc != null) {
                assertEquals(projectDesc, inventoryItem.getDescription());
            } else {
                assertNull(inventoryItem.getDescription());
            }

            assertEquals(connection.getId(), inventoryItem.getConnection().getId());
            assertNotNull(inventoryItem.getId());
            assertNotNull(inventoryItem.getMetadataId());
            assertTrue(inventoryItem.getHashtags().contains("#jira"));

            if (publicProjectKeys.contains(key)) {
                assertEquals(SobaObject.Visibility.ACCOUNT, inventoryItem.getVisibility());
            } else {
                assertEquals(SobaObject.Visibility.SELF, inventoryItem.getVisibility());
            }
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
