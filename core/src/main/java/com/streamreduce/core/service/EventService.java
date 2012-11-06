package com.streamreduce.core.service;

import java.util.List;
import java.util.Map;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.ObjectWithId;

public interface EventService {

    /**
     * Create an event and returns the saved {@link Event} object.  (Also fills in some default
     * context information based on the objects involved in the event.)  All events will use the currently logged in
     * account/user.  If the event doesn't support having a null userId, null will be returned.  For a list of
     * {@link EventId}s where userId can be null and will be populated accordingly, look below:
     *
     * <b>{@link EventId}s Allowing Null User ID</b>
     * <ul>
     *  <li>{@link EventId#CREATE} (Inventory Item)</li>
     *  <li>{@link EventId#UPDATE} (Inventory Item)</li>
     *  <li>{@link EventId#DELETE} (Inventory Item)</li>
     *  <li>{@link EventId#ACTIVITY}</li>
     *  <li>{@link EventId#CREATE_GLOBAL_MESSAGE}</li>
     * </ul>
     *
     * @param eventId the event id
     * @param target the target of the event
     * @param extraMetadata extra context (There is no special handling of key conflicts)
     *
     * @return the event created or null if the event does not support having a null userId
     */
    public <T extends ObjectWithId> Event createEvent(EventId eventId, T target, Map<String, Object> extraMetadata);

    /**
     * Get list of events for a particular account, or all "global" events if the account is null.
     *
     * @param account the account to retrieve events for
     *
     * @return the list of events
     */
    public List<Event> getEventsForAccount(Account account);

    /**
     * Get list of all events.
     *
     * @return the list of events
     */
    public List<Event> getAllEvents();

}
