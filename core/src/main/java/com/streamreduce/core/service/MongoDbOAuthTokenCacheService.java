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
