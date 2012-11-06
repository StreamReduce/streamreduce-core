package com.streamreduce.util;

import com.google.common.collect.ImmutableSet;
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.connections.JiraProjectHostingProvider;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.User;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import net.sf.json.JSONObject;
import org.apache.abdera.model.Entry;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.namespace.QName;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Tests for {@link JiraClient}.
 */
public class JiraClientIT extends AbstractServiceTestCase {

    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    JiraProjectHostingProvider jiraProjectHostingProvider;

    private ResourceBundle jiraProperties = ResourceBundle.getBundle("jira");
    private String jiraPassword = jiraProperties.getString("nodeable.jira.password");
    private String jiraUrl = jiraProperties.getString("nodeable.jira.url");
    private String jiraUsername = jiraProperties.getString("nodeable.jira.username");
    private Set<String> monitoredProjects = new HashSet<String>();
    private Connection connection;
    private InventoryItem inventoryItem;
    private JiraClient jiraClient;

    @Before
    public void setUp() throws Exception {
        jiraProjectHostingProvider = (JiraProjectHostingProvider) connectionProviderFactory.connectionProviderFromId(
                ProviderIdConstants.JIRA_PROVIDER_ID);

        Account testAccount = new Account.Builder()
                .url("http://nodeable.com")
                .description("Nodeable Test Account")
                .name("Nodeable Testing")
                .build();
        User testUser = new User.Builder()
                .account(testAccount)
                .accountLocked(false)
                .accountOriginator(true)
                .fullname("Nodeable Test User")
                .username("test_user_" + new Date().getTime() + "@nodeable.com")
                .build();

        connection = new Connection.Builder()
                .provider(jiraProjectHostingProvider)
                .credentials(new ConnectionCredentials(jiraUsername, jiraPassword))
                .url(jiraUrl)
                .alias("Test Jira Connection")
                .user(testUser)
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        // Connection properties normally set by persistence/polling
        connection.setId(new ObjectId());
        connection.setLastActivityPollDate(new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24))); // Create a date a day in the past

        inventoryItem = new InventoryItem.Builder()
                .account(testAccount)
                .user(testUser)
                .connection(connection)
                .hashtags(ImmutableSet.of("#jira"))
                .build();

        jiraClient = new JiraClient(connection);

        List<JSONObject> allProjects = jiraClient.getProjects(false);

        for (JSONObject project : allProjects) {
            monitoredProjects.add(project.getString("key"));
        }
    }

    @Test
    public void testJiraGetIssuePriorities() throws Exception {
        Assert.assertTrue(jiraClient.getIssuePriorities().size() > 0);
    }

    @Test
    public void testJiraGetIssueStatuses() throws Exception {
        Assert.assertTrue(jiraClient.getIssueStatuses().size() > 0);
    }

    @Test
    public void testJiraGetIssueTypes() throws Exception {
        Assert.assertTrue(jiraClient.getIssueTypes().size() > 0);
    }

    @Test
    public void testJiraGetSubTaskIssueTypes() throws Exception {
        // We can only check that the response doesn't error here
        Assert.assertTrue(jiraClient.getSubTaskIssueTypes().size() >= 0);
    }

    /**
     * Tests {@link JiraClient#getActivity(java.util.Set)}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testJiraGetActivityParts() throws Exception {
        int maxResults = 100;
        List<Entry> testProjectActivity = jiraClient.getActivity(monitoredProjects, maxResults);

        Assert.assertTrue(testProjectActivity.size() > 0 && testProjectActivity.size() <= maxResults);

        for (Entry activity : testProjectActivity) {
            org.apache.abdera.model.Element activityObjectElement = activity.getFirstChild(
                    new QName("http://activitystrea.ms/spec/1.0/", "object", "activity"));
            String projectKey = jiraClient.getProjectKeyOfEntry(activityObjectElement, monitoredProjects);

            // Prepare the inventory item
            inventoryItem.setExternalId(projectKey);

            Map<String, Object> activityParts = jiraClient.getPartsForActivity(inventoryItem, activity);

            Assert.assertNotNull(activityParts);

            String title = (String)activityParts.get("title");
            String content = (String)activityParts.get("content");
            Set<String> hashtags = (Set<String>)activityParts.get("hashtags");
            int expectedHashtagCount = 2; // #jira and #[project.key] is always expected

            try {
                Assert.assertNotNull(title);
                Assert.assertTrue(hashtags.contains("#" + projectKey.toLowerCase()));
                Assert.assertTrue(hashtags.contains("#jira"));

                // No good way to test content since it can be null a few different ways

                if (hashtags.contains("#issue")) {
                    // Issue related tests
                    expectedHashtagCount += 4; // #issue #[issue-type] #[issue-priority] #[issue-status]

                    // There is no good way to test the actual issue hashtags because they could outdated
                } else if (hashtags.contains("#source")) {
                    // Source related tests
                    expectedHashtagCount += 2; // #source #[activity]

                    Assert.assertFalse(hashtags.contains("#file")); // This is an ignored hashtag
                    Assert.assertTrue(hashtags.contains("#changeset") || hashtags.contains("#review"));
                } else if (hashtags.contains("#wiki")) {
                    // Wiki related tests
                    if (!hashtags.contains("#blog") && !hashtags.contains("#page")) {
                        expectedHashtagCount += 1; // #wiki
                    } else {
                        expectedHashtagCount += 2; // #wiki #[target]
                    }

                    Assert.assertFalse(hashtags.contains("#space")); // This is an ignored hashtag
                    Assert.assertFalse(hashtags.contains("#article")); // This is an ignored hashtag
                    Assert.assertFalse(hashtags.contains("#file")); // This is an ignored hashtag
                } else {
                    Assert.fail("All Jira activity hashtags should contain at least one of he following: " +
                            "#issue, #source or #wiki");
                }

                // For comments and attachments, expected hashtags is one extra
                if (hashtags.contains("#comment") || hashtags.contains("#attachment")) {
                    expectedHashtagCount += 1;
                }

                if (hashtags.contains("#issue") || (hashtags.contains("#wiki") && !hashtags.contains("#blog"))) {
                    Assert.assertTrue(hashtags.size() >= expectedHashtagCount);
                } else {
                    Assert.assertEquals(expectedHashtagCount, hashtags.size());
                }
            } catch (AssertionFailedError e) {
                // Add some extra output to make debugging easier
                System.out.println("Problematic title: " + activity.getTitle());
                System.out.println("Hashtags (Expected: " + expectedHashtagCount + "):");
                for (String hashtag : hashtags) {
                    System.out.println("  " + hashtag);
                }
                throw e;
            }
        }
    }

}
