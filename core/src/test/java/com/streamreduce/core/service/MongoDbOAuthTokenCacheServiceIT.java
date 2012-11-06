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

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.dao.OAuthRequestTokenDao;
import org.junit.Before;
import org.junit.Test;
import org.scribe.model.Token;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MongoDbOAuthTokenCacheServiceIT extends AbstractServiceTestCase {

    MongoDbOAuthTokenCacheService oAuthTokenCacheService;

    @Autowired
    OAuthRequestTokenDao dao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        oAuthTokenCacheService = new MongoDbOAuthTokenCacheService(dao);
        Token testToken = new Token("access","secret");
        oAuthTokenCacheService.cacheToken(testToken);
    }

    @Test
    public void testRetrieveToken() throws Exception {
        Token retrievedToken = oAuthTokenCacheService.retrieveToken("access");
        assertEquals("access",retrievedToken.getToken());
        assertEquals("secret",retrievedToken.getSecret());

    }

    @Test
    public void testCacheTokenWithSameKey() throws Exception {
        Token newToken = new Token("access","anotherSecret");
        oAuthTokenCacheService.cacheToken(newToken);

        Token retrievedToken = oAuthTokenCacheService.retrieveToken("access");
        assertEquals("anotherSecret", retrievedToken.getSecret());
    }

    @Test
    public void testRetrieveTokenNullKey() throws Exception {
        Token retrievedToken = oAuthTokenCacheService.retrieveToken(null);
        assertEquals(null,retrievedToken);
    }

    @Test
    public void testRemoveToken() throws Exception {
        oAuthTokenCacheService.removeToken("access");
        Token retrievedToken = oAuthTokenCacheService.retrieveToken("access");
        assertNull(retrievedToken);
    }

    @Test
    public void testRetrieveAndRemoveToken() throws Exception {
        Token retrievedToken = oAuthTokenCacheService.retrieveAndRemoveToken("access");
        assertEquals("access",retrievedToken.getToken());
        assertEquals("secret",retrievedToken.getSecret());
        Token retrievedTokenAfterRemoval = oAuthTokenCacheService.retrieveToken("access");
        assertNull(retrievedTokenAfterRemoval);

    }
}
