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

package com.streamreduce.storm.bolts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.streamreduce.core.event.EventId;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link SobaMessageMetricsBolt} works properly.
 */
public class UserMetricsBoltTest extends AbstractMetricsBoltTest {

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractMetricsBolt getBolt() {
        return new UserMetricsBolt();
    }

    /**
     * Make sure account event handling works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEventHandling() throws Exception {
        List<Map<String, Object>> events = getEventsForTesting();
        Map<String, Float> metrics = processEvents(events);

        for (Map.Entry<String, Float> metric : metrics.entrySet()) {
            String metricId = metric.getKey();
            Float metricValue = metric.getValue();

            if (metricId.equals("global.USER_COUNT")) {
                Assert.assertEquals(new Float(4), metricValue);
            } else if (metricId.equals("globa.PENDING_USER_COUNT")) {
                Assert.assertEquals(new Float(1), metricValue);
            } else {
                if (metricId.endsWith("PENDING_USER_COUNT")) {
                    Assert.assertEquals(new Float(1), metricValue);
                } else {
                    Assert.assertEquals(new Float(2), metricValue);
                }
            }
        }
    }

    /**
     * Return a list of mocked message events for testing.
     *
     * @return the list of mock events
     */
    public List<Map<String, Object>> getEventsForTesting() {
        List<Map<String, Object>> allEvents = new ArrayList<>();

        Map<String, Object> pendingUserMetadata = new HashMap<>();

        pendingUserMetadata.put("userRequestIsNew", true);

        Map<String, Object> newAccountAndUser = createBaseEventMock(EventId.CREATE, "User", null);
        Map<String, Object> newAccountUser = createBaseEventMock(EventId.CREATE,
                                                                 newAccountAndUser.get("accountId").toString(),
                                                                 null,
                                                                 new ObjectId().toString(), "User", null);
        Map<String, Object> anotherNewAccountAndUser = createBaseEventMock(EventId.CREATE, "User", null);
        Map<String, Object> anotherNewAccountPendingUser = createBaseEventMock(EventId.CREATE_USER_INVITE_REQUEST,
                                                                               anotherNewAccountAndUser.get("accountId")
                                                                                                       .toString(),
                                                                               null,
                                                                               new ObjectId().toString(), "User",
                                                                               pendingUserMetadata);

        allEvents.add(newAccountAndUser);
        allEvents.add(newAccountUser);
        allEvents.add(anotherNewAccountAndUser);
        allEvents.add(anotherNewAccountPendingUser);

        return allEvents;
    }

}
