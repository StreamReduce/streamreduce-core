package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.core.model.Connection;
import com.streamreduce.connections.FeedProvider;
import com.streamreduce.feed.types.FeedType;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;

import net.sf.json.JSONObject;

import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ValidationResourceITCase extends AbstractInContainerTestCase {

    private String sampleFeedUri = "http://feeds.feedburner.com/github";
    private String authnToken = null;
    private Connection connection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        authnToken = login(testUsername, testUsername);
    }

    @Test
    @Ignore
    public void testValidation() throws Exception {
        JSONObject json = new JSONObject();
        ConstraintViolationExceptionResponseDTO exceptionDTO;

        String req = makeRequest(connectionsBaseUrl, "POST", null, authnToken);
        exceptionDTO = jsonToObject(req,
                TypeFactory.defaultInstance().constructType(ConstraintViolationExceptionResponseDTO.class));

        assertEquals(3, exceptionDTO.getViolations().keySet().size());

        // Test invalid name
        json.put("alias", "a");

        exceptionDTO = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConstraintViolationExceptionResponseDTO.class));

        assertEquals(3, exceptionDTO.getViolations().keySet().size());

        json.put("alias", "GitHub Feed");

        exceptionDTO = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConstraintViolationExceptionResponseDTO.class));

        assertEquals(2, exceptionDTO.getViolations().keySet().size());

        json.put("type", FeedProvider.TYPE);

        exceptionDTO = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConstraintViolationExceptionResponseDTO.class));

        assertEquals(1, exceptionDTO.getViolations().keySet().size());

        // Test invalid url
        json.put("url", "h:/feeds.feedburner.com/github");

        exceptionDTO = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConstraintViolationExceptionResponseDTO.class));

        assertEquals(1, exceptionDTO.getViolations().keySet().size());

        json.put("url", sampleFeedUri);

        // Test invalid description
        int maxLength = 256;
        StringBuilder desc = new StringBuilder();

        for (int i = 0; i <= maxLength; i++) {
            desc.append("X");
        }

        json.put("description", desc.toString());
        json.put("providerId", FeedType.RSS.toString());
        
        exceptionDTO = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConstraintViolationExceptionResponseDTO.class));

        assertEquals(1, exceptionDTO.getViolations().keySet().size());

        json.put("description", "Valid description.");

        try {
            ConnectionResponseDTO connectionResponseDTO = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                    TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));
        } catch (Exception e) {
            fail("Should be able to create the feed now: " + e.getMessage());
        }
    }

}
