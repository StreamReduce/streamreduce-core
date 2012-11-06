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
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository("accountDAO")
public class AccountDAO extends ValidatingDAO<Account> {

    @Autowired
    protected AccountDAO(@Qualifier(value="businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public Account findByName(String name) {
        Assert.hasText(name);
        return ds.find(entityClazz, "name", name).get();
    }

    public List<User> getUsersForAccount(Account account) {
        Assert.notNull(account);
        return ds.find(User.class, "account", account).asList();
    }
}
