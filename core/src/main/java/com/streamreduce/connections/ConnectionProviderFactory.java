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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.streamreduce.core.service.OAuthTokenCacheService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

@Service("connectionProviderFactory")
public class ConnectionProviderFactory implements InitializingBean {

    //cached sets of references to different provider types
    private ImmutableSet<ConnectionProvider> allConnectionProviders;
    private ImmutableSet<OAuthEnabledConnectionProvider> allOauthEnabledConnectionProviders;
    private ImmutableSet<ExternalIntegrationConnectionProvider> allExternalIntegrationConnectionProviders;
    private ImmutableSet<PushConnectionProvider> allPushConnectionProviders;

    @Autowired
    OAuthTokenCacheService oAuthTokenCacheService;

    @Autowired
    private AWSCloudProvider awsCloudProvider;
    @Autowired
    private CustomProvider customProvider;
    @Autowired
    private GitHubProjectHostingProvider gitHubProjectHostingProvider;
    @Autowired
    private GoogleAnalyticsProvider googleAnalyticsProvider;
    @Autowired
    private JiraProjectHostingProvider jiraProjectHostingProvider;
    @Autowired
    private NagiosProvider nagiosProvider;
    @Autowired
    private PingdomProvider pingdomProvider;
    @Autowired
    private RssProvider rssProvider;
    @Autowired
    private TwitterProvider twitterProvider;
    @Autowired
    private WebHDFSProvider webHDFSProvider;


    @Override
    public void afterPropertiesSet() throws Exception {
        /*
           If new providers are introduced, add them to this Set, the rest of the constructor dynamically populates
           the other Sets.    Only include concrete providers here.
        */
        allConnectionProviders = new ImmutableSet.Builder<ConnectionProvider>()
                .add(awsCloudProvider)
                .add(customProvider)
                .add(gitHubProjectHostingProvider)
                .add(googleAnalyticsProvider)
                .add(jiraProjectHostingProvider)
                .add(nagiosProvider)
                .add(pingdomProvider)
                .add(rssProvider)
                .add(twitterProvider)
                .add(webHDFSProvider)
                .build();

        allOauthEnabledConnectionProviders = ImmutableSet.copyOf(Iterables.filter(allConnectionProviders,
                OAuthEnabledConnectionProvider.class));

        allExternalIntegrationConnectionProviders = ImmutableSet.copyOf(Iterables.filter(allConnectionProviders,
                ExternalIntegrationConnectionProvider.class));

        allPushConnectionProviders = ImmutableSet.copyOf(Iterables.filter(allConnectionProviders,
                PushConnectionProvider.class));
    }

    public ConnectionProvider connectionProviderFromId(final String providerId) {
        return Iterables.find(allConnectionProviders, new Predicate<ConnectionProvider>() {
            @Override
            public boolean apply(@Nullable ConnectionProvider connectionProvider) {
                return connectionProvider != null && connectionProvider.getId().equals(providerId);
            }
        });
    }

    public OAuthEnabledConnectionProvider oauthEnabledConnectionProviderFromId(final String providerId) {
        return Iterables.find(allOauthEnabledConnectionProviders, new Predicate<OAuthEnabledConnectionProvider>() {
            @Override
            public boolean apply(@Nullable OAuthEnabledConnectionProvider connectionProvider) {
                return connectionProvider != null && connectionProvider.getId().equals(providerId);
            }
        });
    }

    public ExternalIntegrationConnectionProvider externalIntegrationConnectionProviderFromId(final String providerId) {
        return Iterables.find(allExternalIntegrationConnectionProviders, new Predicate<ExternalIntegrationConnectionProvider>() {
            @Override
            public boolean apply(@Nullable ExternalIntegrationConnectionProvider connectionProvider) {
                return connectionProvider != null && connectionProvider.getId().equals(providerId);
            }
        });
    }

    public PushConnectionProvider pushConnectionProviderFromId(final String providerId) {
        return Iterables.tryFind(allPushConnectionProviders, new Predicate<PushConnectionProvider>() {
            @Override
            public boolean apply(@Nullable PushConnectionProvider connectionProvider) {
                return connectionProvider != null && connectionProvider.getId().equals(providerId);
            }
        }).orNull();
    }
}
