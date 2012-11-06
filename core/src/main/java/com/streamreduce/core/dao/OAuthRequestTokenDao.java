package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.streamreduce.core.model.OAuthRequestToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository("oauthRequestTokenDAO")
public class OAuthRequestTokenDao extends ValidatingDAO<OAuthRequestToken> {

    @Autowired
    protected OAuthRequestTokenDao(@Qualifier(value = "businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public OAuthRequestToken findByToken(String token) {
        if (token == null)  { return null; }
        return ds.find(entityClazz, "oauthToken", token).get();
    }

    public void removeToken(String token) {
        ds.delete(ds.find(entityClazz, "oauthToken", token));
    }
}
