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

import com.google.api.services.analytics.AnalyticsScopes;
import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.util.ExternalIntegrationClient;
import com.streamreduce.util.GoogleAnalyticsClient;
import net.sf.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.GoogleApi20;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/5/12 09:29</p>
 */
@Component
public class GoogleAnalyticsProvider extends AbstractAnalyticsProvider
        implements OAuthEnabledConnectionProvider, ExternalIntegrationConnectionProvider {

    private static final Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(
            AuthType.OAUTH
    );

    @Value("${nodeable.googleanalytics.oauth.callback.url}")
    private String oauthCallbackUrl;
    @Value("${nodeable.googleanalytics.oauth.client_id}")
    private String oauthClientId;
    @Value("${nodeable.googleanalytics.oauth.secret}")
    private String oauthSecret;

    public GoogleAnalyticsProvider() {}

    @Override
    public ExternalIntegrationClient getClient(Connection connection) {
        return new GoogleAnalyticsClient(connection);
    }

    @Override
    public OAuthService getOAuthService() {
        return new ServiceBuilder()
                .scope(AnalyticsScopes.ANALYTICS_READONLY)
                .provider(GoogleApi20.class)
                .apiKey(getClientId())
                .apiSecret(getClientSecret())
                .callback(getCallbackUrl())
                .grantType("authorization_code")
                .accessType("offline")
                .build();
    }

    @Override
    public String getAuthorizationUrl() {
        OAuthService oAuthService = getOAuthService();
        String authorizationUrl = oAuthService.getAuthorizationUrl(null);
        // need to add this manually until the next version of the Jalios fork of scribe-java is released.
        return authorizationUrl + "&approval_prompt=force";
    }

    @Override
    public String getIdentityFromProvider(Connection c) {
        return null;
    }

    @Override
    public void updateCredentials(ConnectionCredentials credentials, Token token) {
        JSONObject jsonToken = JSONObject.fromObject(token.getRawResponse());

        if (jsonToken.containsKey("access_token")) {
            credentials.setOauthToken(jsonToken.getString("access_token"));
        }

        if (jsonToken.containsKey("refresh_token")) {
            credentials.setOauthRefreshToken(jsonToken.getString("refresh_token"));
        }
        credentials.setIdentity(null);
        credentials.setCredential(null);
        credentials.setApiKey(null);
    }

    @Override
    public String getId() {
        return ProviderIdConstants.GOOGLE_ANALYTICS_PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return "Google Analytics";
    }

    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

    public String getCallbackUrl() {
        return oauthCallbackUrl;
    }

    public String getClientId() {
        return oauthClientId;
    }

    public String getClientSecret() {
        return oauthSecret;
    }

}
