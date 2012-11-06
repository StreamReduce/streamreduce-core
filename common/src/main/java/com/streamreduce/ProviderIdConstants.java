package com.streamreduce;

import static com.streamreduce.feed.types.FeedType.RSS;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class ProviderIdConstants {

    private ProviderIdConstants() {}

    public static final String AWS_PROVIDER_ID = "aws";
    public static final String CUSTOM_PROVIDER_ID = "custom";
    public static final String FEED_PROVIDER_ID = RSS.toString();
    public static final String GITHUB_PROVIDER_ID = "github";
    public static final String GOOGLE_ANALYTICS_PROVIDER_ID = "googleanalytics";
    public static final String JIRA_PROVIDER_ID = "jira";
    public static final String NAGIOS_PROVIDER_ID = "nagios";
    public static final String PINGDOM_PROVIDER_ID = "pingdom";
    public static final String TWITTER_PROVIDER_ID = "twitter";
    public static final String WEBHDFS_PROVIDER_ID = "webhdfs";
    public static final Set<String> ALL_PROVIDER_IDS = ImmutableSet.of(
            AWS_PROVIDER_ID,
            CUSTOM_PROVIDER_ID,
            FEED_PROVIDER_ID,
            GITHUB_PROVIDER_ID,
            JIRA_PROVIDER_ID,
            NAGIOS_PROVIDER_ID,
            PINGDOM_PROVIDER_ID,
            TWITTER_PROVIDER_ID,
            WEBHDFS_PROVIDER_ID
    );

}
