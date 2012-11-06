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

package com.streamreduce.core.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.scribe.model.Token;

import java.util.concurrent.TimeUnit;

public class LocalOAuthTokenCacheService implements OAuthTokenCacheService {
    Cache<String, Token> tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    @Override
    public void cacheToken(Token requestToken) {
        if (requestToken == null || requestToken.getToken() == null) {
            return;
        }
        tokenCache.put(requestToken.getToken(), requestToken);
    }

    @Override
    public Token retrieveToken(String oauthToken) {
        if (oauthToken == null) {
            return null;
        }
        return tokenCache.getIfPresent(oauthToken);
    }

    @Override
    public void removeToken(String oauthToken) {
        if (oauthToken == null) { return; }
        tokenCache.invalidate(oauthToken);
    }

    @Override
    public Token retrieveAndRemoveToken(String oauthToken) {
        Token retrievedToken = retrieveToken(oauthToken);
        if (retrievedToken != null) {
            tokenCache.invalidate(oauthToken);
        }
        return retrievedToken;
    }
}
