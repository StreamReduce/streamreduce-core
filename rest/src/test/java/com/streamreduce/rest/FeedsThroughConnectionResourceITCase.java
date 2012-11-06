package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.connections.FeedProvider;
import com.streamreduce.feed.types.FeedType;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FeedsThroughConnectionResourceITCase extends AbstractInContainerTestCase {

    private String authnToken = null;
    private ConnectionResponseDTO feed;

    /**
     * {@inheritDoc}
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        authnToken = login(testUsername, testUsername);
    }

    @Test
    @Ignore
    public void testCreateFeed() {
        try {
            JSONObject createConnectionPayload = createJsonPostBody();
            String createConnResponse = makeRequest(connectionsBaseUrl, "POST", createConnectionPayload, authnToken);
            feed = jsonToObject(createConnResponse,
                    TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void testCreateFeedWithNullIdentityAndCredential() {
        try {
            JSONObject createConnectionPayload = createJsonPostBody();
            JSONObject credentials = new JSONObject();
            credentials.put("identity",null);
            credentials.put("credential",null);
            createConnectionPayload.put("credentials",credentials);
            String createConnResponse = makeRequest(connectionsBaseUrl, "POST", createConnectionPayload, authnToken);
            feed = jsonToObject(createConnResponse,
                    TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void testInventoryForFeedReturnsEmptyList200() {
        try {
            JSONObject createConnectionPayload = createJsonPostBody();
            String createConnResponse = makeRequest(connectionsBaseUrl, "POST", createConnectionPayload, authnToken);
            feed = jsonToObject(createConnResponse,
                    TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));
            String id = feed.getId().toString();
            String inventoryUrlForConnection = connectionsBaseUrl + "/" + id + "/" + "inventory";
            String inventoryResponse = makeRequest(inventoryUrlForConnection, "GET", null, authnToken);
            assertEquals("Response should be an empty list", "[]", inventoryResponse);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }


    @Test
    @Ignore
    public void receiveFeedMessagesThroughRest() {
        try {
            JSONObject createConnectionPayload = createJsonPostBody();
            String createConnResponse = makeRequest(connectionsBaseUrl, "POST", createConnectionPayload, authnToken);
            feed = jsonToObject(createConnResponse,
                    TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

            String messageUrlWithSearch = messagesBaseUrl + "?search=sample";
            String messagesResponse = makeRequest(messageUrlWithSearch, "GET", null, authnToken);
            logger.debug("messagesResponse is:");
            logger.debug(messagesResponse);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    private JSONObject createJsonPostBody() {
        JSONObject json = new JSONObject();
        json.put("providerId", FeedType.RSS.toString());
        json.put("type", FeedProvider.TYPE);
        json.put("url", "http://status.aws.amazon.com/rss/EC2NoCal.rss?foo");
        json.put("alias", "sample");
        json.put("authType","NONE");
        return json;
    }
}
