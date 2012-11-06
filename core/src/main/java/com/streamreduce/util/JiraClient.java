package com.streamreduce.util;

import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.ProjectHostingIssue;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.abdera.model.Entry;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.util.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JiraClient provides necessary methods for interacting with Jira.
 */
public class JiraClient extends AbstractProjectHostingClient {

    private final String encodingStyle = "http://schemas.xmlsoap.org/soap/encoding/";
    private final String jiraBeanSchema = "http://beans.soap.rpc.jira.atlassian.com";

    private String jiraRestAPIBase = null;
    private String jiraToken = null;
    private String confluenceToken = null;

    private enum JiraStudioApp {
        CONFLUENCE,
        JIRA
    }

    /**
     * Constructs a client for GitHub using the credentials in the connection provided.
     *
     * @param connection the connection to use for interacting with GitHub
     */
    public JiraClient(Connection connection) {
        super(connection);

        Assert.isTrue(connection.getProviderId().equals(ProviderIdConstants.JIRA_PROVIDER_ID));

        init();
    }

    /**
     * Initializes the client.
     */
    private void init() {
        debugLog(LOGGER, "Client created for " + getConnectionCredentials().getIdentity());

        jiraRestAPIBase = getBaseUrl() + "/rest/api/latest/";

        try {
            jiraToken = login(JiraStudioApp.JIRA);
            confluenceToken = login(JiraStudioApp.CONFLUENCE);
        } catch (SOAPException e) {
            LOGGER.error("Unable to login to Jira via SOAP for connection (" + getConnectionId() + ")", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanUp() {
        try {
            logout(JiraStudioApp.JIRA, jiraToken);
            logout(JiraStudioApp.CONFLUENCE, confluenceToken);
        } catch (SOAPException e) {
            LOGGER.error("Unable to logout of Jira via SOAP for connection " + getConnectionId(), e);
        }

        super.cleanUp();
    }

    /**
     * Returns a JSON object representing a Jira project's details.
     *
     * @param projectKey the project's key
     * @return the JSON representation of a project
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     * @throws IOException                 if anything else goes wrong
     */
    public JSONObject getProjectDetails(String projectKey) throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Getting project details for " + projectKey);

        String projectDetailsUrl = jiraRestAPIBase + "project/" + projectKey;
        List<JSONObject> rawResponse = makeRESTRequest(projectDetailsUrl, Integer.MAX_VALUE, true, false);

        // Should never happen where we're returning null but just in case
        return (rawResponse.size() == 1 ? rawResponse.get(0) : null);
    }

    /**
     * Returns a list of JSONObjects representing the Jira projects the connecting user has access to.
     *
     * @param anonymous whether the request should be done anonymously
     * @return the JSON representation of the available Jira projects
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     * @throws IOException                 if anything else goes wrong
     */
    public List<JSONObject> getProjects(boolean anonymous) throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Getting projects" + (anonymous ? " anonymously" : ""));

        String projectsUrl = jiraRestAPIBase + "project";
        List<JSONObject> projects;

        if (anonymous) {
            projects = makeRESTRequest(projectsUrl, Integer.MAX_VALUE, true, true);
        } else {
            projects = makeRESTRequest(projectsUrl, Integer.MAX_VALUE, true, false);
        }

        debugLog(LOGGER, "  Projects found: " + (projects != null ? projects.size() : 0));

        return projects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection() throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Validating connection");

        String validationUrl = getBaseUrl() + "/rest/auth/1/session";

        makeRESTRequest(validationUrl, Integer.MAX_VALUE, true, false);
    }

    /**
     * Checks if a Jira project key is "public" by seeing if the project can be found by the anonymous user.
     *
     * @param projectKey the project key
     * @return whether or not the project key is a public (anonymously accessible) project
     * @throws InvalidCredentialsException Should never happen since credentials aren't used
     * @throws IOException                 Should never happen since the connection has been validated prior to use
     */
    public boolean isProjectPublic(String projectKey) throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Checking if " + projectKey + " is public");

        List<JSONObject> publicProjectsJSON = getProjects(true);

        for (JSONObject project : publicProjectsJSON) {
            if (project.getString("key").equals(projectKey)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves all activity for the projects passed in with no limit on the maximum number of activity
     * entries returned.
     *
     * @see #getActivity(java.util.Set, int)
     */
    public List<Entry> getActivity(Set<String> monitoredProjectKeys) {
        return getActivity(monitoredProjectKeys, Integer.MAX_VALUE);
    }

    /**
     * Retrieves the activity for the given connection based on the last poll date stored in the connection.
     * <p/>
     * Note: This list is already sorted in the proper order, contains no duplicates and contains only entries that
     * are pertinent:
     * <p/>
     * * Entries will correspond with a project in the projectKeys set unless that set is empty/null and then
     * entries can be for any project
     * * Entries will after the last activity date in the connection
     *
     * @param projectKeys   the project keys we're interested in or null for all
     * @param maxActivities the maximum number of results to return
     * @return list of Entry representing activity entries
     */
    public List<Entry> getActivity(Set<String> projectKeys, int maxActivities) {
        // The Jira Activity Stream is a combination of the connection's URL, the time in millis since the last
        // poll and a list of projects being monitored.  To make sure we only pull activity we need, we are
        // using a timestamp to know when we last polled and a list of monitored project keys, unless there are
        // no unmonitored projects in which case we will not provide any project keys, so we only pull in new
        // activity.  Here is an example URL to pull all SOBA activity since 2011-11-01 00:00:00.000:
        //
        //   [-----CONNECTION URL-----][-----COMMON PARTS-----][-PROJECT FILTER-][-------------DATE FILTER-------------]
        //   https://nodeable.jira.com/activity?maxActivities=100&stream=key+IS+SOBA&stream=update-date+AFTER+1320127200000
        //
        // To efficiently retrieve activity from Jira, we first make a request to get the activity after the
        // last poll date.  If the number of results is the maximum number of results, we might have a
        // situation where we need to handle pagination because there is more activity than we can retrieve
        // at one time.  The way we handle this is we figure out the oldest activity in the results and do a
        // between request so that we get the activity between the oldest known activity and the previous
        // poll until we get a response of zero, indicating no more activity.

        debugLog(LOGGER, "Getting activity");

        final int resultsPerPage = 100;
        final StringBuilder requestURLBuilder = new StringBuilder()
                .append(getBaseUrl())
                .append("/activity?maxResults=")
                .append(resultsPerPage);

        // Only pull in activity for projects we are monitoring
        requestURLBuilder.append("&streams=key+IS");

        for (String projectKey : projectKeys) {
            requestURLBuilder.append("+")
                    .append(projectKey);
        }

        // Create a base URL just in case we have to do subsequent calls for pagination
        final String baseRequestURL = requestURLBuilder.toString();
        Date afterDate = getLastActivityPollDate();
        String requestUrl = baseRequestURL + "&streams=update-date+AFTER+" + afterDate.getTime();

        List<Entry> allEntries = new ArrayList<Entry>();
        List<Entry> feedEntries = FeedUtils.getFeedEntries(requestUrl, getConnectionCredentials().getIdentity(),
                getConnectionCredentials().getCredential());

        // Since Jira doesn't have any pagination, we have to walk from newest activity to
        // oldest so we will retrieve all feed items first and then handle them so the
        // messages are created in the same order that the activity was created.
        while (feedEntries != null && feedEntries.size() > 0 && allEntries.size() < maxActivities) {
            for (Entry entry : feedEntries) {
                if (allEntries.size() < maxActivities) {
                    allEntries.add(entry);
                } else {
                    break;
                }
            }

            if (resultsPerPage == feedEntries.size()) {
                Date oldestActivityDate = feedEntries.get(feedEntries.size() - 1).getPublished();
                requestUrl = baseRequestURL + "&streams=update-date+BETWEEN+" + afterDate.getTime() +
                        "+" + oldestActivityDate.getTime();

                feedEntries = FeedUtils.getFeedEntries(requestUrl, getConnectionCredentials().getIdentity(),
                        getConnectionCredentials().getCredential());
            } else {
                feedEntries = null;
            }
        }

        // Reverse them since the order is newest to oldest and we want to create messages in the order
        // in which they really happened
        Collections.reverse(allEntries);

        debugLog(LOGGER, "  Activities found: " + allEntries.size());

        return allEntries;
    }

    /**
     * Returns a map with the following keys in it or null if the entry is unhandleable:
     * <p/>
     * * title: This is the title of the activity
     * * content: This is the content of the activity (Summarizing changes when necessary)
     * * hashtags: A set of hashtags
     *
     * @param inventoryItem the inventory item the activity entry corresponds to
     * @param entry         the JSONObject to parse
     * @return the map described above
     */
    public Map<String, Object> getPartsForActivity(InventoryItem inventoryItem, Entry entry) {
        Assert.isTrue(getConnectionId().equals(inventoryItem.getConnection().getId()));

        String projectKey = inventoryItem.getExternalId();
        Map<String, Object> activityParts = new HashMap<String, Object>();
        String title = entry.getTitle() != null ? MessageUtils.cleanEntry(entry.getTitle()) : null;
        String rawContent = concatRawTitleAndContent(entry);
        String content = entry.getContent() != null ? MessageUtils.cleanEntry(entry.getContent()) : null;
        Set<String> hashtags = new HashSet<String>();
        String applicationHashtag;
        String activityHashtag;
        String activityTargetHashtag = null;

        // Bring in the inventory item hashtags
        for (String hashtag : inventoryItem.getHashtags()) {
            hashtags.add(hashtag);
        }

        // If we cannot parse the title, we cannot create the message so do nothing and let the caller log
        if (title == null) {
            LOGGER.warn("There is no associated <title /> element for the Jira activity.");
            return null;
        }

        // Always add the project
        hashtags.add("#" + projectKey.toLowerCase());

        // Jira activity information gathering is a pain in the ass.  It's a pretty complex process that involves a lot
        // of raw XML parsing.  The activity title and content are pretty simple since they are given to us, other than
        // having to remove HTML tags from them due to the title/content being HTML strings, but the hashtags are where
        // things need a little extra explanation.
        //
        // Hashtags for Jira activity will always have three hashtags:
        //
        //  * Provider: This is the project hosting provider (#jira)
        //  * Application: A hashtag to indicate the Jira application the activity corresponds with like #issue
        //                 for all Jira activity, #source for all Fisheye/Crucible activity and #wiki for all
        //                 Confluence activity.
        //  * Activity: A hashtag to indicate the type of activity like #changeset
        //              for commits, #review for code reviews and others.
        //  * Generated: This is a hashtag or set of hashtags that gets generated based on the type of activity
        //               and/or the target of the activity.
        //
        // If the activity is a comment, there will be an additional #comment hashtag above and beyond the usually
        // generated hashtags.  Right now there are only two types of activity that will generate tags: issue activity
        // and wiki activity.  Each implementation documents its process in detail to explain how/why the code is
        // as-is.

        // Let's gather the application id
        org.apache.abdera.model.Element applicationElement = entry.getFirstChild(
                new QName("http://streams.atlassian.com/syndication/general/1.0", "application", "atlassian"));
        String application = applicationElement != null ? applicationElement.getText() : null;

        // Just in case, fail if there is no atlassian:application.
        if (application == null) {
            LOGGER.error("There is no associated <atlassian:application /> element for the Jira activity.");
            return null;
        }

        // Add the corresponding hashtag for the given application string
        if (application.equals("com.atlassian.fisheye")) {
            applicationHashtag = "#source";
        } else if (application.equals("com.atlassian.confluence")) {
            applicationHashtag = "#wiki";
        } else if (application.equals("com.atlassian.jira")) {
            applicationHashtag = "#issue";
        } else {
            LOGGER.error("Unable to handle Jira application type: " + application);
            return null;
        }

        // Let's gather the activity information
        org.apache.abdera.model.Element activityObjectElement = entry.getFirstChild(
                new QName("http://activitystrea.ms/spec/1.0/", "object", "activity"));

        // Just in case, fail if there is no activity:object.
        if (activityObjectElement == null) {
            LOGGER.error("There is no associated <activity:object /> element for the Jira activity.");
            return null;
        }

        org.apache.abdera.model.Element activityObjectTypeElement = activityObjectElement.getFirstChild(
                new QName("http://activitystrea.ms/spec/1.0/", "object-type", "activity"));

        // Just in case, fail if there is no activity:object > activity:object-type
        if (activityObjectTypeElement == null) {
            LOGGER.error("There is no associated <activity:object /> > <activity:object-type> element for " +
                    "the Jira activity.");
            return null;
        }

        String rawActivityType = activityObjectTypeElement.getText();

        activityHashtag = sanitizeJiraHashtag("#" + rawActivityType.substring(rawActivityType.lastIndexOf("/") + 1));

        // Let's gather the activity target information, if any
        org.apache.abdera.model.Element activityTargetElement = entry.getFirstChild(
                new QName("http://activitystrea.ms/spec/1.0/", "target", "activity"));

        if (activityTargetElement != null) {
            org.apache.abdera.model.Element activityTargetType =
                    activityTargetElement.getFirstChild(new QName("http://activitystrea.ms/spec/1.0/",
                            "object-type", "activity")); // Guaranteed to be there
            String rawTargetType = activityTargetType.getText();

            activityTargetHashtag = sanitizeJiraHashtag("#" + rawTargetType.substring(rawTargetType.lastIndexOf("/") +
                    1));
        }

        hashtags.add(applicationHashtag);
        hashtags.add(activityHashtag);

        if (activityTargetHashtag != null) {
            hashtags.add(activityTargetHashtag);
        }

        // Let's generate the autotags where applicable
        if (applicationHashtag.equals("#wiki")) {
            // For Confluence, attempt to get the page's labels and add them as hashtags
            handleJiraWikiAutotags(projectKey, activityObjectElement, entry, hashtags);
        } else if (applicationHashtag.equals("#issue") && !activityHashtag.equals("#review") &&
                (activityTargetHashtag == null || !activityTargetHashtag.equals("#review"))) {
            // For Jira, attempt to get the issue's type, priority and status and add them as hashtags
            //
            // Note: We do not generate autotags for Crucible (review) activity
            handleJiraIssueAutotags(projectKey, activityObjectElement, hashtags);
        }

        activityParts.put("title", title);
        activityParts.put("content", content);
        activityParts.put("rawContent", rawContent);
        activityParts.put("hashtags", hashtags);

        return activityParts;
    }

    public SOAPMessage invokeSoap(JiraStudioApp app, String soapBody) throws SOAPException {
        String cacheKey = (app + "-SOAP-" + soapBody.hashCode());
        Object objectFromCache = requestCache.getIfPresent(cacheKey);

        if (objectFromCache != null) {
            debugLog(LOGGER, "  (From cache)");
            return (SOAPMessage) objectFromCache;
        }

        // Wrap the SOAP body content in an envelope/body container
        StringBuilder sb = new StringBuilder();
        String soapBaseURL = getBaseUrl();
        String soapNamespaceURL;

        sb.append("<soapenv:Envelope ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ")
                .append("xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");

        switch (app) {
            case CONFLUENCE:
                soapNamespaceURL = "http://soap.rpc.confluence.atlassian.com";
                soapBaseURL += "/wiki/rpc/soap-axis/confluenceservice-v1";
                break;
            case JIRA:
                soapNamespaceURL = "http://soap.rpc.jira.atlassian.com";
                soapBaseURL += "/rpc/soap/jirasoapservice-v2";
                break;
            default:
                throw new SOAPException("Unknown Jira Studio application: " + app);
        }

        sb.append("xmlns:soap=\"" + soapNamespaceURL + "\">\n");
        sb.append("<soapenv:Body>\n");
        sb.append(soapBody);
        sb.append("</soapenv:Body></soapenv:Envelope>");

        String rawResponse;
        List<Header> requestHeaders = new ArrayList<Header>();

        requestHeaders.add(new BasicHeader("SOAPAction", ""));

        try {
            rawResponse = HTTPUtils.openUrl(soapBaseURL, "POST", sb.toString(), MediaType.TEXT_XML, null, null,
                    requestHeaders, null);
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to make SOAP call to %s: %s", soapBaseURL, e.getMessage()), e);
            throw new SOAPException(e);
        }

        Source response = new StreamSource(new StringReader(rawResponse));
        MessageFactory msgFactory = MessageFactory.newInstance();
        SOAPMessage message = msgFactory.createMessage();
        SOAPPart env = message.getSOAPPart();
        env.setContent(response);

        if (message.getSOAPBody().hasFault()) {
            SOAPFault fault = message.getSOAPBody().getFault();
            LOGGER.error("soap fault in jira soap response: " + fault.getFaultString());
        }

        requestCache.put(cacheKey, message);

        return message;
    }

    public List<Element> asList(NodeList nodeList) {
        ArrayList<Element> elements = new ArrayList<Element>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }
        return elements;
    }

    private String login(JiraStudioApp app) throws SOAPException {
        String username = getConnectionCredentials().getIdentity();

        debugLog(LOGGER, "Logging into " + (app == JiraStudioApp.CONFLUENCE ? "Confluence" : "Jira") +
                " via SOAP as " + username);

        StringBuilder sb = new StringBuilder()
                .append("  <soap:login soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("    <in0 xsi:type=\"xsd:string\">" + username + "</in0>\n")
                .append("    <in1 xsi:type=\"xsd:string\">" + getConnectionCredentials().getCredential() + "</in1>\n")
                .append("  </soap:login>\n");
        SOAPMessage loginResponse = invokeSoap(app, sb.toString());
        return loginResponse.getSOAPBody().getElementsByTagName("loginReturn").item(0)
                .getFirstChild().getNodeValue();
    }

    private boolean logout(JiraStudioApp app, String token) throws SOAPException {
        debugLog(LOGGER, "Logging out of " + (app == JiraStudioApp.CONFLUENCE ? "Confluence" : "Jira") +
                " via SOAP for " + getConnectionCredentials().getIdentity());

        StringBuilder sb = new StringBuilder()
                .append("  <soap:logout soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("    <in0 xsi:type=\"xsd:string\">" + token + "</in0>\n")
                .append("  </soap:logout>\n");
        SOAPMessage logoutResponse = invokeSoap(app, sb.toString());
        String logoutReturn = logoutResponse.getSOAPBody().getElementsByTagName("logoutReturn").item(0)
                .getFirstChild().getNodeValue();

        return Boolean.valueOf(logoutReturn);
    }

    // Jira SOAP calls

    public List<org.w3c.dom.Element> getSubTaskIssueTypes() throws SOAPException {
        debugLog(LOGGER, "Getting sub-task issue types");

        StringBuilder sb = new StringBuilder()
                .append("  <soap:getSubTaskIssueTypes soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("     <in0 xsi:type=\"xsd:string\">" + jiraToken + "</in0>\n")
                .append("  </soap:getSubTaskIssueTypes>\n");
        SOAPMessage subTaskIssueTypesResponse = invokeSoap(JiraStudioApp.JIRA, sb.toString());
        NodeList subTaskIssueTypes = subTaskIssueTypesResponse.getSOAPBody().getElementsByTagName("multiRef");
        return asList(subTaskIssueTypes);
    }

    public List<Element> getIssueTypes() throws SOAPException {
        debugLog(LOGGER, "Getting issue types");

        StringBuilder sb = new StringBuilder()
                .append("  <soap:getIssueTypes soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("     <in0 xsi:type=\"xsd:string\">" + jiraToken + "</in0>\n")
                .append("  </soap:getIssueTypes>\n");
        SOAPMessage issueTypesResponse = invokeSoap(JiraStudioApp.JIRA, sb.toString());
        NodeList issueTypes = issueTypesResponse.getSOAPBody().getElementsByTagName("multiRef");
        return asList(issueTypes);
    }

    public List<Element> getIssuePriorities() throws SOAPException {
        debugLog(LOGGER, "Getting issue priorities");

        StringBuilder sb = new StringBuilder()
                .append("  <soap:getPriorities soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("     <in0 xsi:type=\"xsd:string\">" + jiraToken + "</in0>\n")
                .append("  </soap:getPriorities>\n");
        SOAPMessage issuePrioritiesResponse = invokeSoap(JiraStudioApp.JIRA, sb.toString());
        NodeList issuePriorities = issuePrioritiesResponse.getSOAPBody().getElementsByTagName("multiRef");
        return asList(issuePriorities);
    }

    public List<Element> getIssueStatuses() throws SOAPException {
        debugLog(LOGGER, "Getting issue statuses");

        StringBuilder sb = new StringBuilder()
                .append("  <soap:getStatuses soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("     <in0 xsi:type=\"xsd:string\">" + jiraToken + "</in0>\n")
                .append("  </soap:getStatuses>\n");
        SOAPMessage issueStatusesResponse = invokeSoap(JiraStudioApp.JIRA, sb.toString());
        NodeList issueStatuses = issueStatusesResponse.getSOAPBody().getElementsByTagName("multiRef");
        return asList(issueStatuses);
    }

    public String createIssue(ProjectHostingIssue issue) throws SOAPException {
        debugLog(LOGGER, "Creating issue");

        StringBuilder sb = new StringBuilder()
                .append("  <soap:createIssue soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("    <in0 xsi:type=\"xsd:string\">" + jiraToken + "</in0>\n")
                .append("    <in1 xsi:type=\"bean:RemoteIssue\" xmlns:bean=\"" + jiraBeanSchema + "\">\n")
                .append("      <description xsi:type=\"xsd:string\">" + issue.getDescription() + "</description>\n")
                .append("      <project xsi:type=\"xsd:string\">" + issue.getProject() + "</project>\n")
                .append("      <summary xsi:type=\"xsd:string\">" + issue.getSummary() + "</summary>\n")
                .append("      <type xsi:type=\"xsd:string\">" + issue.getType() + "</type>\n")
                .append("    </in1>\n")
                .append("  </soap:createIssue>\n");
        SOAPMessage createIssueResponse = invokeSoap(JiraStudioApp.JIRA, sb.toString());

        return createIssueResponse.getSOAPBody().getElementsByTagName("id").item(0)
                .getFirstChild().getNodeValue();
    }

    public List<Element> getIssueDetails(String issue) throws SOAPException {
        debugLog(LOGGER, "Getting issue details for " + issue);

        StringBuilder sb = new StringBuilder()
                .append("  <soap:getIssue soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("     <in0 xsi:type=\"xsd:string\">" + jiraToken + "</in0>\n")
                .append("     <in1 xsi:type=\"xsd:string\">" + issue + "</in1>\n")
                .append("  </soap:getIssue>\n");
        SOAPMessage issueDetailsResponse = invokeSoap(JiraStudioApp.JIRA, sb.toString());
        NodeList issueDetails = issueDetailsResponse.getSOAPBody().getElementsByTagName("multiRef");
        return asList(issueDetails);
    }

    // Confluence SOAP calls

    public List<Element> getWikiPageLabels(long pageId) throws SOAPException {
        debugLog(LOGGER, "Getting labels for wiki page " + pageId);
        StringBuilder sb = new StringBuilder()
                .append("  <soap:getLabelsById soapenv:encodingStyle=\"" + encodingStyle + "\">\n")
                .append("    <in0 xsi:type=\"xsd:string\">" + confluenceToken + "</in0>\n")
                .append("    <in1 xsi:type=\"xsd:long\">" + pageId + "</in1>\n")
                .append("  </soap:getLabelsById>\n");
        SOAPMessage labelsResponse = invokeSoap(JiraStudioApp.CONFLUENCE, sb.toString());
        NodeList labels = labelsResponse.getSOAPBody().getElementsByTagName("getLabelsByIdReturn");
        return asList(labels);
    }

    /**
     * Returns the project key based on the activity:object element.
     *
     * @param activityObjectElement the activity:object element
     * @param monitoredProjectKeys  the set of project keys monitored in Nodeable
     * @return the project key or null if one couldn't be found
     */
    public String getProjectKeyOfEntry(org.apache.abdera.model.Element activityObjectElement,
                                       Set<String> monitoredProjectKeys) {

        // not sure how/why this would happen, but logs indicate it does
        if (activityObjectElement == null) {
            LOGGER.error("activityObjectElement is null");
            return null;
        }

        QName linkQname = new QName("http://www.w3.org/2005/Atom", "link");
        org.apache.abdera.model.Element linkElement = activityObjectElement.getFirstChild(linkQname);

        if (linkElement == null) {
            LOGGER.error("Unable to parse the linkElement of the Jira activity:object " +
                    JSONUtils.xmlToJSON(activityObjectElement.toString()));
            return null;
        }

        URL href = getURLFromLinkElement(linkElement);

        // the returned href may be null
        if (href == null) {
            LOGGER.error("Unable to parse the link of the Jira activity:object for: " +
                    JSONUtils.xmlToJSON(activityObjectElement.toString()));
            return null;
        }

        String hrefAsString = href.toString();
        String[] pathParts = href.getPath().split("/");
        String projectKey = getProjectKeyFromId(pathParts[pathParts.length - 1]);

        if (!monitoredProjectKeys.contains(projectKey)) {
            // Since the activity:object > link didn't give us the project key based on the logic
            // above (last path segment is the project key), lets try to see if we can adjust our
            // logic for the special cases as the approach above is the norm.

            if (hrefAsString.contains("/wiki/display/")) {
                // This is specialized logic to handle wiki comment activity
                projectKey = hrefAsString.split("/wiki/display/")[1].split("/")[0];
            } else if (hrefAsString.contains("/wiki/download/")) {
                // This is specialized logic to handle wiki attachment activity
                linkElement = ((Entry) activityObjectElement.getParentElement()).getFirstChild(linkQname);
                href = getURLFromLinkElement(linkElement);
                hrefAsString = href.toString();
                projectKey = hrefAsString.split("/wiki/display/")[1].split("/")[0];
            } else {
                // This is specialized logic to handle new/updated/deleted wiki
                org.apache.abdera.model.Element activityTargetElement =
                        ((Entry) activityObjectElement.getParentElement()).getFirstChild(
                                new QName("http://activitystrea.ms/spec/1.0/", "target", "activity"));

                if (activityTargetElement != null) {
                    linkElement = activityTargetElement.getFirstChild(linkQname);
                    href = getURLFromLinkElement(linkElement);
                    pathParts = href.getPath().split("/");
                    projectKey = pathParts[pathParts.length - 1];
                }
            }

            projectKey = getProjectKeyFromId(projectKey);

            if (!monitoredProjectKeys.contains(projectKey)) {
                // Revert back to null
                projectKey = null;
            }
        }

        return projectKey;
    }

    /**
     * Returns the URL object from the link element
     *
     * @param linkElement the link element
     * @return a URL for the href attribute of the link element or null if there was problem
     */
    private URL getURLFromLinkElement(org.apache.abdera.model.Element linkElement) {
        // Just in case, return null if there is no link to parse
        if (linkElement == null || !linkElement.getQName().getLocalPart().equals("link")) {
            return null;
        }

        String linkValue = linkElement.getAttributeValue("href");

        try {
            return URI.create(linkValue).toURL();
        } catch (MalformedURLException e) {
            // Should never happen
            LOGGER.error(String.format("Error creating a URL for %s: %s", linkValue, e.getMessage()), e);
            return null;
        }
    }

    /**
     * Returns the project key based on the passed in id.
     *
     * @param id the potential project id
     * @return the project key
     */
    private String getProjectKeyFromId(String id) {
        String projectKey = id;

        // Handle Jira issue ids
        if (id.contains("-")) {
            projectKey = projectKey.substring(0, projectKey.lastIndexOf("-"));
        }

        // Handle Crucible ids
        if (projectKey.contains("-")) {
            projectKey = projectKey.substring(projectKey.indexOf("-") + 1);
        }

        return projectKey;
    }

    /**
     * Takes a list of elements, each having an id and name child element, and finds the name for the corresponding
     * id passed in.
     *
     * @param elements the elements to search for the corresponding id
     * @param id       the id we're interested in finding the name for
     * @return the name or null if one could not be found
     */
    private String getNameForId(List<org.w3c.dom.Element> elements, String id) {
        String name = null;

        for (org.w3c.dom.Element element : elements) {
            NodeList idNodes = element.getElementsByTagName("id");

            if (idNodes == null || idNodes.getLength() == 0) {
                // Nothing we can do so just return null so the caller can log
                return null;
            }

            if (idNodes.item(0).getTextContent().equals(id)) {
                NodeList nameNodes = element.getElementsByTagName("name");

                if (nameNodes == null || nameNodes.getLength() == 0) {
                    // Nothing we can do so just return null so the caller can log
                    return null;
                }

                name = nameNodes.item(0).getTextContent();
                break;
            }
        }

        return name;
    }

    /**
     * The way we're getting hashtags from the Jira Activity Stream means we sometimes get values from Jira that we'd
     * rather replace with something more meaningful.  Below are the cases we'll be handling:
     * <p/>
     * * article   : This corresponds with a blog entry and so we'll return blog
     * * file      : This corresponds with an attachment and so we'll return attachment
     * * repository: This corresponds with a source activity and we're already using #source
     * * space     : This corresponds with a wiki activity and we're using #wiki
     * <p/>
     * This method also removes all illegal characters.
     *
     * @param hashtag the hashtag to sanitize
     * @return the sanitized hashtag or the original hashtag if we do not sanitize the hashtag passed in
     */
    private String sanitizeJiraHashtag(String hashtag) {
        if (hashtag.equals("#article")) {
            return "#blog";
        } else if (hashtag.equals("#file")) {
            return "#attachment";
        } else if (hashtag.equals("#repository")) {
            return "#source";
        } else if (hashtag.equals("#space")) {
            return "#wiki";
        }

        return removeIllegalHashtagCharacters(hashtag);
    }

    /**
     * Removes illegal characters from hashtags.
     *
     * @param hashtag the hashtag to cleanup
     * @return the cleaned up hashtag
     */
    private String removeIllegalHashtagCharacters(String hashtag) {
        String regex = "[^a-zA-Z0-9._\\-]";
        int start = 0;

        // Figure out the first character that is not a #
        for (int i = 0; i < hashtag.length(); i++) {
            if (hashtag.charAt(i) != '#') {
                start = i;
                break;
            }
        }

        return hashtag.substring(0, start) + hashtag.substring(start).replaceAll(regex, "");
    }

    /**
     * Handles the auto-tags, extra metadata, to be added to the activity message for Jira issue activity.
     *
     * @param projectKey     the project key of the inventory item
     * @param activityObject the raw <activity:object /> element of the activity
     * @param entry          the root element for the activity entry
     * @param hashtags       the hashtags set we will manipulate
     */
    private void handleJiraWikiAutotags(String projectKey, org.apache.abdera.model.Element activityObject,
                                        Entry entry, Set<String> hashtags) {
        // Right now, the only additional auto-tags we add for wiki activity are the page's labels.  To do this, we
        // parse the page id from ref attribute of the <thr:in-reply-to /> element.  Once we get this, we make a SOAP
        // call to get the page labels for that id.
        //
        // Note: This does not appear to work for blog/article entries.  The pageId we parse from the activity entry
        //       always returns an empty array response when gathering the labels.  We will still attempt to retrieve
        //       the labels for blog/article entries just in case it gets fixed.
        org.apache.abdera.model.Element thr = entry.getFirstChild(new QName("http://purl.org/syndication/thread/1.0",
                "in-reply-to", "thr"));

        if (thr != null) {
            String pageUrl = thr.getAttributeValue("ref");
            long pageId;

            // Just in case
            if (pageUrl == null) {
                LOGGER.error("Unable to parse the thr:in-reply-to of the Jira activity:object for: " +
                        JSONUtils.xmlToJSON(activityObject.toString()));
                return;
            }

            try {
                pageId = Long.valueOf(pageUrl.substring(pageUrl.lastIndexOf("/") + 1));
            } catch (NumberFormatException e) {
                // Not much we can do so log the error and continue processing
                LOGGER.error("Unexpected wiki page id when parsing activity URL (" + pageUrl + "): ", e.getMessage());
                return;
            }

            try {
                List<org.w3c.dom.Element> labels = getWikiPageLabels(pageId);

                for (org.w3c.dom.Element label : labels) {
                    // Confluence labels are returned as an array and we're only interested in ones that have children.
                    //
                    // Note: When a page has no labels, we get back an empty array element and so we need to check that
                    //       the label has children before trying to use it.
                    if (!label.hasChildNodes()) {
                        continue;
                    }

                    NodeList labelNames = label.getElementsByTagName("name");

                    if (labelNames == null) {
                        // Not much we can do at this point but log the problem
                        LOGGER.error("Unexpected response when retrieving labels for wiki page with an " +
                                "id of " + pageId + " in the " + projectKey + " project.");
                    } else {
                        hashtags.add(removeIllegalHashtagCharacters("#" + labelNames.item(0).getTextContent()));
                    }
                }
            } catch (SOAPException e) {
                // Not much we can do at this point but log the problem
                LOGGER.error("Unable to make SOAP call to get wiki labels for " + projectKey +
                        ": " + e.getMessage());
            }
        }
    }

    /**
     * Handles the auto-tags, extra metadata, to be added to the activity message for Jira issue activity.
     *
     * @param projectKey     the project key of the invenentory item
     * @param activityObject the raw <activity:object /> element of the activity
     * @param hashtags       the hashtags set we will manipulate
     */
    private void handleJiraIssueAutotags(String projectKey, org.apache.abdera.model.Element activityObject,
                                         Set<String> hashtags) {

        // Right now, the only additional auto-tags we add for issue activity are the issue's type, status and
        // priority.  To do this, we parse the first href attribute of the first <link /> element of the
        // <activity:object /> tag.  Once we get the issue name, we get the issues details with a SOAP call.  Once we
        // get the issue's details, we get the integer id for the issue type, status and priority.  We then have to
        // make three subsequent calls to get the list of issue types, issue statuses and issue priorities.  Once those
        // are available, we reference the integer (id) value for the issue's type, status and priority against the
        // list to get the display name for the issue type, status and priority.
        org.apache.abdera.model.Element linkElement = ((Entry) activityObject.getParentElement()).getFirstChild(
                new QName("http://www.w3.org/2005/Atom", "link"));

        // Just in case
        if (linkElement == null) {
            LOGGER.error("Unable to find the link of the Jira activity entry for: " +
                    JSONUtils.xmlToJSON(activityObject.toString()));
            return;
        }

        URL activityLink = getURLFromLinkElement(linkElement);

        // Just in case
        if (activityLink == null) {
            LOGGER.error("Unable to parse the link of the Jira activity:object for: " +
                    JSONUtils.xmlToJSON(activityObject.toString()));
            return;
        }

        String[] pathParts = activityLink.getPath().split("/");
        String issueId = pathParts[pathParts.length - 1];

        if (!issueId.contains("-")) {
            // Not much we can do at this point but log the problem
            LOGGER.error("Unable to get the Jira issue id from the following url: " + activityLink.toString());
            return;
        }

        try {
            List<org.w3c.dom.Element> allIssueDetails = getIssueDetails(issueId);

            if (allIssueDetails == null || allIssueDetails.size() == 0) {
                // Not much we can do at this point but log the problem
                LOGGER.error("Unexpected response when retrieving Jira issue details for " + issueId +
                        " in the " + projectKey + " project.");
                return;
            }

            // Our DOM parsing code is verbose, not my fault, but also very cautious.

            // Retrieve the issue details
            org.w3c.dom.Element issueDetails = allIssueDetails.get(0);
            NodeList issueDetailsPriorityNodes = issueDetails.getElementsByTagName("priority");
            NodeList issueDetailsStatusNodes = issueDetails.getElementsByTagName("status");
            NodeList issueDetailsTypeNodes = issueDetails.getElementsByTagName("type");

            // Validate the responses
            if (issueDetailsPriorityNodes == null || issueDetailsPriorityNodes.getLength() == 0 ||
                    issueDetailsStatusNodes == null || issueDetailsStatusNodes.getLength() == 0 ||
                    issueDetailsTypeNodes == null || issueDetailsTypeNodes.getLength() == 0) {
                // Not much we can do at this point but log the problem
                LOGGER.error("Unexpected response when retrieving Jira issue detail priority/status/type for " +
                        issueId + " in the " + projectKey + " project.");
                return;
            }

            // These represent the actual low-level identifiers for issue priority, status and type.  These will
            // be compared against the priorites, statuses and types below.
            String issueDetailsPriority = issueDetailsPriorityNodes.item(0).getTextContent();
            String issueDetailsStatus = issueDetailsStatusNodes.item(0).getTextContent();
            String issueDetailsType = issueDetailsTypeNodes.item(0).getTextContent();

            // Retrieve the issue priorities, statuses and types.
            List<org.w3c.dom.Element> issuePriorities = getIssuePriorities();
            List<org.w3c.dom.Element> issueStatuses = getIssueStatuses();
            List<org.w3c.dom.Element> issueTypes = getIssueTypes();
            List<org.w3c.dom.Element> subTaskIssueTypes = getSubTaskIssueTypes();

            // Validate the responses
            if (issuePriorities == null || issuePriorities.size() == 0 ||
                    issueStatuses == null || issueStatuses.size() == 0 ||
                    issueTypes == null || issueTypes.size() == 0 ||
                    subTaskIssueTypes == null) {
                // Not much we can do at this point but log the problem
                LOGGER.error("Unexpected response when retrieving Jira issue priority/status/type for " +
                        issueId + " in the " + projectKey + " project.");
                return;
            }

            // Get the priority and add the hashtag
            String issuePriorityName = getNameForId(issuePriorities, issueDetailsPriority);
            String issueStatusName = getNameForId(issueStatuses, issueDetailsStatus);
            String issueTypeName = getNameForId(issueTypes, issueDetailsType);
            String subTaskIssueTypeName = getNameForId(subTaskIssueTypes, issueDetailsType);

            if (issueTypeName == null && subTaskIssueTypeName == null) {
                // Not much we can do at this point but log the problem
                LOGGER.error("Unable to get the issue type name for id " + issueDetailsType +
                        " of the " + issueId + " issue in the " + projectKey + " project.");
            } else {
                hashtags.add(removeIllegalHashtagCharacters("#" +
                        (issueTypeName != null ? issueTypeName.toLowerCase() : subTaskIssueTypeName.toLowerCase())));
            }

            if (issuePriorityName == null) {
                // Not much we can do at this point but log the problem
                LOGGER.error("Unable to get the issue priority name for id " + issueDetailsPriority +
                        " of the " + issueId + " issue in the " + projectKey + " project.");
            } else {
                hashtags.add(removeIllegalHashtagCharacters("#" + issuePriorityName.toLowerCase()));
            }

            if (issueStatusName == null) {
                // Not much we can do at this point but log the problem
                LOGGER.error("Unable to get the issue status name for id " + issueDetailsStatus +
                        " of the " + issueId + " issue in the " + projectKey + " project.");
            } else {
                hashtags.add(removeIllegalHashtagCharacters("#" + issueStatusName.toLowerCase()));
            }

            // Would be nice to get the labels on the issue but there doesn't appear to be an API to retrieve them
        } catch (SOAPException e) {
            LOGGER.error("Unable to get the issue details for " + issueId + " in the " +
                    projectKey + " project.");
        }
    }

    /**
     * Makes a call to Jira via REST.
     *
     * @param url        the Jira REST URL to make a call to
     * @param maxResults the maximum number of results to return
     * @param useCache   specifies whether or not to use cache
     * @param anonymous  specifies whether or not to make the request anonymously
     * @return a list of JSONObjects or an empty list if the response had no content
     * @throws InvalidCredentialsException if the connection associated with this client has invalid credentials
     * @throws IOException                 if anything goes wrong making the actual request
     */
    @SuppressWarnings("unchecked")
    private List<JSONObject> makeRESTRequest(String url, int maxResults, boolean useCache, boolean anonymous)
            throws InvalidCredentialsException, IOException {
        // Caching in Jira is a little harder than in GitHub because we have to make anonymous calls sometimes.
        // That being said, our cache key is the username-url instead of just the URL.
        String cacheKey = (getConnectionCredentials().getIdentity() + "-" + url);
        Object objectFromCache = (useCache ? requestCache.getIfPresent(cacheKey) : null);
        List<JSONObject> response = (objectFromCache != null ? (List<JSONObject>) objectFromCache : null);
        String rUsername = (anonymous ? null : getConnectionCredentials().getIdentity());
        String rPassword = (anonymous ? null : getConnectionCredentials().getCredential());

        // Quick return if there was an entry in the cache
        if (response != null) {
            debugLog(LOGGER, "  (From cache)");
            return response;
        } else {
            response = new ArrayList<JSONObject>();
        }

        JSONArray rawResponse = new JSONArray();

        try {
            // Try to parse as a JSONArray knowing that it might be a JSONObject
            rawResponse = JSONArray.fromObject(HTTPUtils.openUrl(url, HttpMethod.GET, null,
                    MediaType.APPLICATION_JSON,
                    rUsername, rPassword, null, null));
        } catch (JSONException e) {
            try {
                // Try to parse as a JSONObject
                JSONObject rawObject = JSONObject.fromObject(HTTPUtils.openUrl(url, HttpMethod.GET, null,
                        MediaType.APPLICATION_JSON,
                        rUsername, rPassword, null, null));

                rawResponse.add(rawObject);
            } catch (JSONException e2) {
                // Fail
                return null;
            }
        }

        for (Object anArrayResponse : rawResponse) {
            if (response.size() < maxResults) {
                response.add(JSONObject.fromObject(anArrayResponse));
            }

            if (response.size() == maxResults) {
                break;
            }
        }

        if (useCache) {
            requestCache.put(cacheKey, response);
        }

        return response;
    }

    private String concatRawTitleAndContent(Entry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.getTitle() != null) {
            sb.append(entry.getTitle());
        }
        if (entry.getContent() != null) {
            sb.append(entry.getContent());
        }
        return sb.toString();
    }
}
