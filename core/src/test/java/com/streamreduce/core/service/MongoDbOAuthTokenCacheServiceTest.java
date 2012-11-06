package com.streamreduce.core.service;

import com.streamreduce.core.dao.OAuthRequestTokenDao;
import com.streamreduce.core.model.OAuthRequestToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.scribe.model.Token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MongoDbOAuthTokenCacheServiceTest {

    MongoDbOAuthTokenCacheService mongoDbOAuthTokenCacheService;

    @Mock
    OAuthRequestTokenDao oAuthRequestTokenDao;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        OAuthRequestToken testToken = new OAuthRequestToken(new Token("access", "secret"));
        when(oAuthRequestTokenDao.findByToken("access")).thenReturn(testToken);
        mongoDbOAuthTokenCacheService = new MongoDbOAuthTokenCacheService(oAuthRequestTokenDao);
    }

    @Test
    public void testCacheToken() throws Exception {
        Token t = new Token("foo", "bar");
        mongoDbOAuthTokenCacheService.cacheToken(t);
        verify(oAuthRequestTokenDao).save(any(OAuthRequestToken.class));
    }

    @Test
    public void testCacheToken_Null() throws Exception {
        mongoDbOAuthTokenCacheService.cacheToken(null);
        verify(oAuthRequestTokenDao, times(0)).save(any(OAuthRequestToken.class));
    }

    @Test
    public void testCacheToken_OauthTokenNull() throws Exception {
        mongoDbOAuthTokenCacheService.cacheToken(new Token(null,"whatever"));
        verify(oAuthRequestTokenDao, times(0)).save(any(OAuthRequestToken.class));
    }

    @Test
    public void testRetrieveToken() throws Exception {
        Token expected = new Token("access","secret");
        Token actual = mongoDbOAuthTokenCacheService.retrieveToken("access");
        assertEquals(expected.getToken(),actual.getToken());
        assertEquals(expected.getSecret(),actual.getSecret());
    }

    @Test
    public void testRetrieveToken_Null() throws Exception {
        Token actual = mongoDbOAuthTokenCacheService.retrieveToken(null);
        assertNull(actual);
    }

    @Test
    public void testRetrieveToken_CacheMiss() throws Exception {
        Token actual = mongoDbOAuthTokenCacheService.retrieveToken("not here, really");
        assertNull(actual);
    }

    @Test
    public void testRemoveToken() throws Exception {
        mongoDbOAuthTokenCacheService.removeToken("access");
        verify(oAuthRequestTokenDao,times(1)).removeToken("access");
    }

    @Test
    public void testRemoveToken_Null() throws Exception {
        mongoDbOAuthTokenCacheService.removeToken(null);
        verify(oAuthRequestTokenDao,times(0)).removeToken(anyString());
    }

    @Test
    public void testRetrieveAndRemoveToken() throws Exception {
        Token retrievedToken = mongoDbOAuthTokenCacheService.retrieveAndRemoveToken("access");
        verify(oAuthRequestTokenDao,times(1)).findByToken("access");
        verify(oAuthRequestTokenDao,times(1)).removeToken("access");
        Assert.assertNotNull(retrievedToken);
    }

    @Test
    public void testRetrieveAndRemoveToken_NonExistentKey() throws Exception {
        Token retrievedToken = mongoDbOAuthTokenCacheService.retrieveAndRemoveToken("not here");
        verify(oAuthRequestTokenDao,times(1)).findByToken("not here");
        verify(oAuthRequestTokenDao,times(0)).removeToken(anyString());
        assertNull(retrievedToken);
    }

    @Test
    public void testRetrieveAndRemoveToken_NullKey() throws Exception {
        Token retrievedToken = mongoDbOAuthTokenCacheService.retrieveAndRemoveToken(null);
        verify(oAuthRequestTokenDao,times(0)).findByToken(anyString());
        verify(oAuthRequestTokenDao,times(0)).removeToken(anyString());
        assertNull(retrievedToken);
    }
}
