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
import com.streamreduce.core.model.Role;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository("roleDAO")
public class RoleDAO extends ValidatingDAO<Role> {

    @Autowired
    protected RoleDAO(@Qualifier(value = "businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public Role findRole(String name) {
        Assert.hasText(name);
        return ds.find(entityClazz, "name", name).get();
    }

    // remove super user!
    public Set<Role> findAccountRoles(ObjectId accountId) {
        Assert.notNull(accountId);
        return new HashSet<Role>(ds.find(entityClazz).asList());
    }
}
