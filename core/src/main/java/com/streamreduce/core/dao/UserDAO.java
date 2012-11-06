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
import com.streamreduce.core.model.APIAuthenticationToken;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository("userDAO")
public class UserDAO extends ValidatingDAO<User> {

    @Autowired
    protected UserDAO(@Qualifier(value = "businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public User findUser(String username) {
        Assert.hasText(username);
        return ds.createQuery(entityClazz)
                .field("username").equal(Pattern.compile("^\\Q" + username + "\\E$", Pattern.CASE_INSENSITIVE))
                .get();
    }

    public User findUser(String signupKey, String userId) {
        Assert.hasText(signupKey);
        Assert.hasText(userId);
        return ds.createQuery(entityClazz)
                .field("secretKey").equal(signupKey)
                .field("_id").equal(new ObjectId(userId))
                .get();
    }

    public User findInvitedUser(String inviteKey, String accountId) {
        Assert.hasText(inviteKey);
        Assert.hasText(accountId);

        User user = ds.createQuery(entityClazz)
                .field("secretKey").equal(inviteKey)
                .get();

        if (user != null) {
            if ((user.getAccount().getId().toString().equals(accountId))) {
                return user;
            }
        }
        return null;
    }

    public List<User> allUsersForAccount(Account account) {
        return ds.find(entityClazz, "account", account).asList();
    }

    public List<User> allEnabledUsersForAccount(Account account) {
        return ds.createQuery(entityClazz)
                .field("account").equal(account)
                .field("userStatus").notEqual(User.UserStatus.DISABLED)
                .asList();

    }

    public User findUserForAlias(Account account, String alias) {
        return ds.createQuery(entityClazz)
                .field("alias").equal(Pattern.compile("^\\Q" + alias + "\\E$", Pattern.CASE_INSENSITIVE))
                .field("account").equal(account)
                .get();
    }

    public User findUserForUsername(Account account, String username) {
        return ds.createQuery(entityClazz)
                .field("username").equal(Pattern.compile("^\\Q" + username + "\\E$", Pattern.CASE_INSENSITIVE))
                .field("account").equal(account)
                .get();
    }

    public User findUserInAccount(Account account, String name) {
        Query<User> query = ds.createQuery(entityClazz);
        query.field("account").equal(account);
        query.or(
                query.criteria("username").equal(name),
                query.criteria("alias").equal(Pattern.compile("^\\Q" + name + "\\E$", Pattern.CASE_INSENSITIVE))
        );
        return query.get();
    }

    public APIAuthenticationToken findAuthToken(String authenticationToken) {
        User user = findByAuthToken(authenticationToken);
        if (user != null) {
            return user.getAuthenticationToken();
        }
        return null;
    }

    public User findByAuthToken(String authenticationToken) {
        Assert.hasText(authenticationToken);
        APIAuthenticationToken authToken = new APIAuthenticationToken(authenticationToken);

        return ds.createQuery(entityClazz)
                .field("authenticationToken").hasThisElement(authToken)
                .get();
    }


    public Set<User> getActiveLoggedInUsers(Account account, Long maxInactivity) {
        Query<User> q = createQuery();
        q.field("account").equal(account);
        q.field("lastActivity").greaterThanOrEq(System.currentTimeMillis() - maxInactivity);
        return new HashSet<User>(q.asList());
    }

}

