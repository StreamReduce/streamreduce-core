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
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.http.Header;
import org.scribe.oauth.OAuthService;
import org.springframework.util.Assert;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GitHubClient provides necessary methods for interacting with GitHub.
 */
public class GitHubClient extends AbstractProjectHostingClient {

    public static final Set<String> SUPPORTED_EVENT_TYPES = ImmutableSet.of(
            "CommitCommentEvent", // This tests a commit comment
            "CreateEvent", // This tests create events (branch, repository and tag)
            "DeleteEvent", // This tests delete events (branch and tag)
            "DownloadEvent", // This tests file upload events
            "ForkEvent", // This tests for events
            "ForkApplyEvent", // This tests the fork apply (apply patch from fork) event
            "GollumEvent", // This tests the wiki page changes
            "IssueCommentEvent", // This tests issue comment events
            "IssuesEvent", // This tests issue events
            "MemberEvent", // This tests membership events
            "PublicEvent", // This tests when a private repository goes public
            "PullRequestEvent", // This tests pull request events
            "PushEvent", // This tests push events
            "WatchEvent" // This tests watch events
    );
    public static final String GITHUB_API_BASE = "https://api.github.com/";

    final OAuthService oAuthService;


    /**
     * Constructs a client for GitHub using the credentials in the connection provided.
     *
     * @param connection the connection to use for interacting with GitHub
     */
    public GitHubClient(Connection connection, OAuthService oAuthService) {
        super(connection);

        this.oAuthService = oAuthService;

        Assert.isTrue(connection.getProviderId().equals(ProviderIdConstants.GITHUB_PROVIDER_ID));

        debugLog(LOGGER, "Client created for " + getConnectionCredentials().getIdentity());
    }

    /**
     * Returns a JSON array representing the all GitHub repositories connection user has access to.
     *
     * Note: This includes all repositories the user owns (public/private), watches and can see as part
     *       of an organization.
     *
     * @return the JSON representation of the available GitHub projects
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     * @throws IOException if anything else goes wrong
     */
    public List<JSONObject> getRepositories() throws InvalidCredentialsException, IOException {
        // To gather the full list of repositories we're interested in, we have to make multiple calls to the GitHub API:
        //
        //   * user/repos: Repositories the user owns
        //   * user/watched: Repositories the user watches
        //   * orgs/<org_name>/repos: Repositories the user can see in the organization the user is a member of (if any)

        debugLog(LOGGER, "Getting repositories visible to " + getConnectionCredentials().getIdentity());

        List<JSONObject> repositories = new ArrayList<JSONObject>();
        Set<String> reposUrls = new HashSet<String>();

        reposUrls.add(GITHUB_API_BASE + "user/repos");
        reposUrls.add(GITHUB_API_BASE + "user/watched");

        for (String repoUrl : reposUrls) {
            List<JSONObject> reposFromUrl = makeRequest(repoUrl);

            for (JSONObject repo : reposFromUrl) {
                repositories.add(repo);
            }
        }

        debugLog(LOGGER, "  Repositories found: " + repositories.size());

        return repositories;
    }

    /**
     * Returns a list of organizations the user is a member of.
     *
     * @return list of JSONObjects representing the organizations the user is a member of
     *
     * @throws InvalidCredentialsException if the connection associated with this client has invalid credentials
     * @throws IOException if anything goes wrong making the actual request
     */
    @SuppressWarnings("unused")  //Default Oauth scope causes 404 when reading from orgs
    public List<JSONObject> getOrganizations() throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Getting organzation " + getConnectionCredentials().getIdentity() + " is a member of");

        List<JSONObject> organizations = makeRequest(GITHUB_API_BASE + "user/orgs");

        debugLog(LOGGER, "  Organizations: " + organizations.size());

