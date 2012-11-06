package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.streamreduce.core.model.EventLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository("eventLogDAO")
public class EventLogDAO extends ValidatingDAO<EventLog> {

    @Autowired
    protected EventLogDAO(@Qualifier(value = "businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

}
