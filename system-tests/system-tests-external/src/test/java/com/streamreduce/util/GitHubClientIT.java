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

package com.streamreduce.util;

import com.google.common.collect.ImmutableSet;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.User;
import junit.framework.Assert;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Tests for {@link GitHubClient}.
 */
public class GitHubClientIT {

    private ResourceBundle gitHubProperties = ResourceBundle.getBundle("github");
    private String githubPassword = gitHubProperties.getString("nodeable.github.password");
    private String githubUsername = gitHubProperties.getString("nodeable.github.username");
    private Set<String> monitoredProjects = new HashSet<>();
    private Connection connection;
    private InventoryItem inventoryItem;
    private GitHubClient gitHubClient;

    @Before
    public void setUp() throws Exception {
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
                .credentials(new ConnectionCredentials(githubUsername, githubPassword))
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .alias("Test GitHub Connection")
                .authType(AuthType.USERNAME_PASSWORD)
                .user(testUser)
                .build();

        //Properties normally set by persistence/polling
        connection.setId(new ObjectId());
        connection.setLastActivityPollDate(new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24))); // Create a date a day in the past

        inventoryItem = new InventoryItem.Builder()
                .account(testAccount)
                .user(testUser)
                .connection(connection)
                .hashtags(ImmutableSet.of("#github"))
                .build();


        gitHubClient = new GitHubClient(connection,ConnectionProvidersForTests.GITHUB_PROVIDER.getOAuthService());

        List<JSONObject> allRepositories = gitHubClient.getRepositories();

        for (JSONObject repo : allRepositories) {
            monitoredProjects.add(repo.getJSONObject("owner").getString("login") + "/" + repo.getString("name"));
        }
    }

    /**
     * This tests that the parsing of GitHub activities generates the expected title and content strings
     * for the project hosting inventory item activity messages.
     * 
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetGitHubActivityParts() throws Exception {
        String validCharsClassRegex = "[a-zA-Z0-9_.-]";
        int maxResults = 100;
        List<JSONObject> activities = gitHubClient.getActivity(monitoredProjects, maxResults);

        Assert.assertTrue("activities size was " + activities.size(),
                activities.size() > 0 && activities.size() <= maxResults);

        for (JSONObject activity : activities) {
            String projectKey = activity.getJSONObject("repo").getString("name");
            // Prepare the inventory item
            inventoryItem.getExternalId();

            String activityType = activity.getString("type");

            // Only run supported tests and tests that haven't already been ran
            if (GitHubClient.SUPPORTED_EVENT_TYPES.contains(activityType)) {
                Map<String, Object> activityParts = gitHubClient.getPartsForActivity(inventoryItem, activity);
                String title = (String)activityParts.get("title");
                String content = (String)activityParts.get("content");
                Set<String> hashtags = (Set<String>)activityParts.get("hashtags");
                String titleRegex = "^" + validCharsClassRegex + "+ ";
                String contentRegex = null;
                Set<String> expectedActivityHashtags = null;

                Assert.assertTrue(hashtags.contains("#" + projectKey.toLowerCase()));
                Assert.assertTrue(hashtags.contains("#github"));

                if (activityType.equals("CommitCommentEvent")) {
                    titleRegex += ("commented on " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^Comment in [a-zA-Z0-9]{10}: [\\s\\S]+$";
                    expectedActivityHashtags = ImmutableSet.of("#comment", "#source", "#changeset");
                } else if (activityType.equals("CreateEvent")) {
                    // We need to test three different create events
                    String refType = activity.getJSONObject("payload").getString("ref_type");

                    titleRegex += ("created " + refType +
                            (refType.equals("repository") ? " " + validCharsClassRegex + "+$" : " " +
                                    validCharsClassRegex + "+ at " + validCharsClassRegex + "+/" +
                                    validCharsClassRegex + "+$"));
                    contentRegex = (refType.equals("repository") ? "^.*$" : "New " + refType + " is at /" +
                            validCharsClassRegex + "+/" + validCharsClassRegex + "+/tree/" +
                            validCharsClassRegex + "+$");
                    expectedActivityHashtags = ImmutableSet.of("#source", "#create", "#" + refType);
                } else if (activityType.equals("DeleteEvent")) {
                    // We need to test two different create events
                    String refType = activity.getJSONObject("payload").getString("ref_type");

                    titleRegex += ("deleted " + refType + " " + validCharsClassRegex + "+ at " + validCharsClassRegex +
                            "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^Deleted " + refType + " was at /" + validCharsClassRegex + "+/" +
                            validCharsClassRegex + "+/tree/" + validCharsClassRegex + "+$";
                    expectedActivityHashtags = ImmutableSet.of("#source", "#delete", "#" + refType);
                } else if (activityType.equals("DownloadEvent")) {
                    titleRegex += ("uploaded a file to " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^\".+\" is at /" + validCharsClassRegex + "+/" + validCharsClassRegex +
                            "+/downloads\n.+$";
                    expectedActivityHashtags = ImmutableSet.of("#download");
                } else if (activityType.equals("ForkEvent")) {
                    titleRegex += "forked " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$";
                    contentRegex = "Forked repository is at " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$";
                    expectedActivityHashtags = ImmutableSet.of("#repository", "#fork", "#create");
                } else if (activityType.equals("ForkApplyEvent")) {
                    titleRegex += "applied fork commits to " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$";
                    contentRegex = "([a-zA-Z0-9]{7} .+[\\n]*)+";
                    expectedActivityHashtags = ImmutableSet.of("#fork", "#apply", "#source", "#changeset");
                } else if (activityType.equals("GollumEvent")) {
                    JSONArray pages = activity.getJSONObject("payload").getJSONArray("pages");
                    String action = null;

                    if (pages.size() <= 1) {
                        action = pages.getJSONObject(0).getString("action");
                        titleRegex += (action + " the " + validCharsClassRegex + "+/" + validCharsClassRegex + "+ wiki$");
                    } else {
                        titleRegex += ("made multiple changes to the " + validCharsClassRegex + "+/" + validCharsClassRegex + "+ wiki$");
                    }

                    contentRegex = "((Created|Edited) .+[\\n]*)+";
                    expectedActivityHashtags = ImmutableSet.of("#wiki");
                } else if (activityType.equals("IssueCommentEvent")) {
                    titleRegex += ("commented on (issue|pull request) \\d+ on " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^[\\s\\S]+$";
                    expectedActivityHashtags = ImmutableSet.of("#issue", "#comment", "#" + activity.getJSONObject("payload").getJSONObject("issue").getString("state"));
                } else if (activityType.equals("IssuesEvent")) {
                    titleRegex += ("(opened|closed|reopened) issue \\d+ on " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^[\\s\\S]+$";
                    expectedActivityHashtags = ImmutableSet.of("#issue", "#" + activity.getJSONObject("payload").getJSONObject("issue").getString("state"));
                } else if (activityType.equals("MemberEvent")) {
                    titleRegex += (" added " + validCharsClassRegex + "+ to " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = ("^" + validCharsClassRegex + "+ is at " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    expectedActivityHashtags = ImmutableSet.of("#repository", "#membership");
                } else if (activityType.equals("PublicEvent")) {
                    titleRegex += ("open sourced " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^[\\s\\S]+$";
                    expectedActivityHashtags = ImmutableSet.of("#repository", "#opensourced");
                } else if (activityType.equals("PullRequestEvent")) {
                    titleRegex += ("(opened|closed|synchronized|reopened|merged) pull request \\d+ on " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^.+\\n\\d+ commits with \\d+ additions and \\d+ deletions$";
                    String action = activity.getJSONObject("payload").getString("action").equals("closed") ? "merged" : activity.getJSONObject("payload").getString("action"); //handle case where we tranform "closed" to "merged"
                    expectedActivityHashtags = ImmutableSet.of("#pullrequest", "#source", "#" +  action);
                } else if (activityType.equals("PushEvent")) {
                    titleRegex += ("pushed to " + validCharsClassRegex + "+ at " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "([a-zA-Z0-9]{7}[\\s\\S]+[\\n]*)*";
                    expectedActivityHashtags = ImmutableSet.of("#source", "#changeset");
                } else if (activityType.equals("WatchEvent")) {
                    titleRegex += ("started watching " + validCharsClassRegex + "+/" + validCharsClassRegex + "+$");
                    contentRegex = "^" + validCharsClassRegex + "+ description: \\n.+$";
                    expectedActivityHashtags = ImmutableSet.of("#repository", "#watch");
                }

                Assert.assertTrue(activityType + " title (" + title + ") does not match regex (" + titleRegex + ").", title.matches(titleRegex));
                Assert.assertTrue(activityType + " content (" + content + ") does not match regex (" + contentRegex + ").", content.matches(contentRegex));

                for (String hashtag : expectedActivityHashtags) {
                    Assert.assertTrue(activityType + " hashtag (" + hashtag + ") is not present as expected",
                                      hashtags.contains(hashtag));
                }
            }
        }
    }

}
