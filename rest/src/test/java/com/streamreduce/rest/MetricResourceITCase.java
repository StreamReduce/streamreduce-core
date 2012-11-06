package com.streamreduce.rest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.rest.resource.ErrorMessage;
import com.streamreduce.rest.resource.api.MetricResource;
import junit.framework.Assert;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Class that tests {@link com.streamreduce.rest.resource.api.MetricResource} works as expected.
 */
public class MetricResourceITCase extends AbstractInContainerTestCase {

    /**
     * Tests that {@link MetricResource#getMetrics(javax.ws.rs.core.UriInfo)} returns error objects as expected for
     * the known bad request scenarios.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore
    public void testInvalidRequests() throws Exception {
        Map<String, String> urlsToTest = ImmutableMap.<String, String> builder()
                                                     .put(getPublicApiUrlBase() + "/metrics",
                                                          "ACCOUNT_ID is a required, non-empty query parameter.")
                                                     .put(getPublicApiUrlBase() + "/metrics?ACCOUNT_ID=global",
                                                          "METRIC_NAME is a required, non-empty query parameter.")
                                                     .put(getPublicApiUrlBase() + "/metrics?ACCOUNT_ID=global" +
                                                                  "&METRIC_NAME=FAKE",
                                                          "GRANULARITY is a required, non-empty query parameter.")
                                                     .put(getPublicApiUrlBase() + "/metrics?ACCOUNT_ID=global" +
                                                                  "&METRIC_NAME=FAKE&GRANULARITY=FAKE",
                                                          "FAKE is not a valid METRIC_NAME.")
                                                     .put(getPublicApiUrlBase() + "/metrics?ACCOUNT_ID=global" +
                                                                  "&METRIC_NAME=ACCOUNT_COUNT&GRANULARITY=FAKE",
                                                          "FAKE is not a valid GRANULARITY.")
                                                     .put(getPublicApiUrlBase() + "/metrics?ACCOUNT_ID=global" +
                                                                  "&METRIC_NAME=ACCOUNT_COUNT&GRANULARITY=DAYS" +
                                                                  "&START_TIME=FAKE",
                                                          "START_TIME parameter should be a number.")
                                                     .put(getPublicApiUrlBase() + "/metrics?ACCOUNT_ID=global" +
                                                                  "&METRIC_NAME=ACCOUNT_COUNT&GRANULARITY=DAYS" +
                                                                  "&criteria.FAKE=FAKE",
                                                          "FAKE is not a valid METRIC_CRITERIA.")
                                                     .build();
        String authToken = login(testUsername, testUsername);

        for (Map.Entry<String, String> urlToTest : urlsToTest.entrySet()) {
            ErrorMessage error = jsonToObject(makeRequest(urlToTest.getKey(), "GET", null, authToken),
                                              TypeFactory.defaultInstance().constructType(ErrorMessage.class));

            Assert.assertEquals(urlToTest.getValue(), error.getErrorMessage());
        }
    }

    /**
     * Tests that {@link MetricResource#getMetrics(javax.ws.rs.core.UriInfo)} returns the expected metrics.
     *
     * @throws Exception if anything goes wrong
     */
    public void testValidRequests() throws Exception {
        // TODO: Implement MetricResourceITCase#testValidRequests()
    }

}
