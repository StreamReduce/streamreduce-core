package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.streamreduce.core.model.SystemInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository("systemInfoDAO")
public class SystemInfoDAO extends ValidatingDAO<SystemInfo> {

    @Autowired
    protected SystemInfoDAO(@Qualifier(value = "businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public SystemInfo getLatest() {
        return ds.find(entityClazz).get();
    }

}
