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
