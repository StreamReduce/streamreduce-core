package com.streamreduce.core.service;

import com.streamreduce.core.dao.OAuthRequestTokenDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuthTokenCacheServiceFactory {

    @Value("${cache.type}")
    String cacheType;

    @Autowired
    OAuthRequestTokenDao oAuthRequestTokenDao;

    @Bean(name = "oauthTOkenCacheService")
    public OAuthTokenCacheService getOAuthTokenCacheService() {
        if ("mongo".equals(cacheType)) {
            return new MongoDbOAuthTokenCacheService(oAuthRequestTokenDao);
        }
        return new LocalOAuthTokenCacheService();
    }
}
