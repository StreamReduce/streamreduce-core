package com.streamreduce.core.service;

import org.scribe.model.Token;

/**
 * A Service that allows {@link org.scribe.model.Token} instances created for Oauth in Scribe to be cached during a
 * OAuth handshakes that span multiple HTTP/REST interactions with the Nodeable Server.
 *
 * This service only keeps one cache for OAuth Tokens created for Connections whose provider is
 * {@link com.streamreduce.connections.TwitterProvider}
 */
public interface OAuthTokenCacheService {

    /**
     * Caches a Token using the Token's token property as a key and the full token instance
     * as the value.  If null is passed in as the parameter or if Token.token is null,
     * then no caching occurs.
     *
     * If Token.token already exists as a key in the cache, then its value is replaced with
     * the pass in Token instance.
     *
     * @param requestToken - The {@link org.scribe.model.Token} instance that should be cached.
     */
    public void cacheToken(Token requestToken);

    /**
     * Retreives a cached Token whose key in the cached is the passed in String token value.
     * @param token - An expected Token.token key that should be in the cache.
     * @return A {@link org.scribe.model.Token} instance retrieved from the cache, or null if
     * the token key didn't exist in the Cache.
     */
    public Token retrieveToken(String oauthToken);

    /**
     * Combines {@link OAuthTokenCacheService#retrieveToken(String)} and
     * {@link OAuthTokenCacheService#removeToken(String)} into a single call.  A Token object identified by the
     * passed in oauthToken String is retrieved from cache, removed from cache, and returned.  If a Token could not
     * be found, nothing is removed.
     *
     * @return A Token that is removed from cache after this method returns, or null or if the Token could not be found.
     */
    public Token retrieveAndRemoveToken(String oauthToken);

    /**
     * Removes the token identified by the passed in String token value from the cache.  If null
     * is passed in, no entires are removed from the cache..
     * @param token - An expected Token.token key whose entry should be removed from the cache.
     */
    public void removeToken(String oauthToken);
}
