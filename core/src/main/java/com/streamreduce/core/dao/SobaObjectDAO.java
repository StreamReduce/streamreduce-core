package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.SobaObject;
import org.springframework.util.Assert;

import java.util.List;

public abstract class SobaObjectDAO<T extends SobaObject> extends ValidatingDAO<T> {

    protected SobaObjectDAO(Datastore datastore) {
        super(datastore);
    }

    final public List<T> forAccount(Account account) {
        Assert.notNull(account);
        Query<T> q = ds.createQuery(entityClazz);
        q.criteria("account").equal(account);
        return q.asList();
    }
}