        return organizations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection() throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Validating connection");
        getUser();
    }


    public JSONObject getUser() throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Validating connection");

        String userUrl = GITHUB_API_BASE + "user";

        try {
            List<JSONObject> response =  makeRequest(userUrl);
            return (response.size() == 1 ? response.get(0) : null);
        } catch (InvalidCredentialsException e) {
            throw new InvalidCredentialsException("The GitHub connection credentials for " +
                                                          getConnectionCredentials().getIdentity() + " are invalid.");
        }
    }

    /**
     * Returns a JSONObject representing the comparison of two commits on a repository.
     *
     * @param repoName the repository whose revisions we're comparing
     * @param before the first revision
     * @param after the second revision
     *
     * @return a JSONObject describing the commits comparison
     *
     * @throws InvalidCredentialsException if the connection credentials are invalid
     * @throws IOException if any other communication error happens
     */
    public JSONObject compareCommits(String repoName, String before, String after)
            throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Comparing commits between " + before + " and " + after + " on " + repoName);

        String commitsUrl = GITHUB_API_BASE + "repos/" + repoName + "/compare/" + before + "..." + after;
        List<JSONObject> rawResponse = makeRequest(commitsUrl);

        // Should never happen where we're returning null but just in case
        return (rawResponse.size() == 1 ? rawResponse.get(0) : null);
    }

    /**
     * Returns a JSONObject representing a GitHub project/repository.
     *
     * @param repoName the repository name
     *
     * @return a JSONObject representing the GitHub project/repository
     *
     * @throws InvalidCredentialsException if the connection credentials are invalid
     * @throws IOException if any other communication error happens
     */
    public JSONObject getRepositoryDetails(String repoName) throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Getting repository details for " + repoName);

        String projectUrl = GITHUB_API_BASE + "repos/" + repoName;
        List<JSONObject> rawResponse = makeRequest(projectUrl);

        // Should never happen where we're returning null but just in case
        return (rawResponse.size() == 1 ? rawResponse.get(0) : null);
    }

    /**
     * Retrieves all activity for the projects passed in with no limit on the maximum number of activity
     * entries returned.
     *
     * @see #getActivity(java.util.Set, int)
     */
    public List<JSONObject> getActivity(Set<String> projectKeys) throws InvalidCredentialsException, IOException {
        return getActivity(projectKeys, Integer.MAX_VALUE);
    }

    /**
     * Retrieves the activity for the given connection based on the last poll date stored in the connection.
     *
     * Note: This list is already sorted in the proper order, contains no duplicates and contains only entries that
     *       are pertinent:
     *
     *         * Entries will correspond with a project in the projectKeys set unless that set is empty/null and then
     *           entries can be for any project
     *         * Entries will after the last activity date in the connection
     *
     * @param projectKeys the project keys we're interested in or null for all
     * @param maxActivities the maximum number of results to return
     *
     * @return list of JSONObjects representing activity entries
     *
     * @throws InvalidCredentialsException if the connection associated with this client has invalid credentials
     * @throws IOException if anything goes wrong making the actual request
     */
    public List<JSONObject> getActivity(Set<String> projectKeys, int maxActivities)
            throws InvalidCredentialsException, IOException {
        // The way we gather activity for a GitHub connection is by making a few events feeds calls, merging them
        // together and then returning the results.  The end result should be a list of pertinent events that have no
        // duplicates and includes all necessary events after the last poll period.
        //
        // The GitHub API request logic looks like this:
        //
        //   * /users/<user_id>/received_events: This is the list of events that the user has "received" by watching
        //                                       repositories/users.
        //   * /users/<user_id>/events: This is a list of events that the user itself has created.
        //   * /users/<user_id>/events/orgs/<org_id>: This is a list of events that have been performed within the
        //                                            the organization.  (This will also require a call prior to this
        //                                            to get the user's organizations, if any.)

        debugLog(LOGGER, "Getting activity");

        // Establish some defaults for fields that can be null
        projectKeys = (projectKeys != null ? projectKeys : new HashSet<String>());
        maxActivities = (maxActivities >= 1 ? maxActivities : 100);

        List<JSONObject> allActivityItems = new ArrayList<JSONObject>();
        Set<Integer> processedActivityHashes = new HashSet<Integer>();
        Date lastActivity = getLastActivityPollDate();
        Set<String> eventsUrls = new HashSet<String>();
        String username = getConnectionCredentials().getIdentity();

        // Generate the list of events URLs to process
        eventsUrls.add(GITHUB_API_BASE + "users/" + username + "/received_events"); // User's received events
        eventsUrls.add(GITHUB_API_BASE + "users/" + username + "/events"); // User's events

//        // To generate the list of organization URLs to process, we need to walk through the user's organizations list
//        List<JSONObject> organizations = getOrganizations();
//
//        for (JSONObject organization : organizations) {
//            eventsUrls.add(GITHUB_API_BASE + "users/" + username + "/events/orgs/" + organization.getString("login"));
//        }

        for (String eventUrl : eventsUrls) {
            List<JSONObject> rawActivity = makeRequest(eventUrl, maxActivities, false);

            for (JSONObject activity : rawActivity) {
                String eventType = activity.getString("type");
                String repoName = activity.getJSONObject("repo").getString("name");
                Date activityDate = getCreatedDate(activity);

                // If we do not support the event type or its for a repository we don't monitor, move on
                if (!SUPPORTED_EVENT_TYPES.contains(eventType) || !projectKeys.contains(repoName)) {
                    continue;
                }

                if (activityDate.before(lastActivity)) {
                    break;
                }

                int activityHash = activity.hashCode();

                if (!processedActivityHashes.contains(activityHash) && allActivityItems.size() < maxActivities) {
                    allActivityItems.add(activity);
                    processedActivityHashes.add(activityHash);
                }
            }
        }

        // Sort the activities
        Collections.sort(allActivityItems, new Comparator<JSONObject>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(JSONObject jo0, JSONObject jo1) {
                Date jod0 = getCreatedDate(jo0);
                Date jod1 = getCreatedDate(jo1);

                return jod0.compareTo(jod1);
            }
        });

        // Return only the maximum number of results if the list of activities is greater than the maximum requested
        if (allActivityItems.size() > maxActivities) {
            allActivityItems = allActivityItems.subList(0, maxActivities);
        }

        debugLog(LOGGER, "  Activities found: " + allActivityItems.size());

        return allActivityItems;
    }

    /**
     * Returns a map with the following keys in it or null if the entry is unhandleable:
     *
     *   * title: This is the title of the activity
     *   * content: This is the content of the activity (Summarizing changes when necessary)
     *   * hashtags: A set of hashtags
     *
     * The event processing only handles repository events at this time.  That being said, these events are known events
     * that are not handled at this time:
     *
     *   * FollowEvent: This is a user-specific event
     *   * GistEvent: Gists are outside of the repository/project boundary
     *   * TeamAddEvent: This is an organization-specific event
     *
     * @param inventoryItem the inventory item the activity entry corresponds to
     * @param entry the JSONObject to parse
     *
     * @return the map described above
     *
     * @throws InvalidCredentialsException if the connection credentials are invalid (in the event they are used)
     * @throws IOException if there is an issue making requests (if necessary)
     */
    public Map<String, Object> getPartsForActivity(InventoryItem inventoryItem, JSONObject entry)
            throws InvalidCredentialsException, IOException {
        Assert.isTrue(getConnectionId().equals(inventoryItem.getConnection().getId()));

        // GitHub activity information gathering is pretty straight forward thanks to GitHub's events
        // API (http://developer.github.com/v3/events/).  Basically, there are a finite set of GitHub
        // event types (http://developer.github.com/v3/events/types/) and knowing this we can deduce
        // the activity/event title, content and hashtags in a pretty simple fashion.  For extensive
        // details, look at the large if/else statement below and each branch will outline the format
        // for the title, content and the generated hashtags for each event type.
        //
        // Note: We do generate the title and content of each activity to be just like what the stream
        //       text would look like.  So unlike Jira, where the title/content are given to us, we
        //       do in fact generate the title/content.

        String activityType = entry.getString("type");
        JSONObject payload = entry.getJSONObject("payload");
        String repoName = entry.getJSONObject("repo").getString("name");
        Map<String, Object> activityParts = new HashMap<String, Object>();
        StringBuilder title = new StringBuilder();
        StringBuilder content = new StringBuilder();
        Set<String> hashtags = new HashSet<String>();

        // Bring in the inventory item hashtags
        for (String hashtag : inventoryItem.getHashtags()) {
            hashtags.add(hashtag);
        }

        // Always add the project
        hashtags.add("#" + repoName.toLowerCase());

        // Generate the title based on common event properties.  The titles will all have the same
        // common structure: [actor.login] [action] at [repo.name]
        title.append(entry.getJSONObject("actor").getString("login"))
             .append(" ");

        // Generate the action based on the event type
        if (activityType.equals("CommitCommentEvent")) {
            // [actor.login] commented on [repo.name]

            title.append("commented on ")
                 .append(repoName);

            // Comment in [payload.comment.commit_id]: [payload.comment.body]
            JSONObject comment = payload.getJSONObject("comment");

            content.append("Comment in ")
                   .append(comment.getString("commit_id").substring(0, 10))
                   .append(": ")
                   .append(comment.getString("body"));

            // Commit comment hashtags:
            //
            // #comment
            // #source
            // #changeset
            hashtags.add("#comment");
            hashtags.add("#source");
            hashtags.add("#changeset");
        } else if (activityType.equals("CreateEvent")) {
            // Branch    : [actor.login] created branch [payload.ref] at [repo.name]
            // Tag       : [actor.login] created tag [payload.ref] at [repo.name]
            // Repository: [actor.login] created repository [repo.name (without owner)]
            String refType = payload.getString("ref_type");
            String ref = payload.getString("ref");

            title.append("created ")
                 .append(refType)
                 .append(" ");

            if (refType.equals("repository")) {
                title.append(repoName.split("/")[1]);
            } else {
                title.append(ref)
                     .append(" at ")
                     .append(repoName);
            }

            // Branch    : New branch is at /[repo.name]/tree/[payload.ref]
            // Tag       : New tag is at /[repo.name]/tree/[payload.ref]
            // Repository: [payload.description]
            if (refType.equals("branch") || refType.equals("tag")) {
                content.append("New ")
                       .append(refType)
                       .append(" is at /")
                       .append(repoName)
                       .append("/tree/")
                       .append(ref);
            } else if (refType.equals("repository")) {
                content.append(payload.getString("description"));
            }

            // Create event hashtags:
            //
            // #source
            // #create
            // #[payload.ref_type]
            hashtags.add("#source");
            hashtags.add("#create");
            hashtags.add("#" + refType);
        } else if (activityType.equals("DeleteEvent")) {
            // Branch: [actor.login] deleted branch [payload.ref] at [repo.name]
            // Tag   : [actor.login] deleted tag [payload.ref] at [repo.name]
            String refType = payload.getString("ref_type");
            String ref = payload.getString("ref");

            title.append("deleted ")
                 .append(refType)
                 .append(" ")
                 .append(ref)
                 .append(" at ")
                 .append(repoName);

            // Branch : Deleted branch was at /[repo.name]/tree/[payload.ref]
            // Tag    : Deleted tag was at /[repo.name]/tree/[payload.ref]
            content.append("Deleted ")
                   .append(refType)
                   .append(" was at /")
                   .append(repoName)
                   .append("/tree/")
                   .append(ref);

            // Delete event hashtags:
            //
            // #source
            // #delete
            // #[payload.ref_type]
            hashtags.add("#source");
            hashtags.add("#delete");
            hashtags.add("#" + refType);
        } else if (activityType.equals("DownloadEvent")) {
            // [actor.login] uploaded a file to [repo.name]
            title.append("uploaded a file to ")
                 .append(repoName);

            // "[download.name]" is at /[repo.name]/downloads
            // [download.description]
            JSONObject download = payload.getJSONObject("download");

            content.append("\"")
                   .append(download.getString("name"))
                   .append("\" is at /")
                   .append(repoName)
                   .append("/downloads")
                   .append("\n")
                   .append(download.getString("description"));

            // Download event hashtags:
            //
            // #download
            hashtags.add("#download");
        } else if (activityType.equals("ForkEvent")) {
            // [actor.login] forked [repo.name]
            title.append("forked ")
                 .append(repoName);

            // Forked repository is at [actor.login/payload.forkee/name]
            JSONObject forkee = payload.getJSONObject("forkee");

            content.append("Forked repository is at ")
                   .append(entry.getJSONObject("actor").getString("login"))
                   .append("/")
                   .append(forkee.getString("name"));

            // Fork event hashtags:
            //
            // #repository
            // #fork
            // #create
            hashtags.add("#repository");
            hashtags.add("#fork");
            hashtags.add("#create");
        } else if (activityType.equals("ForkApplyEvent")) {
            // [actor.login] applied fork commits to [repo.name]
            title.append("applied fork commits to ")
                 .append(repoName);

            // We have to retrieve the commits between the [payload.before] and [payload.after] and then render
            // each commit as follows:
            // [commit.sha] [commit.message]
            JSONObject commitsComparison = compareCommits(repoName, payload.getString("before"),
                                                          payload.getString("after"));
            JSONArray commits = commitsComparison.getJSONArray("commits");

            for (int i = 0; i < commits.size(); i++) {
                JSONObject commit = commits.getJSONObject(i);

                if (i != 0) {
                    content.append("\n");
                }

                content.append(commit.getString("sha").substring(0, 7))
                       .append(" ")
                       .append(commit.getJSONObject("commit").getString("message").split("\n")[0]);
            }

            // Fork apply event hashtags:
            //
            // #fork
            // #apply
            // #source
            // #changeset
            hashtags.add("#fork");
            hashtags.add("#apply");
            hashtags.add("#source");
            hashtags.add("#changeset");
        } else if (activityType.equals("GollumEvent")) {
            // Single change   : [actor.login] [payload.pages[0].action] the [repo.name] wiki
            // Multiple changes: [actor.login] made multiple changes to the [repo.name] wiki
            JSONArray pages = payload.getJSONArray("pages");

            if (pages.size() == 1) {
                JSONObject page = pages.getJSONObject(0);

                title.append(page.getString("action"))
                     .append(" the");
            } else {
                title.append("made multiple changes to the");
            }

            title.append(" ")
                 .append(repoName)
                 .append(" wiki");

            // For each page change:
            // [page[i].action] [page[i].title]

            for (int i = 0; i < pages.size(); i++) {
                JSONObject page = pages.getJSONObject(i);
                String action = page.getString("action");

                if (i != 0) {
                    content.append("\n");
                }

                content.append(action.substring(0, 1).toUpperCase())
                       .append(action.substring(1))
                       .append(" ")
                       .append(page.getString("title"))
                       .append(".");
            }

            // Gollum event hashtags
            //
            // #wiki
            hashtags.add("#wiki");
        } else if (activityType.equals("IssueCommentEvent")) {
            // [actor.login] commented on issue [payload.issue.number] on [repo.name]
            JSONObject issue = payload.getJSONObject("issue");

            title.append("commented on ")
                 .append(issue.getJSONObject("pull_request").get("diff_url") instanceof JSONNull ? "issue " : "pull request ")
                 .append(issue.getInt("number"))
                 .append(" on ")
                 .append(repoName);

            // [payload.comment.body]
            content.append(payload.getJSONObject("comment").getString("body"));

            // Issue comment event hashtags
            //
            // #comment
            // #issue
            // #[issue.state]
            // #[labels]
            hashtags.add("#comment");
            hashtags.add("#issue");
            hashtags.add("#" + issue.getString("state"));

            JSONArray labels = issue.getJSONArray("labels");

            for (int i = 0; i < labels.size(); i++) {
                hashtags.add("#" + labels.getJSONObject(i).getString("name"));
            }
        } else if (activityType.equals("IssuesEvent")) {
            // [actor.login] [payload.issue.action] issue [payload.issue.number] on [repo.name]
            JSONObject issue = payload.getJSONObject("issue");

            title.append(payload.getString("action"))
                 .append(" issue ")
                 .append(issue.getInt("number"))
                 .append(" on ")
                 .append(repoName);

            // [payload.issue.title]
            content.append(issue.getString("title"));

            // Issue event hashtags
            //
            // #issue
            // #[issue.state]
            // #[labels]
            hashtags.add("#issue");
            hashtags.add("#" + issue.getString("state"));

            JSONArray labels = issue.getJSONArray("labels");

            for (int i = 0; i < labels.size(); i++) {
                hashtags.add("#" + labels.getJSONObject(i).getString("name"));
            }
        } else if (activityType.equals("MemberEvent")) {
            // [actor.login] added [payload.member.login] to [repo.name]
            title.append("added ")
                 .append(payload.getJSONObject("member"))
                 .append(" to ")
                 .append(repoName);

            // [repo.name] is at [repo.name]
            content.append(repoName.split("/")[1])
                   .append(" is at ")
                   .append(repoName);

            // Member event hashtags
            //
            // #repository
            // #membership
            hashtags.add("#repository");
            hashtags.add("#membership");
        } else if (activityType.equals("PublicEvent")) {
            // [actor.login] open sourced [repo.name]
            title.append("open sourced ")
                 .append(repoName);

            // We have to retrieve the project information and then this is the format:
            // [project.description]
            content.append(getRepositoryDetails(repoName).getString("description"));

            // Public event hashtags
            //
            // #repository
            // #opensourced
            hashtags.add("#repository");
            hashtags.add("#opensourced");
        } else if (activityType.equals("PullRequestEvent")) {
            // [actor.login] [action] pull request [payload.issue.number] on [repo.name]
            String action = payload.getString("action");
            boolean merged = payload.getJSONObject("pull_request").getBoolean("merged");

            title.append(action.equals("closed") && merged ? "merged" : action)
                 .append(" pull request ")
                 .append(payload.getJSONObject("pull_request").getInt("number"))
                 .append(" on ")
                 .append(repoName);

            // [payload.pull_request.title]
            // [payload.pull_request.commits] commits with [payload.pull_request.additions] additions and [payload.pull_request.deletions] deletions
            JSONObject pullRequest = payload.getJSONObject("pull_request");

            content.append(pullRequest.getString("title"))
                   .append("\n")
                   .append(pullRequest.getInt("commits"))
                   .append(" commits with ")
                   .append(pullRequest.getInt("additions"))
                   .append(" additions and ")
                   .append(pullRequest.getInt("deletions"))
                   .append(" deletions");

            // Pull request hashtags
            //
            // #source
            // #pullrequest
            // #[action]
            hashtags.add("#source");
            hashtags.add("#pullrequest");
            hashtags.add("#" + (action.equals("closed") && merged ? "merged" : action));
        } else if (activityType.equals("PushEvent")) {
            // [actor.login] pushed to [branch] at [repo.name]
            String ref = payload.getString("ref");

            // Shorten the ref
            ref = ref.substring(ref.lastIndexOf("/") + 1);

            title.append("pushed to ")
                 .append(ref)
                 .append(" at ")
                 .append(repoName);

            // We have to list each commit in the push with this format:
            // [commit.sha] [commit.message]

            // Sometimes a PushEvent can omit the commits property
            if (payload.has("commits")) {
                JSONArray commits = payload.getJSONArray("commits");

                for (int i = 0; i < commits.size(); i++) {
                    JSONObject commit = commits.getJSONObject(i);
                    if (i != 0) {
                        content.append("\n");
                    }

                    content.append(commit.getString("sha").substring(0,7))
                           .append(" ")
                           .append(commit.getString("message").split("\n")[0]);
                }
            }

            // Push event hashtags
            //
            // #source
            // #changeset
            hashtags.add("#source");
            hashtags.add("#changeset");
        } else if (activityType.equals("WatchEvent")) {
            // [actor.login] [payload.action] watching [repo.name]
            title.append(payload.getString("action"))
                 .append(" watching ")
                 .append(repoName);

            // [repo.name] description:
            // [project.description]
            content.append(repoName.split("/")[1])
                   .append(" description: \n")
                   .append(getRepositoryDetails(repoName).getString("description"));

            // Watch event hashtags
            //
            // #repository
            // #watch
            hashtags.add("#repository");
            hashtags.add("#watch");
        } else {
            LOGGER.error("Unsupported GitHub event type: " + activityType);
            return null;
        }

        activityParts.put("title", title.toString());
        activityParts.put("content", content.toString());
        activityParts.put("hashtags", hashtags);

        return activityParts;
    }

    /**
     * Returns a Date object for the value represented in the JSON object's 'created_at' property.
     *
     * @param jsonObject the JSON object to parse
     *
     * @return the date value of the 'created_at' property of the JSON object or null otherwise
     */
    private Date getCreatedDate(JSONObject jsonObject) {
        String rawActivityDate = (jsonObject.has("created_at") ? jsonObject.getString("created_at") : null);
        Date activityDate = null;

        if (rawActivityDate != null) {
            // Example date: 2011-09-06T17:26:27Z
            try {
                activityDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(rawActivityDate);
            } catch (ParseException e) {
                LOGGER.error("Unable to parse the date (" + rawActivityDate + "): " + e.getMessage());
                e.printStackTrace();
            }
        }

        return activityDate;
    }

    /**
     * Make a request using cache with no maximum number of items returned.
     *
     * @see #makeRequest(String, int, boolean)
     */
    private List<JSONObject> makeRequest(String url) throws InvalidCredentialsException, IOException {
        return makeRequest(url, Integer.MAX_VALUE, true);
    }

    /**
     * Make a request using cache and limited by maxResults.
     *
     * @see #makeRequest(String, int, boolean)
     */
    private List<JSONObject> makeRequest(String url, int maxResults) throws InvalidCredentialsException, IOException {
        return makeRequest(url, maxResults, true);
    }

    /**
     * Makes a call to GitHub and handles pagination of the results.
     *
     * @param url the GitHub URL to make a call to
     * @param maxResults the maximum number of results to return
     * @param cache use caching
     *
     * @return a list of JSONObjects or an empty list if the response had no content
     *
     * @throws InvalidCredentialsException if the connection associated with this client has invalid credentials
     * @throws IOException if anything goes wrong making the actual request
     */
    @SuppressWarnings("unchecked")
    public List<JSONObject> makeRequest(String url, int maxResults, boolean cache)
            throws InvalidCredentialsException, IOException {
        // Caching in GitHub is very, very simple.  Since all requests are made from the same user and each request
        // URL is unique, caching is as simple as mapping the cached result (when necessary) to the request URL.
        //
        // Note: We do not use cache for the actual activity requests

        Object objectFromCache = (cache ? requestCache.getIfPresent(url) : null);
        List<JSONObject> response = (objectFromCache != null ? (List<JSONObject>)objectFromCache : null);
        int pageSize = 100;

        // Quick return if there was an entry in the cache
        if (response != null) {
            debugLog(LOGGER, "  (From cache)");
            return response;
        } else {
            response = new ArrayList<JSONObject>();
        }

        List<Header> responseHeaders = new ArrayList<Header>();
        JSONArray rawResponse = new JSONArray();


        String payload = "";
        if (getAuthType().equals(AuthType.USERNAME_PASSWORD)) {
            payload = HTTPUtils.openUrl(url + "?per_page=" + pageSize,
                HttpMethod.GET, null,
                MediaType.APPLICATION_JSON,
                getConnectionCredentials().getIdentity(), getConnectionCredentials().getCredential(), null,
                responseHeaders);
        } else if (getAuthType().equals(AuthType.OAUTH)) {
            payload = HTTPUtils.openOAuthUrl(url + "?per_page=" + pageSize,
                                             HttpMethod.GET, null,
                                             MediaType.APPLICATION_JSON, oAuthService,
                                             getConnectionCredentials(), null, responseHeaders);
        }

        try {
            // Try to parse as a JSONArray knowing that it might be a JSONObject
            rawResponse = JSONArray.fromObject(payload);
        } catch (JSONException e) {
            try {
                // Try to parse as a JSONObject
                JSONObject rawObject = JSONObject.fromObject(payload);
                rawResponse.add(rawObject);
            } catch (JSONException e2) {
                // Fail
                return null;
            }
        }

        int sizeOfArray = rawResponse.size();
        List<Integer> processedObjectHashes = new ArrayList<Integer>();
        int page = 1;
        boolean complete = false;

        while(!complete && response.size() < maxResults) {
            for (Object anArrayResponse : rawResponse) {
                JSONObject arrayEntry = JSONObject.fromObject(anArrayResponse);
                int entryHash = arrayEntry.hashCode();

                if (sizeOfArray < maxResults && !processedObjectHashes.contains(entryHash)) {
                    response.add(arrayEntry);
                    processedObjectHashes.add(entryHash);
                }

                if (sizeOfArray == maxResults) {
                    complete = true;
                    break;
                }
            }

            // If we returned fewer entries than the max, we know pagination isn't necessary
            if (sizeOfArray < pageSize) {
                complete = true;
            }

            // GitHub pagination is pretty simple.  Basically if there is a Link header, we can expect that pagination
            // is necessary: http://developer.github.com/v3/#pagination
            boolean linkHeaderFound = false;

            for (Header header : responseHeaders) {
                if ( header.getName() != null && header.getName().equals("Link")) {
                    linkHeaderFound = true;
                    break;
                }
            }

            if (!linkHeaderFound) {
                complete = true;
            }

            // If we've decided that pagination is necessary, move on to the next page and continue processing
            if (!complete) {
                page++;
                if (getAuthType().equals(AuthType.USERNAME_PASSWORD)) {
                    rawResponse = JSONArray.fromObject(
                            HTTPUtils.openUrl(url + "?per_page=" + pageSize + "&page=" + page, "GET", null,
                                              MediaType.APPLICATION_JSON, getConnectionCredentials().getIdentity(),
                                              getConnectionCredentials().getCredential(), null, responseHeaders));
                } else if (getAuthType().equals(AuthType.OAUTH)) {
                    rawResponse = JSONArray.fromObject(
                            HTTPUtils.openOAuthUrl(url + "?per_page=" + pageSize + "&page=" + page, "GET", null,
                                                   MediaType.APPLICATION_JSON, oAuthService,
                                                   getConnectionCredentials(), null, responseHeaders));
                }
                sizeOfArray = rawResponse.size();
            }
        }

        // Cache the entry if necessary
        if (cache) {
            requestCache.put(url, response);
        }

        return response;
    }

}
