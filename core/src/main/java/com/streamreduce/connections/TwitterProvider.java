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
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.service.OAuthTokenCacheService;
import com.streamreduce.util.TwitterClient;
import net.sf.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The TwitterProvider class is an implementation of the
 * {@link com.streamreduce.connections.ConnectionProvider} that provides support
 * for the Twitter.
 */
@Component
public class TwitterProvider extends AbstractSocialProvider
        implements OAuthEnabledConnectionProvider, ExternalIntegrationConnectionProvider {

    private static final Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(
            AuthType.OAUTH
    );

    @Autowired
    private OAuthTokenCacheService cacheService;

    @Value("${nodeable.twitter.oauth.client_id}")
    private String oauthClientId;
    @Value("${nodeable.twitter.oauth.secret}")
    private String oauthSecret;
    @Value("${nodeable.twitter.oauth.callback.url}")
    private String oauthCallbackUrl;

    public TwitterProvider() {
        //Needed for SPI
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ProviderIdConstants.TWITTER_PROVIDER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "Twitter";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OAuthService getOAuthService() {
        return new ServiceBuilder()
                .provider(TwitterApi.class)
                .apiKey(oauthClientId)
                .apiSecret(oauthSecret)
                .callback(oauthCallbackUrl)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthorizationUrl() {
        OAuthService oAuthService = getOAuthService();
        Token requestToken = oAuthService.getRequestToken();
        String authorizationUrl = oAuthService.getAuthorizationUrl(requestToken);
        cacheService.cacheToken(requestToken);
        return authorizationUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentityFromProvider(Connection c) {
        try {
            TwitterClient twitterClient = getClient(c);
            JSONObject jsonObject = twitterClient.getLoggedInProfile();
            return jsonObject.getString("screen_name");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateCredentials(ConnectionCredentials credentials, Token token) {
        credentials.setOauthToken(token.getToken());
        credentials.setOauthTokenSecret(token.getSecret());
        credentials.setIdentity(null);
        credentials.setCredential(null);
        credentials.setApiKey(null);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public TwitterClient getClient(Connection connection) {
        return new TwitterClient(connection,getOAuthService());
    }
}
