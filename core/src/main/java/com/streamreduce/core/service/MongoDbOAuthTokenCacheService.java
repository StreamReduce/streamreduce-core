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


import com.streamreduce.core.dao.OAuthRequestTokenDao;
import com.streamreduce.core.model.OAuthRequestToken;
import org.scribe.model.Token;

public class MongoDbOAuthTokenCacheService implements OAuthTokenCacheService {

    OAuthRequestTokenDao dao;

    public MongoDbOAuthTokenCacheService(OAuthRequestTokenDao dao) {
        this.dao = dao;
    }

    @Override
    public void cacheToken(Token requestToken) {
        if (requestToken == null || requestToken.getToken() == null) { return ; }
        dao.save(new OAuthRequestToken(requestToken));
    }


    @Override
    public Token retrieveToken(String oauthToken) {
        if (oauthToken == null) { return null; }
        OAuthRequestToken requestToken = dao.findByToken(oauthToken);
        if (requestToken == null) { return null; }
        return requestToken.toScribeToken();
    }

    @Override
    public void removeToken(String oauthToken) {
        if (oauthToken == null) { return;}
        dao.removeToken(oauthToken);
    }

    @Override
    public Token retrieveAndRemoveToken(String oauthToken) {
        Token retrievedToken = retrieveToken(oauthToken);
        if (retrievedToken != null) {
            removeToken(oauthToken);
        }
        return retrievedToken;
    }
}
