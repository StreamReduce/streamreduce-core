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

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.JSONObject;
import org.apache.http.Header;
import org.scribe.oauth.OAuthService;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TwitterClient provides necessary methods for interacting with Twitter.
 */
public class TwitterClient extends ExternalIntegrationClient {

    //TODO: refactor this so we don't new up a ConnectionProviderFactory
    public static final String TWITTER_API_BASE = "https://api.twitter.com/1/";

    // TODO: Think about how to tune this
    protected final Cache<String, Object> requestCache = CacheBuilder.newBuilder()
                                                                     .concurrencyLevel(32)
                                                                     .weakValues()
                                                                     .build();
    final OAuthService oAuthService;


    /**
     * Constructs a client for Twitter using the credentials in the connection provided.
     *
     * @param connection the connection to use for interacting with Twitter
     */
    public TwitterClient(Connection connection, OAuthService oAuthService) {
        super(connection);

        Preconditions.checkArgument(connection.getProviderId().equals(ProviderIdConstants.TWITTER_PROVIDER_ID),
                                    "TwitterClient only supports connections with the provider id of 'twitter'.");

        debugLog(LOGGER, "Client created for " + getConnectionCredentials().getIdentity());
        this.oAuthService = oAuthService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection() throws InvalidCredentialsException, IOException {
        debugLog(LOGGER, "Validating connection");
        getLoggedInProfile();
    }

    /**
     * Returns a JSONObject representing the currently logged in user profile.
     *
     * @see <a href="https://dev.twitter.com/docs/api/1/get/account/verify_credentials" />
     *
     * @return the JSONObject for the user
     *
     * @throws InvalidCredentialsException if the credentials for this client are invalid
     * @throws IOException if any communication exception occurs
     */
    public JSONObject getLoggedInProfile() throws InvalidCredentialsException, IOException {
        return makeSimpleRequest(TWITTER_API_BASE + "account/verify_credentials.json?" +
                                          "skip_status=true&include_entities=false");
    }

    /**
     * Makes a Twitter request that requires no pagination/cursoring.
     *
     * @param url the url to make the request
     *
     * @return the JSONObject or null
     */
    private JSONObject makeSimpleRequest(String url) throws InvalidCredentialsException, IOException {
        List<Header> responseHeaders = new ArrayList<Header>();
        String payload = HTTPUtils.openOAuthUrl(url, HttpMethod.GET, null, MediaType.APPLICATION_JSON, oAuthService,
                                                getConnectionCredentials(), null, responseHeaders);

        return payload != null ? JSONObject.fromObject(payload) : null;
    }

}
