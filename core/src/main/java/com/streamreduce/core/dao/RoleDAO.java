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
