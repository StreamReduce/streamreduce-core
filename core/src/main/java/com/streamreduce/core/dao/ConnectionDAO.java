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
import com.google.code.morphia.query.Query;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.SobaObject.Visibility;
import com.streamreduce.core.model.User;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository("connectionDAO")
public class ConnectionDAO extends ValidatingDAO<Connection> {

    @Autowired
    protected ConnectionDAO(@Qualifier(value = "businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public Connection getByAPIKey(String apiKey, String type) {
        Query<Connection> q = ds.createQuery(entityClazz);
        q.field("authenticationToken.token").equal(apiKey);
        q.field("type").equal(type);
        return q.get();
    }

    public List<Connection> forTypeAndUser(@Nullable String type, User user) {
        Assert.notNull(user);
        Query<Connection> q = ds.createQuery(entityClazz);
        if (type != null) {
            q.field("type").equal(type);
        }
        q.or(
                q.criteria("user").equal(user), // Return all user connections
                q.and(
                        q.criteria("account").equal(user.getAccount()), // Return all account visible connections
                        q.criteria("visibility").equal(Visibility.ACCOUNT)
                ),
                q.criteria("visibility").equal(Visibility.PUBLIC) // Return all public connections
        );
        return q.asList();
    }

    public List<Connection> forAccount(Account account) {
        Assert.notNull(account);
        Query<Connection> q = ds.createQuery(entityClazz);
        q.criteria("account").equal(account);
        return q.asList();
    }

    public Connection getByAliasAndType(Account account, String alias, String type) {
        Assert.notNull(account);
        Assert.notNull(alias);
        Query<Connection> q = ds.createQuery(entityClazz);
        q.field("type").equal(type);
        q.criteria("account").equal(account);
        q.criteria("alias").equal(alias);
        return q.get();
    }

    public List<Connection> allConnectionsOfType(@Nullable String type) {
        Query<Connection> q = ds.createQuery(entityClazz);
        if (type != null) {
            q.field("type").equal(type);
        }
        return q.asList();
    }

    public List<Connection> allBrokenConnectionsOfType(@Nullable String type) {
        Query<Connection> q = ds.createQuery(entityClazz);
        if (type != null) {
            q.field("type").equal(type);
        }
        q.field("broken").equal(true);
        return q.asList();
    }

    public List<Connection> allDisabledConnectionsOfType(@Nullable String type) {
        Query<Connection> q = ds.createQuery(entityClazz);
        if (type != null) {
            q.field("type").equal(type);
        }
        q.field("disabled").equal(true);
        return q.asList();
    }

    public List<Connection> allPublicConnectionsOfType(@Nullable String type) {
        Query<Connection> q = ds.createQuery(entityClazz);
        if (type != null) {
            q.field("type").equal(type);
        }
        q.criteria("visibility").equal(Visibility.PUBLIC);
        return q.asList();
    }

}
