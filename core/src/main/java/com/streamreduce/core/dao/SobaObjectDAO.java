package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.SobaObject;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

public abstract class SobaObjectDAO<T extends SobaObject> extends ValidatingDAO<T> {

    protected SobaObjectDAO(Datastore datastore) {
        super(datastore);
    }

    public List<T> forAccount(Account account) {
        Assert.notNull(account);
        Query<T> q = ds.createQuery(entityClazz);
        q.criteria("account").equal(account);
        return q.asList();
    }


    /**
     * Returns the list of all objects of type T who have an externalId that is equal to the passed in externalId.
     *
     * @param externalId the externalId the SobaObject is mapped to in an external system.
     * @return List of all matching objects.  An empty list is returned if the passed in externalId was null.
     */
    public List<T> getByExternalId(String externalId) {
        if (externalId == null) {
            return Collections.emptyList();
        }
        Query<T> q = ds.createQuery(entityClazz);
        q.criteria("externalId").equal(externalId);
        return q.asList();
    }
}
