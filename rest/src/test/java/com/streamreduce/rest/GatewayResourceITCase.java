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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBList;
import com.mongodb.util.JSON;
import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.service.EventService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.connections.CustomProvider;
import com.streamreduce.connections.GatewayProvider;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.resource.ErrorMessage;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GatewayResourceITCase extends AbstractInContainerTestCase {

    @Autowired
    private EventService eventService;
    @Autowired
    private InventoryService inventoryService;

    private Account account;
    private ConnectionResponseDTO connection;
    private String apiKey;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        String authnToken = login(testUsername, testUsername);
        JSONObject json = new JSONObject();

        json.put("providerId", ProviderIdConstants.CUSTOM_PROVIDER_ID);
        json.put("type", CustomProvider.TYPE);
        json.put("alias", "Test Generic Connection");
        json.put("authType", AuthType.API_KEY);

        try {
            // Create a new IMG connection
            connection = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                                      TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));

            // Get the IMG connection API key
            apiKey = connection.getIdentity();

            // Validate the connection was created
            Connection cConnection = applicationManager.getSecurityService().getByApiKey(apiKey, GatewayProvider.TYPE);

            assertNotNull(cConnection);

            account = cConnection.getAccount();

            // Enable inbound IMG support
            account.setConfigValue(Account.ConfigKey.DISABLE_INBOUND_API, false);
            applicationManager.getUserService().updateAccount(account);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (connection != null) {
            applicationManager.getConnectionService().deleteConnection(
                    applicationManager.getConnectionService().getConnection(connection.getId()));
        }
        super.tearDown();
    }

    @Test
    @Ignore
    public void testGMGInvalidAuth() throws Exception {
        assertTrue(makeRequest(imgBaseUrl, "POST", new JSONObject(), "some_bogus_key", AuthTokenType.GATEWAY)
                           .contains("Authentication failed"));
    }

    @Test
    @Ignore
    public void testGMGInboundDisabled() throws Exception {
        // Disable inbound IMG support
        account.setConfigValue(Account.ConfigKey.DISABLE_INBOUND_API, true);
        applicationManager.getUserService().updateAccount(account);

        ErrorMessage error = jsonToObject(makeRequest(imgBaseUrl, "POST", new JSONObject(), apiKey,
                                                      AuthTokenType.GATEWAY),
                                          TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage().equals("This account is not provisioned for inbound payloads, please " +
                                                          "contact support@nodeable.com."));
    }

    @Test
    @Ignore
    public void testInvalidGMGPayloads() throws Exception {
        ErrorMessage error = jsonToObject(makeRequest(imgBaseUrl, "POST", new JSONObject(), apiKey,
                                                      AuthTokenType.GATEWAY),
                                          TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        // Invalid message because the message or metrics attributes were missing
        assertTrue(error.getErrorMessage().equals("You must supply at least a 'message' or 'metrics' attribute in " +
                                                          "the payload."));

        // Invalid message because hashtags is a non-array
        JSONObject json = new JSONObject();

        json.put("message", "This message doesn't matter.");
        json.put("hashtags", 12345);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage().equals("'hashtags' must be a string array."));

        // Invalid message because hashtags is an array with non-string values
        JSONArray jsonArray = new JSONArray();
        json = new JSONObject();

        jsonArray.add(12345);

        json.put("message", "This message doesn't matter.");
        json.put("hashtags", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage()
                        .equals("All hashtags specified in the 'hashtags' attribute must be strings."));

        // Invalid message because metrics is a non-array
        json = new JSONObject();

        json.put("message", "This message doesn't matter.");
        json.put("metrics", 12345);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage().equals("'metrics' must be an object array."));

        // Invalid message because metrics is an array with non-object values
        json = new JSONObject();
        jsonArray = new JSONArray();

        jsonArray.add(12345);

        json.put("message", "This message doesn't matter.");
        json.put("metrics", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage()
                        .equals("All metrics specified in the 'metrics' attribute must be objects."));

        // Invalid message because metric in metrics doesn't contain name
        JSONObject metric = new JSONObject();
        json = new JSONObject();
        jsonArray = new JSONArray();

        jsonArray.add(metric);

        json.put("message", "This message doesn't matter.");
        json.put("metrics", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage()
                        .equals("'name' is required for each metric in the 'metrics' attribute."));

        // Invalid message because metric in metrics doesn't contain type
        metric = new JSONObject();
        json = new JSONObject();
        jsonArray = new JSONArray();

        metric.put("name", "This name doesn't matter.");

        jsonArray.add(metric);

        json.put("message", "This message doesn't matter.");
        json.put("metrics", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage()
                        .equals("'type' is required for each metric in the 'metrics' attribute and must be " +
                                        "either 'ABSOLUTE' or 'DELTA'."));

        // Invalid message because metric in metrics doesn't contain valid type
        metric = new JSONObject();
        json = new JSONObject();
        jsonArray = new JSONArray();

        metric.put("name", "This name doesn't matter.");
        metric.put("type", "FAKE");

        jsonArray.add(metric);

        json.put("message", "This message doesn't matter.");
        json.put("metrics", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage()
                        .equals("'type' is required for each metric in the 'metrics' attribute and must be " +
                                        "either 'ABSOLUTE' or 'DELTA'."));

        // Invalid message because metric in metrics doesn't contain value
        metric = new JSONObject();
        json = new JSONObject();
        jsonArray = new JSONArray();

        metric.put("name", "This name doesn't matter.");
        metric.put("type", "ABSOLUTE");

        jsonArray.add(metric);

        json.put("message", "This message doesn't matter.");
        json.put("metrics", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage()
                        .equals("'value' is required for each metric in the 'metrics' attribute and must " +
                                        "be a valid numerical value."));

        // Invalid message because metric in metrics doesn't contain valid value
        metric = new JSONObject();
        json = new JSONObject();
        jsonArray = new JSONArray();

        metric.put("name", "This name doesn't matter.");
        metric.put("type", "ABSOLUTE");
        metric.put("value", "fake");

        jsonArray.add(metric);

        json.put("message", "This message doesn't matter.");
        json.put("metrics", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage()
                        .equals("'value' for each metric in the 'metrics' attribute must be a numerical value."));

        // Invalid because it's a multi-entry request but the data attribute isn't a JSONArray
        json = new JSONObject();

        json.put("data", "This should be a JSONArray.");

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage().equals("'data' must be an object array."));

        // Invalid because it's a multi-entry request but the data entry isn't an object
        json = new JSONObject();
        jsonArray = new JSONArray();

        jsonArray.add("This should be a JSONObject.");
        json.put("data", jsonArray);

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage().equals("Every object in the 'data' array should be an object."));

        // Invalid because dateGenerated is not a number
        json = new JSONObject();

        json.put("message", "This is a test message.");
        json.put("dateGenerated", "This should be a number.");

        error = jsonToObject(makeRequest(imgBaseUrl, "POST", json, apiKey,
                                         AuthTokenType.GATEWAY),
                             TypeFactory.defaultInstance().constructType(ErrorMessage.class));

        assertTrue(error.getErrorMessage().equals("'dateGenerated' must be an number."));
    }

    @Test
    @Ignore
    public void testSuccessfulConnectionMessages() throws Exception {
        List<Event> events = getEventsForTarget(connection.getId());
        JSONObject json = new JSONObject();
        int originalEventCount = events.size();

        json.put("message", "This is a test message.");

        assertEquals("201", makeRequest(imgBaseUrl, "POST", json, apiKey, AuthTokenType.GATEWAY));

        events = getEventsForTarget(connection.getId());

        assertEquals(originalEventCount + 1, events.size());

        Event event = events.get(events.size() - 1);

        validateEvent(event, json);

        json = new JSONObject();

        json.put("message", "This is another test message.");
        json.put("hashtags", ImmutableSet.of("#testing", "#test"));
        json.put("superfluous", "This is a superfluous property.");

        JSONArray metrics = new JSONArray();

        for (int i = 0; i < 2; i++) {
            JSONObject metric = new JSONObject();

            metric.put("name", "Test Metric" + (i + 1));
            metric.put("type", "ABSOLUTE");
            metric.put("value", (i + 1));

            metrics.add(metric);
        }

        json.put("metrics", metrics);

        assertEquals("201", makeRequest(imgBaseUrl, "POST", json, apiKey, AuthTokenType.GATEWAY));

        events = getEventsForTarget(connection.getId());
        event = events.get(events.size() - 1);

        assertEquals(originalEventCount + 2, events.size());

        validateEvent(event, json);
    }

    @Test
    @Ignore
    public void testSuccessfulInventoryItemMessages() throws Exception {
        List<InventoryItem> inventoryItems = inventoryService.getInventoryItems(connection.getId());
        int connectionEventCount = getEventsForTarget(connection.getId()).size();
        JSONObject json = new JSONObject();
        List<Event> events;
        InventoryItem inventoryItem;
        int originalEventCount;

        assertEquals(0, inventoryItems.size());

        json.put("message", "Creating a new IMG inventory item.");
        json.put("inventoryItemId", "testNodeId");

        assertEquals("201", makeRequest(imgBaseUrl, "POST", json, apiKey, AuthTokenType.GATEWAY));

        inventoryItems = inventoryService.getInventoryItems(connection.getId());

        // Make sure the connection isn't receiving the event
        assertEquals(connectionEventCount, getEventsForTarget(connection.getId()).size());
        // Make sure the inventory item was created
        assertEquals(1, inventoryItems.size());

        inventoryItem = inventoryItems.get(0);
        events = getEventsForTarget(inventoryItem.getId());
        originalEventCount = events.size();

        validateEvent(events.get(events.size() - 1), json);

        json = new JSONObject();

        json.put("inventoryItemId", "testNodeId");
        json.put("hashtags", ImmutableSet.of("#testing", "#test"));
        json.put("superfluous", "This is a superfluous property.");

        JSONArray metrics = new JSONArray();

        for (int i = 0; i < 2; i++) {
            JSONObject metric = new JSONObject();

            metric.put("name", "Test Metric" + (i + 1));
            metric.put("type", "DELTA");
            metric.put("value", (i + 1));

            metrics.add(metric);
        }

        json.put("metrics", metrics);

        assertEquals("201", makeRequest(imgBaseUrl, "POST", json, apiKey, AuthTokenType.GATEWAY));

        events = getEventsForTarget(inventoryItem.getId());

        validateEvent(events.get(events.size() - 1), json);

        // Make sure the connection isn't receiving the event
        assertEquals(connectionEventCount, getEventsForTarget(connection.getId()).size());
        // Make sure the inventory item event count incremented by two (read of inventory item and new message/metrics)
        assertEquals(originalEventCount + 2, events.size());
    }

    @Test
    @Ignore
    public void testMultipleMessagesInOneRequest() throws Exception {
        int originalEventCount = getEventsForTarget(connection.getId()).size();
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < 2; i++) {
            JSONObject message = new JSONObject();

            message.put("message", "Test message " + (i + 1) + ".");
            message.put("dateGenerated", new Date().getTime());

            jsonArray.add(message);
        }

        json.put("data", jsonArray);

        assertEquals("201", makeRequest(imgBaseUrl, "POST", json, apiKey, AuthTokenType.GATEWAY));

        List<Event> events = getEventsForTarget(connection.getId());

        assertEquals(originalEventCount + 2, events.size());
    }

    private void validateEvent(Event event, JSONObject json) {
        Map<String, Object> eventMetadata = event.getMetadata();

        // Validate the hashtags
        if (json.containsKey("hashtags")) {
            BasicDBList targetHashtags = (BasicDBList)eventMetadata.get("targetHashtags");
            JSONArray messageHashtags = json.getJSONArray("hashtags");
            Set<String> expectedHashtags = new HashSet<String>();

            for (Object rawHashtag : targetHashtags) {
                expectedHashtags.add(rawHashtag.toString());
            }

            // Account for the inherited '#custom' hashtag from the target
            assertEquals(messageHashtags.size() + 1, targetHashtags.size());

            // Ensure all message hashtags are in the targetHashtags
            for (Object rawHashtag : messageHashtags) {
                assertTrue(expectedHashtags.contains(rawHashtag.toString()));
            }
        }

        // Validate the payload
        assertEquals(JSON.parse(json.toString()).toString(), eventMetadata.get("payload").toString());
    }

    /**
     * Returns all events for the given target object id.
     *
     * @param targetId the target object id
     *
     * @return the list of events
     */
    private List<Event> getEventsForTarget(final ObjectId targetId) {
        return Lists.newArrayList(Iterables.filter(eventService.getEventsForAccount(testAccount),
                                                   new Predicate<Event>() {
                                                       @Override
                                                       public boolean apply(Event event) {
                                                           return event.getTargetId().equals(targetId);
                                                       }
                                                   }));
    }

}
