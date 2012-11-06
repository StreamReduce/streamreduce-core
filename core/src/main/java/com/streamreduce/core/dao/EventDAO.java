package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Event;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DAO for accessing {@link Event} objects.
 */
@Repository("eventDAO")
public class EventDAO extends ValidatingDAO<Event> {

    @Autowired
    protected EventDAO(@Qualifier(value = "messageDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public List<Event> forAccount(Account account) {
        Query<Event> query = createQuery();

        query.field("accountId").equal(account != null ? account.getId() : null);

        return query.asList();
    }

    public List<Event> allEvents() {
        return find().asList();
    }

    public Event previousTargetEvent(ObjectId targetId) {
        Query<Event> query = createQuery();

        query.field("targetId").equal(targetId);

        return query.order("-timestamp").limit(1).get();
    }

}
