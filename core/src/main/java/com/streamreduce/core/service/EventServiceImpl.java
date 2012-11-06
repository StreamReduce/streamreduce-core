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

package com.streamreduce.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.streamreduce.Constants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.dao.DAODatasourceType;
import com.streamreduce.core.dao.EventDAO;
import com.streamreduce.core.dao.GenericCollectionDAO;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.ObjectWithId;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;
import net.sf.json.JSONObject;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authc.AuthenticationException;
import org.bson.types.ObjectId;
import org.jclouds.domain.LocationScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link EventService}.
 */
@Service("eventService")
public class EventServiceImpl extends AbstractService implements EventService {

    @Autowired
    private EventDAO eventDAO;
    @Autowired
    private ConnectionProviderFactory connectionProviderFactory;
    @Autowired
    private GenericCollectionDAO genericCollectionDAO;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private UserService userService;

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends ObjectWithId> Event createEvent(EventId eventId, T target, Map<String, Object> extraMetadata) {
        Account account = null;
        User user = null;

        try {
            user = securityService.getCurrentUser();
            account = user.getAccount();
        } catch (AuthenticationException ae) {
            // We will not persist any READ_* events when a user is not logged in
            if (eventId == EventId.READ) {
                logger.debug("Anonymous read events are not persisted (" + target + "): " + eventId);
                return null;
            }
        } catch (UnavailableSecurityManagerException e) {
            logger.warn("Unable to derive user from SecurityService.  A User will be derived from the target (" +
                              target + ") if possible.  If not, no event will be persisted", e);
        } catch (Exception nfi) {
            logger.warn("Unknown exception type in EventService", nfi);
        }

        if (extraMetadata == null) {
            extraMetadata = new HashMap<String, Object>();
        }

        // TODO: Figure out a way to make these automatically-generated metadata keys constants somewhere

        // Extend the context with T information
        if (target != null) {
            // Fill out ObjectWithId information
            extraMetadata.put("targetId", target.getId());
            extraMetadata.put("targetVersion", target.getVersion());
            // Fill out type (camel casing of the object type)
            extraMetadata.put("targetType", target.getClass().getSimpleName());

            // Fill out the SobaObject information
            if (target instanceof SobaObject) {
                SobaObject tSobaObject = (SobaObject) target;

                // Attempt to gather the user/account from the target of the event when there is no user logged in.
                // We can only do this for subclasses of SobaObject because the other two objects that create events
                // (Account/User) can only provide one piece of the puzzle.
                if (user == null) {
                    user = tSobaObject.getUser();
                    account = tSobaObject.getAccount();
                }

                extraMetadata.put("targetVisibility", tSobaObject.getVisibility());
                extraMetadata.put("targetAlias", tSobaObject.getAlias());
                extraMetadata.put("targetHashtags", tSobaObject.getHashtags());
            }

            // Fill in specific object information
            if (target instanceof Account) {
                Account tAccount = (Account) target;
                extraMetadata.put("targetFuid", tAccount.getFuid());
                extraMetadata.put("targetName", tAccount.getName());

            } else if (target instanceof InventoryItem) {
                InventoryItem inventoryItem = (InventoryItem)target;
                Connection connection = inventoryItem.getConnection();
                ConnectionProvider tConnectionProvider = connectionProviderFactory.connectionProviderFromId(
                        connection.getProviderId());

                extraMetadata.put("targetExternalId", inventoryItem.getExternalId());
                extraMetadata.put("targetExternalType", inventoryItem.getType());

                extraMetadata.put("targetConnectionId", connection.getId());
                extraMetadata.put("targetConnectionAlias", connection.getAlias());
                extraMetadata.put("targetConnectionHashtags", connection.getHashtags());
                extraMetadata.put("targetConnectionVersion", connection.getVersion());

                extraMetadata.put("targetProviderId", tConnectionProvider.getId());
                extraMetadata.put("targetProviderDisplayName", tConnectionProvider.getDisplayName());
                extraMetadata.put("targetProviderType", tConnectionProvider.getType());

                // Fill in the extra metadata stored in the nodeMetadata
                extraMetadata.putAll(getMetadataFromInventoryItem(inventoryItem));
            } else if (target instanceof Connection) {
                Connection tConnection = (Connection) target;
                ConnectionProvider tConnectionProvider = connectionProviderFactory.connectionProviderFromId(
                        tConnection.getProviderId());

                extraMetadata.put("targetProviderId", tConnectionProvider.getId());
                extraMetadata.put("targetProviderDisplayName", tConnectionProvider.getDisplayName());
                extraMetadata.put("targetProviderType", tConnectionProvider.getType());

            } else if (target instanceof User) {
                User tUser = (User) target;

                extraMetadata.put("targetFuid", tUser.getFuid());
                extraMetadata.put("targetFullname", tUser.getFullname());
                extraMetadata.put("targetUsername", tUser.getUsername());

            } else if (target instanceof SobaMessage) {
                // This is actually already handled in MessageServiceImpl.  This was just put here to help keep track
                // of the different types of objects we're supporting in case we want to do more later.
            }

            // If there is no user/account set and the event is not an Account/User/SobaMessage event, quick return.
            // Otherwise, set the user and/or account based on circumstances unique to each EventId.
            if (user == null) {
                if (!(target instanceof Account) && !(target instanceof User) && !(target instanceof SobaMessage)) {
                    logger.debug("Anonymous SobaObject events are not persisted (" + target + "): " + eventId);
                    return null;
                } else {
                    switch (eventId) {
                        case CREATE:
                            if (target instanceof Account) {
                                account = (Account) target;
                                user = null;
                            } else if (target instanceof User) {
                                account = user.getAccount();
                                user = null; // Nullify because this is a system event
                            } else if (target instanceof SobaMessage) {
                                // If this is a logged in user, no need to try and figure out the user/account
                                if (user != null) {
                                    break;
                                }

                                ObjectId originalTargetId = extraMetadata.get("messageEventTargetId") != null ?
                                        (ObjectId)extraMetadata.get("messageEventTargetId") :
                                        null;
                                Event previousEvent = getLastEventForTarget(originalTargetId);

                                // Try to get the user
                                ObjectId originalEventUserId = extraMetadata.get("messageEventUserId") != null ?
                                        (ObjectId)extraMetadata.get("messageEventUserId") :
                                        null;

                                if (originalEventUserId != null) {
                                    try {
                                        user = userService.getUserById(originalEventUserId);
                                    } catch (UserNotFoundException unfe) {
                                        if (previousEvent != null) {
                                            try {
                                                user = userService.getUserById(previousEvent.getUserId());
                                            } catch (UserNotFoundException unfe2) {
                                                // There is nothing we can do at this point.  Let's log so we can keep
                                                // track of these unrecoverable events
                                                logger.error("Unable to identify the sender of the SobaMessage " +
                                                                     "having an id of " + target.getId());
                                            }
                                        }
                                    }
                                }

                                // Try to get the account
                                ObjectId originalEventAccountId = extraMetadata.get("messageEventAccountId") != null ?
                                        (ObjectId)extraMetadata.get("messageEventAccountId") :
                                        null;

                                if (originalEventAccountId != null) {
                                    try {
                                        account = userService.getAccount(originalEventAccountId);
                                    } catch (AccountNotFoundException anfe) {
                                        if (previousEvent != null) {
                                            try {
                                                account = userService.getAccount(previousEvent.getAccountId());
                                            } catch (AccountNotFoundException anfe2) {
                                                // There is nothing we can do at this point.  Let's log so we can
                                                // keep track of these unrecoverable events
                                                logger.error("Unable to identify the sender of the SobaMessage " +
                                                                     "having an id of " + target.getId());
                                            }
                                        }
                                    }
                                }
                            }
                            break;

                        case CREATE_USER_REQUEST:
                            account = null; // Nullify so this doesn't get associated with the admin user's account
                            break;

                        case USER_PASSWORD_RESET_REQUEST:
                            user = (User) target;
                            account = user.getAccount();
                            break;

                        case READ:
                        case UPDATE:
                        case DELETE:
                        case DELETE_USER_INVITE_REQUEST:
                        case CREATE_USER_INVITE_REQUEST:
                        case USER_MESSAGE:
                            if (eventId == EventId.DELETE && target instanceof Account) {
                                account = (Account) target;
                            } else {
                                // Both account and user should be set so if they aren't, quick return as these must be
                                // system events
                                logger.warn("Unexpected anonymous Account/User/SobaMessage event not persisted (" +
                                                    target + "): " + eventId);
                                return null;
                            }
                    }
                }
            }
        }

        // Extend the context with User information
        if (user != null) {
            extraMetadata.put("sourceAlias", user.getAlias());
            extraMetadata.put("sourceFuid", user.getFuid());
            extraMetadata.put("sourceFullname", user.getFullname());
            extraMetadata.put("sourceUsername", user.getUsername());
            extraMetadata.put("sourceVersion", user.getVersion());
        }

        // Extend the context with Account information
        if (account != null) {
            extraMetadata.put("accountFuid", account.getFuid());
            extraMetadata.put("accountName", account.getName());
            extraMetadata.put("accountVersion", account.getVersion());
        }

        // Convert JSONObject entries to BasicDBObject to avoid MongoDB serialization issues
        for (Map.Entry<String, Object> metadataEntry : extraMetadata.entrySet()) {
            String key = metadataEntry.getKey();
            Object rawValue = metadataEntry.getValue();

           if (rawValue instanceof JSONObject) {
               extraMetadata.put(key, JSON.parse(rawValue.toString()));
           }
        }

        return logAndSaveEvent(new Event.Builder()
                .eventId(eventId)
                .accountId(account != null ? account.getId() : null)
                .actorId(user != null ? user.getId() : null)
                .targetId(target != null ? target.getId() : null)
                .context(extraMetadata)
                .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> getEventsForAccount(Account account) {
        return eventDAO.forAccount(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> getAllEvents() {
        return eventDAO.allEvents();
    }

    /**
     * Helper method to get the last {@link Event} based on an object id.
     *
     * @param targetId the target id
     * @return the event or null if there is none
     */
    private Event getLastEventForTarget(ObjectId targetId) {
        return eventDAO.previousTargetEvent(targetId);
    }

    /**
     * Helper method that returns all metadata for a {@link InventoryItem}.
     *
     * @param inventoryItem the cloud inventory item to retrieve metadata for/about
     * @return the metadata
     */
    private Map<String, Object> getMetadataFromInventoryItem(InventoryItem inventoryItem) {
        // NOTE: We're not using CloudService methods here for performance reasons
        Map<String, Object> civMetadata = new HashMap<String, Object>();

        // Right now, we are only creating extended metadata for AWS EC2 instance items
        if (inventoryItem.getConnection().getProviderId().equals(ProviderIdConstants.AWS_PROVIDER_ID) &&
                inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE)) {
            DBObject cMetadata = genericCollectionDAO.getById(DAODatasourceType.BUSINESS,
                                                             Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                                             inventoryItem.getMetadataId());

            if (cMetadata == null) {
                // Fill in the metadata based on the last event for this target
                Event previousEvent = getLastEventForTarget(inventoryItem.getId());

                if (previousEvent != null) {
                    Map<String, Object> peMetadata = previousEvent.getMetadata();

                    if (peMetadata != null) {
                        civMetadata.put("targetIP", peMetadata.get("targetIP"));
                        civMetadata.put("targetOS", peMetadata.get("targetOS"));
                        civMetadata.put("targetISO3166Code", peMetadata.get("targetISO3166Code"));
                        civMetadata.put("targetRegion", peMetadata.get("targetRegion"));
                        civMetadata.put("targetZone", peMetadata.get("targetZone"));
                    }
                }
            } else {
                // Fill in the metadata from the available node metadata

                // Get the IP address
                if (cMetadata.containsField("publicAddresses")) {
                    BasicDBList publicAddresses = (BasicDBList) cMetadata.get("publicAddresses");

                    // TODO: How do we want to handle multiple IP addresses?
                    if (publicAddresses.size() > 0) {
                        civMetadata.put("targetIP", publicAddresses.get(0));
                    }
                }

                // Get location information (ISO 3166 code, region and availability zone)
                if (cMetadata.containsField("location") && cMetadata.get("location") != null) {
                    BasicDBObject location = (BasicDBObject) cMetadata.get("location");
                    boolean regionProcessed = false;
                    boolean zoneProcessed = false;

                    while (location != null) {
                        if (regionProcessed && zoneProcessed) {
                            break;
                        }

                        String locationScope = location.containsField("scope") ? location.getString("scope") : null;

                        if (locationScope != null) {
                            LocationScope scope = LocationScope.valueOf(locationScope);

                            switch (scope) {
                                case REGION:
                                    civMetadata.put("targetRegion", location.get("id"));
                                    regionProcessed = true;
                                    break;
                                case ZONE:
                                    BasicDBList iso3166Codes = (BasicDBList) location.get("iso3166Codes");

                                    civMetadata.put("targetISO3166Code", iso3166Codes.get(0));
                                    civMetadata.put("targetZone", location.get("id"));
                                    zoneProcessed = true;
                                    break;
                            }
                        }

                        location = location.containsField("parent") && location.get("parent") != null ?
                                (BasicDBObject) location.get("parent") :
                                null;
                    }
                }

                // Get OS name
                if (cMetadata.containsField("operatingSystem")) {
                    BasicDBObject operatingSystem = (BasicDBObject) cMetadata.get("operatingSystem");

                    if (operatingSystem != null) {
                        if (operatingSystem.containsField("family")) {
                            civMetadata.put("targetOS", operatingSystem.get("family"));
                        }
                    }
                }
            }
        }

        return civMetadata;
    }

    /**
     * Logs the event (DEBUG) and then persists the {@link Event}.
     *
     * @param event the event to log and persist
     * @return the event after being persisted
     */
    private Event logAndSaveEvent(Event event) {
        logger.debug(event.toString());

        eventDAO.save(event);

        return event;
    }

}
