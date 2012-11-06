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

package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Convenience class for unit tests ran outside of a Spring Context.
 */
public class ConnectionProvidersForTests {

    public static final AWSCloudProvider AWS_CLOUD_PROVIDER;
    public static final CustomProvider CUSTOM_PROVIDER;
    public static final RssProvider RSS_PROVIDER;
    public static final GitHubProjectHostingProvider GITHUB_PROVIDER;
    public static final GoogleAnalyticsProvider GOOGLE_ANALYTICS_PROVIDER;
    public static final JiraProjectHostingProvider JIRA_PROVIDER;
    public static final PingdomProvider PINGDOM_PROVIDER;
    public static final TwitterProvider TWITTER_PROVIDER;
    public static final WebHDFSProvider WEBHDFS_PROVIDER;

    static {
        try {
            AWS_CLOUD_PROVIDER = mock(AWSCloudProvider.class);
            when(AWS_CLOUD_PROVIDER.getId()).thenReturn(ProviderIdConstants.AWS_PROVIDER_ID);
            when(AWS_CLOUD_PROVIDER.getType()).thenReturn(CloudProvider.TYPE);

            CUSTOM_PROVIDER = mock(CustomProvider.class);
            when(CUSTOM_PROVIDER.getId()).thenReturn(ProviderIdConstants.CUSTOM_PROVIDER_ID);
            when(CUSTOM_PROVIDER.getType()).thenReturn(CustomProvider.TYPE);

            RSS_PROVIDER = mock(RssProvider.class);
            when(RSS_PROVIDER.getId()).thenReturn(ProviderIdConstants.FEED_PROVIDER_ID);
            when(RSS_PROVIDER.getType()).thenReturn(FeedProvider.TYPE);

            GITHUB_PROVIDER = mock(GitHubProjectHostingProvider.class);
            when(GITHUB_PROVIDER.getId()).thenReturn(ProviderIdConstants.GITHUB_PROVIDER_ID);
            when(GITHUB_PROVIDER.getType()).thenReturn(ProjectHostingProvider.TYPE);
            when(GITHUB_PROVIDER.getSupportedAuthTypes()).thenReturn(Sets.newHashSet(AuthType.OAUTH));

            GOOGLE_ANALYTICS_PROVIDER = mock(GoogleAnalyticsProvider.class);
            when(GOOGLE_ANALYTICS_PROVIDER.getId()).thenReturn(ProviderIdConstants.GOOGLE_ANALYTICS_PROVIDER_ID);
            when(GOOGLE_ANALYTICS_PROVIDER.getType()).thenReturn(AnalyticsProvider.TYPE);

            JIRA_PROVIDER = mock(JiraProjectHostingProvider.class);
            when(JIRA_PROVIDER.getId()).thenReturn(ProviderIdConstants.JIRA_PROVIDER_ID);
            when(JIRA_PROVIDER.getType()).thenReturn(ProjectHostingProvider.TYPE);

            PINGDOM_PROVIDER = mock(PingdomProvider.class);
            when(PINGDOM_PROVIDER.getId()).thenReturn(ProviderIdConstants.PINGDOM_PROVIDER_ID);
            when(PINGDOM_PROVIDER.getType()).thenReturn(MonitoringProvider.TYPE);

            TWITTER_PROVIDER = mock(TwitterProvider.class);
            when(TWITTER_PROVIDER.getId()).thenReturn(ProviderIdConstants.TWITTER_PROVIDER_ID);
            when(TWITTER_PROVIDER.getType()).thenReturn(SocialProvider.TYPE);
            when(TWITTER_PROVIDER.getSupportedAuthTypes()).thenReturn(Sets.newHashSet(AuthType.OAUTH));

            WEBHDFS_PROVIDER = mock(WebHDFSProvider.class);
            when(WEBHDFS_PROVIDER.getId()).thenReturn(ProviderIdConstants.WEBHDFS_PROVIDER_ID);
            when(WEBHDFS_PROVIDER.getType()).thenReturn(GatewayProvider.TYPE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
