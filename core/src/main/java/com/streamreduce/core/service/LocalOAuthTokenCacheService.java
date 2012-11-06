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
