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

import com.google.common.collect.Lists;
import com.streamreduce.connections.ConnectionProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConnectionUtilsTest {

    @Test
    public void testGetAllProviders() {
        final List<String> expectedProviderIds =
                Lists.newArrayList("aws", "github", "jira", "rss", "custom", "pingdom",
                                   "twitter", "nagios", "googleanalytics");

        List<String> actualProviderIds = new ArrayList<String>();
        for (ConnectionProvider provider : ConnectionUtils.getAllProviders()) {
            actualProviderIds.add(provider.getId());
        }
        Collections.sort(expectedProviderIds);
        Collections.sort(actualProviderIds);

        Assert.assertEquals(expectedProviderIds, actualProviderIds);
    }
}
