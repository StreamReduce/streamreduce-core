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
