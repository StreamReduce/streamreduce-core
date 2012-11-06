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
