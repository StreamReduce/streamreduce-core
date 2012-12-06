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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.Constants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.CommandNotAllowedException;
import com.streamreduce.core.dao.DAODatasourceType;
import com.streamreduce.core.dao.GenericCollectionDAO;
import com.streamreduce.core.dao.InventoryItemDAO;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.details.feed.FeedEntryDetails;
import com.streamreduce.core.model.messages.details.jira.JiraActivityDetails;
import com.streamreduce.core.model.messages.details.pingdom.PingdomEntryDetails;
import com.streamreduce.core.model.messages.details.twitter.TwitterActivityDetails;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.core.service.exception.InventoryItemNotFoundException;
import com.streamreduce.util.AWSClient;
import com.streamreduce.util.ExternalIntegrationClient;
import com.streamreduce.util.FeedClient;
import com.streamreduce.util.GitHubClient;
import com.streamreduce.util.GoogleAnalyticsClient;
import com.streamreduce.util.HashtagUtil;
import com.streamreduce.util.JSONUtils;
import com.streamreduce.util.JiraClient;
import com.streamreduce.util.MessageUtils;
import com.streamreduce.util.PingdomClient;
import com.streamreduce.util.TwitterClient;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.jclouds.aws.ec2.domain.Tag;
import org.jclouds.cloudwatch.CloudWatchAsyncApi;
import org.jclouds.cloudwatch.CloudWatchApi;
import org.jclouds.cloudwatch.domain.*;
import org.jclouds.cloudwatch.features.MetricApi;
import org.jclouds.compute.domain.ComputeType;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.rest.RestContext;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.xml.namespace.QName;

/**
 * Implementation of {@link InventoryService}.
 */
@Service("inventoryService")
public class InventoryServiceImpl implements InventoryService {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Unit> ec2CloudWatchMetricNames = null;
    private Set<Statistics> ec2CloudWatchStatisticsSet = ImmutableSet.of(
            Statistics.AVERAGE,
            Statistics.MINIMUM,
            Statistics.MAXIMUM
    );
    private final Cache<ObjectId, ExternalIntegrationClient> externalClientCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .removalListener(new RemovalListener<ObjectId, ExternalIntegrationClient>() {
                            /**
                             * {@inheritDoc}
                             */
                            @Override
                            public void onRemoval(RemovalNotification<ObjectId, ExternalIntegrationClient> n) {
                                ExternalIntegrationClient client = n.getValue();

                                if (client != null) {
                                    logger.debug("Cleaning up ExternalIntegrationClient [" + client.getConnectionId() + "]");
                                    client.cleanUp();
                                }
                            }
                        }).build();

    @Autowired
    InventoryItemDAO inventoryItemDAO;
    @Autowired
    GenericCollectionDAO genericCollectionDAO;
    @Autowired
    EmailService emailService;
    @Autowired
    EventService eventService;
    @Autowired
    MessageService messageService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicDBObject getInventoryItemPayload(InventoryItem inventoryItem) {
        return genericCollectionDAO.getById(DAODatasourceType.BUSINESS,
                                            Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                            inventoryItem.getMetadataId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventoryItem createInventoryItem(Connection connection, JSONObject json)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        Preconditions.checkNotNull(connection, "connection cannot be null.");
        Preconditions.checkNotNull(json, "json cannot be null.");

        String providerId = connection.getProviderId();
        InventoryItem inventoryItem = new InventoryItem();

        inventoryItem.setAccount(connection.getAccount());
        inventoryItem.setUser(connection.getUser());
        inventoryItem.setConnection(connection);
        inventoryItem.addHashtag(connection.getProviderId());
        inventoryItem.setDeleted(false);

        // Default to visibility of the connection (Should be overridden below if required)
        inventoryItem.setVisibility(connection.getVisibility());

        // No other way...  polymorphism is Hard...
        if (providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
            extendAWSInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.GITHUB_PROVIDER_ID)) {
            extendGitHubInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.GOOGLE_ANALYTICS_PROVIDER_ID)) {
            extendGoogleAnalyticsInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.JIRA_PROVIDER_ID)) {
            extendJiraInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.PINGDOM_PROVIDER_ID)) {
            extendPingdomInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.CUSTOM_PROVIDER_ID)) {
            extendGenericInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.NAGIOS_PROVIDER_ID)) {
            extendGenericInventoryItem(inventoryItem, json);
        } else {
            throw new IllegalArgumentException(providerId + " does not support creating inventory items.");
        }

        // Create metadata
        DBObject metadataEntry =
                genericCollectionDAO.createCollectionEntry(DAODatasourceType.BUSINESS,
                                                           Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                                           json.toString());

        inventoryItem.setMetadataId((ObjectId)metadataEntry.get("_id"));

        // Persist the inventory item
        inventoryItemDAO.save(inventoryItem);

        // Create the event
        Event event = eventService.createEvent(EventId.CREATE, inventoryItem, null);

        // Create the message
        messageService.sendInventoryMessage(event, inventoryItem);

        return inventoryItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventoryItem updateInventoryItem(InventoryItem inventoryItem, JSONObject json)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");
        Preconditions.checkNotNull(json, "json cannot be null.");

        Connection connection = inventoryItem.getConnection();
        String providerId = connection.getProviderId();

        // No other way...
        if (providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
            extendAWSInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.GITHUB_PROVIDER_ID)) {
            extendGitHubInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.GOOGLE_ANALYTICS_PROVIDER_ID)) {
            extendGoogleAnalyticsInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.JIRA_PROVIDER_ID)) {
            extendJiraInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.CUSTOM_PROVIDER_ID)) {
            extendGenericInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.NAGIOS_PROVIDER_ID)) {
            extendGenericInventoryItem(inventoryItem, json);
        } else if (providerId.equals(ProviderIdConstants.PINGDOM_PROVIDER_ID)) {
            extendPingdomInventoryItem(inventoryItem, json);
        } else {
            throw new IllegalArgumentException(providerId + " does not support creating inventory items.");
        }

        // Change visibility if necessary?

        // Update metadata
        try {
            genericCollectionDAO.updateCollectionEntry(DAODatasourceType.BUSINESS,
                                                       Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                                       inventoryItem.getMetadataId(), json.toString());
        } catch (Exception e) {
            // Should never happen
            logger.error("Error updating project hosting inventory item cache: " + inventoryItem.getId(), e);
        }

        // Send a silent update if none of our internal properties have changed
        boolean silentUpdate = false;
        InventoryItem oldInventoryItem;

        try {
            oldInventoryItem = getInventoryItem(inventoryItem.getId());
        } catch (InventoryItemNotFoundException e) {
            // Should never happen but just in case
            oldInventoryItem = null;
        }

        // Be silent unless alias, description, hashtags or visibility are different.  (None of the other properties
        // should be changeable externally or via our exposed APIs.)
        if (oldInventoryItem != null) {
            silentUpdate = (Objects.equal(oldInventoryItem.getAlias(), inventoryItem.getAlias()) &&
                    Objects.equal(oldInventoryItem.getDescription(), inventoryItem.getDescription()) &&
                    Objects.equal(oldInventoryItem.getHashtags(), inventoryItem.getHashtags()) &&
                    Objects.equal(oldInventoryItem.getVisibility(), inventoryItem.getVisibility()));
        }

        // Persist the inventory item
        return updateInventoryItem(inventoryItem, silentUpdate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventoryItem updateInventoryItem(InventoryItem inventoryItem, boolean silentUpdate) {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");

        inventoryItemDAO.save(inventoryItem);

        if (!silentUpdate) {
            // Create the event
            Event event = eventService.createEvent(EventId.UPDATE, inventoryItem, null);

            // Create the message
            messageService.sendInventoryMessage(event, inventoryItem);
        }

        return inventoryItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInventoryItem(InventoryItem inventoryItem) {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");

        // Delete the metadata entry
        genericCollectionDAO.removeCollectionEntry(DAODatasourceType.BUSINESS,
                                                   Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                                   inventoryItem.getMetadataId());

        inventoryItemDAO.delete(inventoryItem);

        // If the item was already marked as deleted, do not resend the event/message
        if (inventoryItem.isDeleted()) {
            // Create the event
            Event event = eventService.createEvent(EventId.DELETE,inventoryItem,null);

            // Create the message
            messageService.sendInventoryMessage(event, inventoryItem);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markInventoryItemDeleted(InventoryItem inventoryItem) {
        inventoryItem.setDeleted(true);

        inventoryItemDAO.save(inventoryItem);

        // Create the event
        Event event = eventService.createEvent(EventId.DELETE,inventoryItem,null);

        // Create the message
        messageService.sendInventoryMessage(event, inventoryItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InventoryItem> getInventoryItems(Connection connection) {
        return getInventoryItems(connection, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InventoryItem> getInventoryItems(Connection connection, User user) {
        List<InventoryItem> inventoryItems = inventoryItemDAO.getInventoryItems(connection, user);

        for (InventoryItem inventoryItem : inventoryItems) {
            // Create the event
            eventService.createEvent(EventId.READ,
                                     inventoryItem,
                                     null);
        }

        return inventoryItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InventoryItem> getInventoryItems(ObjectId connectionId) {
        List<InventoryItem> inventoryItems = inventoryItemDAO.getInventoryItems(connectionId);

        for (InventoryItem inventoryItem : inventoryItems) {
            // Create the event
            eventService.createEvent(EventId.READ,
                                     inventoryItem,
                                     null);
        }

        return inventoryItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InventoryItem> getInventoryItemsForExternalId(String externalId) {
        List<InventoryItem> inventoryItems = inventoryItemDAO.getByExternalIdNotDeleted(externalId);

        for (InventoryItem inventoryItem : inventoryItems) {
            // Create the event
            eventService.createEvent(EventId.READ,
                                     inventoryItem,
                                     null);
        }

        return inventoryItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventoryItem getInventoryItem(ObjectId objectId) throws InventoryItemNotFoundException {
        InventoryItem inventoryItem = inventoryItemDAO.get(objectId);

        if (inventoryItem == null) {
            throw new InventoryItemNotFoundException(objectId.toString());
        }

        // Create the event
        eventService.createEvent(EventId.READ,
                                 inventoryItem,
                                 null);

        return inventoryItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventoryItem getInventoryItemForExternalId(Connection connection, String externalId)
            throws InventoryItemNotFoundException {
        Preconditions.checkNotNull(connection, "connection cannot be null.");
        Preconditions.checkNotNull(externalId, "externalId cannot be null.");

        InventoryItem inventoryItem = inventoryItemDAO.getInventoryItem(connection, externalId);

        if (inventoryItem == null) {
            throw new InventoryItemNotFoundException("No inventory item for connection [" + connection.getId() + "] " +
                    "with the given externalId of " + externalId);
        }

        // Create the event
        eventService.createEvent(EventId.READ,
                                 inventoryItem,
                                 null);

        return inventoryItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshInventoryItemCache(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        Preconditions.checkNotNull(connection, "connection cannot be null.");

        logger.debug("Updating inventory item cache for connection [" + connection.getId() + "]: " +
                             connection.getAlias());

        List<String> processedKeys = new ArrayList<>();
        List<JSONObject> externalInventoryItems;
        ExternalIntegrationClient client = getClient(connection);

        if (client instanceof AWSClient) {
            AWSClient awsClient = null;
            try {
                awsClient = new AWSClient(connection);
                // Get the EC2 inventory items
                externalInventoryItems = (awsClient.getEC2Instances());
                // Get the S3 inventory items
                externalInventoryItems.addAll(awsClient.getS3BucketsAsJson());
            } finally {
                if (awsClient != null) {
                    awsClient.cleanUp();
                }
            }
        } else if (client instanceof GitHubClient) {
            externalInventoryItems = ((GitHubClient) client).getRepositories();
        } else if (client instanceof GoogleAnalyticsClient) {
            externalInventoryItems = ((GoogleAnalyticsClient) client).getProfiles();
        } else if (client instanceof JiraClient) {
            externalInventoryItems = ((JiraClient)client).getProjects(false);
        }  else if (client instanceof PingdomClient) {
            externalInventoryItems = ((PingdomClient)client).checks();
        } else if (client instanceof FeedClient) {
            return;
        } else if (client instanceof TwitterClient) {
            return;
        } else {
            throw new IllegalArgumentException(client.getClass().getName() + " is not a supported external client.");
        }

        logger.debug("  Provider id: " + connection.getProviderId());
        logger.debug("  Inventory items found: " + externalInventoryItems.size());

        for (JSONObject json : externalInventoryItems) {
            InventoryItem inventoryItem = null;
            JSONObject externalInventoryItemAsJSON;
            String externalId;

            if (client instanceof AWSClient) {
                String type = json.getString("type");

                if (type.equals(ComputeType.NODE.toString())) {
                    externalId = json.getString("providerId");
                } else {
                    externalId = json.getString("name");
                }
                externalInventoryItemAsJSON = json;
            } else if (client instanceof GitHubClient) {
                externalId = json.getJSONObject("owner").getString("login") + "/" + json.getString("name");
                externalInventoryItemAsJSON = json;
            } else if (client instanceof PingdomClient) {
                externalId = json.getString("id");
                externalInventoryItemAsJSON = json;
            } else if (client instanceof GoogleAnalyticsClient) {
                externalId = json.getString("id");
                externalInventoryItemAsJSON = json;
            } else {
                externalId = json.getString("key");
                try {
                    externalInventoryItemAsJSON = ((JiraClient)client).getProjectDetails(externalId);
                } catch (InvalidCredentialsException | IOException e) {
                    // This should never happen as we've already validated the connection by this point
                    logger.warn("Unable to get the Jira project details for " + externalId + ": " + e.getMessage());
                    return;
                }
            }

            try {
                inventoryItem = getInventoryItemForExternalId(connection, externalId);
            } catch (InventoryItemNotFoundException e) {
                // This is more than possible and is handled below
            }

            if (inventoryItem == null || inventoryItem.isDeleted()) {
                if (inventoryItem != null && inventoryItem.isDeleted()) {
                    // Permanently remove the current inventory item marked as deleted.  This can happen if you mark
                    // an inventory item as deleted (due to external deletion) and then recreate the same inventory
                    // item externally before the clean process (ran after refresh) could occur.
                    deleteInventoryItem(inventoryItem);
                }

                createInventoryItem(connection, externalInventoryItemAsJSON);
            } else if (!processedKeys.contains(externalId)) {
                updateInventoryItem(inventoryItem, externalInventoryItemAsJSON);
            }

            processedKeys.add(externalId);
        }

        // Handle inventory items deleted externally
        List<InventoryItem> inventoryItems = getInventoryItems(connection);

        for (InventoryItem inventoryItem : inventoryItems) {
            if (processedKeys.contains(inventoryItem.getExternalId())) {
                continue;
            }

            // Just mark the item as deleted.  Future polling jobs will remove the inventory item marked as deleted
            // when appropriate.
            if (!inventoryItem.isDeleted()) {
                markInventoryItemDeleted(inventoryItem);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pullInventoryItemActivity(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        Preconditions.checkNotNull(connection, "connection cannot be null.");

        String providerId = connection.getProviderId();

        // TODO: pullGitHubActivity and pullJiraActivity could be simplified

        // No other way...
        if (providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
            pullEC2CloudWatchMetrics(connection);
            // AWS S3 doesn't do CloudWatch metrics so we can tackle "metrics" when we do the same for GitHub/Jira
        } else if (providerId.equals(ProviderIdConstants.GITHUB_PROVIDER_ID)) {
            pullGitHubActivity(connection);
        } else if (providerId.equals(ProviderIdConstants.GOOGLE_ANALYTICS_PROVIDER_ID)) {
            pullGoogleAnalyticsActivity(connection);
        } else if (providerId.equals(ProviderIdConstants.JIRA_PROVIDER_ID)) {
            pullJiraActivity(connection);
        } else if (providerId.equals(ProviderIdConstants.PINGDOM_PROVIDER_ID)) {
            pullPingdomActivity(connection);
        } else if (providerId.equals(ProviderIdConstants.FEED_PROVIDER_ID)) {
            pullFeedActivity(connection);
        } else if (providerId.equals(ProviderIdConstants.TWITTER_PROVIDER_ID)) {
            pullTwitterActivity(connection);
        } else {
            throw new IllegalArgumentException(providerId + " does not support polling for activity.");
        }
    }

    /* HELPERS METHODS */

    /**
     * {@inheritDoc}
     */
    @Override
    public String getComputeInstanceIPAddress(InventoryItem inventoryItem) {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");
        Preconditions.checkArgument(inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE),
                                    "Only inventory items of type '" + Constants.COMPUTE_INSTANCE_TYPE +
                                            "' can have an OS name.");

        DBObject nodeMetadata = genericCollectionDAO.getById(DAODatasourceType.BUSINESS,
                                                             Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                                             inventoryItem.getMetadataId());

        return getComputeInstanceIPAddress(nodeMetadata != null ?
                                                   JSONObject.fromObject(nodeMetadata.toString()) :
                                                   null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getComputeInstanceIPAddress(JSONObject json) {
        Preconditions.checkNotNull(json, "json cannot be null.");
        Preconditions.checkArgument(json.getString("type").equals(ComputeType.NODE.toString()),
                                    "Only inventory items of type '" + Constants.COMPUTE_INSTANCE_TYPE +
                                            "' can have an OS name.");

        String ipAddress = null;

        if (json.containsKey("publicAddresses")) {
            JSONArray publicAddresses = json.getJSONArray("publicAddresses");

            // TODO: How do we want to handle multiple IP addresses?
            if (publicAddresses.size() > 0) {
                ipAddress = (String) publicAddresses.get(0);
            }
        }

        return ipAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getComputeInstanceOSName(InventoryItem inventoryItem) {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");
        Preconditions.checkArgument(inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE),
                                    "Only inventory items of type '" + Constants.COMPUTE_INSTANCE_TYPE +
                                            "' can have an OS name.");

        DBObject nodeMetadata = genericCollectionDAO.getById(DAODatasourceType.BUSINESS,
                                                             Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                                             inventoryItem.getMetadataId());

        return getComputeInstanceOSName(nodeMetadata != null ? JSONObject.fromObject(nodeMetadata.toString()) : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getComputeInstanceOSName(JSONObject json) {
        Preconditions.checkNotNull(json, "json cannot be null.");
        Preconditions.checkArgument(json.getString("type").equals(ComputeType.NODE.toString()),
                                    "Only inventory items of type '" + Constants.COMPUTE_INSTANCE_TYPE +
                                            "' can have an OS name.");

        String osName = "UNKNOWN";

        if (json.containsKey("operatingSystem")) {
            JSONObject operatingSystem = json.getJSONObject("operatingSystem");

            if (operatingSystem != null && operatingSystem.containsKey("family")) {
                String rawOSName = operatingSystem.getString("family");
                if (rawOSName != null && !(rawOSName.equalsIgnoreCase("UNRECOGNIZED"))) {
                    osName = rawOSName;
                }
            }
        }

        return osName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getLocationByScope(InventoryItem inventoryItem, LocationScope scope) {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");
        Preconditions.checkNotNull(scope, "scope cannot be null.");

        DBObject nodeMetadata = genericCollectionDAO.getById(DAODatasourceType.BUSINESS,
                                                             Constants.INVENTORY_ITEM_METADATA_COLLECTION_NAME,
                                                             inventoryItem.getMetadataId());

        return getLocationByScope(nodeMetadata != null ? JSONObject.fromObject(nodeMetadata.toString()) : null, scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getLocationByScope(JSONObject json, LocationScope scope) {
        Preconditions.checkNotNull(json, "json cannot be null.");
        Preconditions.checkNotNull(scope, "scope cannot be null.");
        Preconditions.checkArgument(json.containsKey("location"), "json must contain a 'location' attribute.");

        JSONObject locationObject = json.containsKey("location") && json.get("location") != null ?
                json.getJSONObject("location") : null;
        Location location = null;

        while (locationObject != null) {
            if (locationObject.containsKey("scope")) {
                String locationScope = locationObject.getString("scope");

                if (locationScope != null) {
                    if (scope == LocationScope.valueOf(locationScope)) {
                        location = buildLocationFromJSON(locationObject);
                        break;
                    }
                }
            }

            locationObject = locationObject.containsKey("parent") && locationObject.get("parent") !=  null ?
                    locationObject.getJSONObject("parent") :
                    null;
        }

        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHashtag(InventoryItem inventoryItem, SobaObject tagger, String tag) {
        inventoryItem.addHashtag(tag);

        handleHashtagEvent(EventId.HASHTAG_ADD, inventoryItem, tagger, tag);

        updateInventoryItem(inventoryItem, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeHashtag(InventoryItem inventoryItem, SobaObject tagger, String tag) {
        inventoryItem.removeHashtag(tag);

        handleHashtagEvent(EventId.HASHTAG_DELETE, inventoryItem, tagger, tag);

        updateInventoryItem(inventoryItem, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebootComputeInstance(InventoryItem inventoryItem)
            throws CommandNotAllowedException, InvalidCredentialsException {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");
        Preconditions.checkArgument(inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE),
                                    "Inventory item of type '" + inventoryItem.getType() + "' cannot be rebooted.");

        AWSClient client = (AWSClient)getClient(inventoryItem.getConnection());

        logger.debug("Rebooting node: " + inventoryItem.getExternalId());

        BasicDBObject payload = getInventoryItemPayload(inventoryItem);
        String jcloudsNodeId = payload.getString("id");
        NodeMetadata nodeMetadata = client.getEC2Instance(jcloudsNodeId);

        if (nodeMetadata.getStatus().equals(NodeMetadata.Status.TERMINATED)) {
            throw new CommandNotAllowedException("You cannot reboot a terminated node.");
        }

        EventId eventId;

        if (client.rebootEC2Instance(jcloudsNodeId)) {
            eventId = EventId.CLOUD_INVENTORY_ITEM_REBOOT;
        } else {
            // TODO: Handle this issue but it can be a false positive if the time it takes surpasses the time we wait
            eventId = EventId.CLOUD_INVENTORY_ITEM_REBOOT_FAILURE;
        }

        // Create the event
        Event event = eventService.createEvent(eventId, inventoryItem, null);
        // Create the message
        messageService.sendInventoryMessage(event, inventoryItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyComputeInstance(InventoryItem inventoryItem) throws InvalidCredentialsException {
        Preconditions.checkNotNull(inventoryItem, "inventoryItem cannot be null.");
        Preconditions.checkArgument(inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE),
                                    "Inventory item of type '" + inventoryItem.getType() + "' cannot be destroyed.");

        AWSClient client = (AWSClient)getClient(inventoryItem.getConnection());

        logger.debug("Terminating node: " + inventoryItem.getExternalId());

        BasicDBObject payload = getInventoryItemPayload(inventoryItem);
        String jcloudsNodeId = payload.getString("id");
        NodeMetadata nodeMetadata = client.getEC2Instance(jcloudsNodeId);

        if (nodeMetadata.getStatus().equals(NodeMetadata.Status.TERMINATED)) {
            return;
        }

        EventId eventId;

        if (client.destroyEC2Instance(jcloudsNodeId)) {
            eventId = EventId.CLOUD_INVENTORY_ITEM_TERMINATE;
        } else {
            // TODO: Handle this issue but it can be a false positive if the time it takes surpasses the time we wait
            eventId = EventId.CLOUD_INVENTORY_ITEM_TERMINATE_FAILURE;
        }

        // Create the event
        Event event = eventService.createEvent(eventId, inventoryItem, null);
        // Create the message
        messageService.sendInventoryMessage(event, inventoryItem);
    }

    /* PRIVATE METHODS */

    /**
     * Takes the passed in JSON and creates a {@link Location} from it.  (Does not include parents or children.)
     *
     * @param json the JSON that should represent a location
     *
     * @return the location
     */
    private Location buildLocationFromJSON(JSONObject json) {
        LocationBuilder builder = new LocationBuilder();
        Set<String> iso3166Codes = new HashSet<>();
        Map<String, Object> metadata = new HashMap<>();

        if (json.containsKey("iso3166Codes")) {
            for (Object rawCode : json.getJSONArray("iso3166Codes")) {
                iso3166Codes.add((String)rawCode);
            }
        }

        if (json.containsKey("metadata")) {
            JSONObject rawMetadata = json.getJSONObject("metadata");

            for (Object rawKey : rawMetadata.keySet()) {
                metadata.put((String)rawKey, rawMetadata.get(rawKey));
            }
        }


        builder.description(json.containsKey("description") ? json.getString("description") : null)
               .iso3166Codes(iso3166Codes)
               .id(json.containsKey("id") ? json.getString("id") : null)
               .scope(json.containsKey("scope") ? LocationScope.valueOf(json.getString("scope")) : null)
               .metadata(metadata);

        return builder.build();
    }

    /**
     * Takes the inventory item already constructed in
     * {@link #createInventoryItem(com.streamreduce.core.model.Connection, net.sf.json.JSONObject)} and updates its
     * information with data in JSON.
     *
     * @param inventoryItem the inventory item
     * @param json the json representing the inventory item (from jclouds)
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    private void extendAWSInventoryItem(InventoryItem inventoryItem, JSONObject json)
            throws InvalidCredentialsException {
        String externalType = json.getString("type");
        Connection connection = inventoryItem.getConnection();
        String externalId = inventoryItem.getExternalId();
        String name = inventoryItem.getAlias();
        String internalType;

        if (externalId == null) {
            if (externalType.equals(ComputeType.NODE.toString())) {
                externalId = json.getString("providerId");
            } else {
                externalId = json.getString("name");
            }
        }

        if (externalType.equals(ComputeType.NODE.toString())) {
            name = json.getString("name");
            AWSClient client = null;
            try {
                client = new AWSClient(inventoryItem.getConnection());
                Set<Tag> ec2Tags = client.getEC2InstanceTags(externalId);

                // Handle adding new hashtags based on the EC2 tags and the instance name
                for (Tag tag : ec2Tags) {
                    String key = tag.getKey();
                    String value = tag.getValue();

                    if (key.equals("Name")) {
                        if (value.length() > 0) {
                            name = value;
                        }
                    } else {
                        inventoryItem.addHashtag(key + "=" + value);
                    }
                }
            } finally {
                if (client != null) {
                    client.cleanUp();
                }
            }

            internalType = Constants.COMPUTE_INSTANCE_TYPE;
        } else {
            internalType = Constants.BUCKET_TYPE;
        }

        if (!StringUtils.hasText(name)) {
            name = externalId;
        }

        inventoryItem.setAlias(name);
        inventoryItem.setExternalId(externalId);
        inventoryItem.setType(internalType);

        inventoryItem.addHashtag(externalId);
        inventoryItem.addHashtag(internalType);

        // For all of AWS, we add hashtags for the availability zone and region
        Location region = getLocationByScope(json, LocationScope.REGION);
        Location zone = getLocationByScope(json, LocationScope.ZONE);

        if (region != null) {
            inventoryItem.addHashtag(region.getId());
        }
        if (zone != null) {
            inventoryItem.addHashtag(zone.getId());
        }

        // For AWS EC2, we add OS name
        if (externalType.equals(ComputeType.NODE.toString())) {
            String osName = getComputeInstanceOSName(json);

            if (osName != null) {
                inventoryItem.addHashtag(getComputeInstanceOSName(json));
            }
        }
    }

    /**
     * Takes the inventory item already constructed in
     * {@link #createInventoryItem(com.streamreduce.core.model.Connection, net.sf.json.JSONObject)} and updates its
     * information with data in JSON.
     *
     * @param inventoryItem the inventory item
     * @param json the json representing the inventory item (from jclouds)
     */
    private void extendGitHubInventoryItem(InventoryItem inventoryItem, JSONObject json) {
        boolean isFork = json.getBoolean("fork");
        boolean isPrivate = json.getBoolean("private");
        String language = json.getString("language");
        String name = json.getString("name");
        String owner = json.getJSONObject("owner").getString("login");
        String externalId = owner + "/" + name;
        String description = json.getString("description");

        if (description.length() > 256) {
            description = description.substring(0, 255);
        }

        inventoryItem.setAlias(externalId);
        inventoryItem.setDescription(description);
        inventoryItem.setExternalId(externalId);
        inventoryItem.setType(Constants.PROJECT_TYPE);

        // Override default visibility, we default to hidden if the repository is private
        if (isPrivate) {
            inventoryItem.setVisibility(SobaObject.Visibility.SELF);
        }

        // For GitHub, we add hashtags indicating the language and whether or not it is a fork
        if (isFork) {
            inventoryItem.addHashtag("fork");
        }

        if (StringUtils.hasText(language) && !language.equals("null")) {
            inventoryItem.addHashtag(language);
        }

        inventoryItem.addHashtag(inventoryItem.getType());
    }

    /**
     * Takes the inventory item already constructed in
     * {@link #createInventoryItem(com.streamreduce.core.model.Connection, net.sf.json.JSONObject)} and updates its
     * information with data in JSON.
     *
     * @param inventoryItem the inventory item
     * @param json the json representing the inventory item (from Google Analytics)
     */
    private void extendGoogleAnalyticsInventoryItem(InventoryItem inventoryItem, JSONObject json) {
        String name = json.getString("name");
        String externalId = json.getString("id");

        inventoryItem.setAlias(name);
        inventoryItem.setExternalId(externalId);
        inventoryItem.setType(Constants.ANALYTICS_TYPE);

        inventoryItem.addHashtag(inventoryItem.getType());
    }

    /**
     * Takes the inventory item already constructed in
     * {@link #createInventoryItem(com.streamreduce.core.model.Connection, net.sf.json.JSONObject)} and updates its
     * information with data in JSON.
     *
     * @param inventoryItem the inventory item
     * @param json the json representing the inventory item (from jclouds)
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    private void extendJiraInventoryItem(InventoryItem inventoryItem, JSONObject json)
            throws InvalidCredentialsException, IOException {
        Connection connection = inventoryItem.getConnection();
        ObjectId connectionId = connection.getId();
        String key = json.getString("key");
        String name = json.getString("name");
        String description = json.getString("description");
        ExternalIntegrationClient rawClient = externalClientCache.getIfPresent(connectionId);
        JiraClient client;

        if (rawClient == null) {
            client = new JiraClient(connection);

            externalClientCache.put(connectionId, client);
        } else {
            client = (JiraClient)rawClient;
        }

        if (description.length() > 256) {
            description = description.substring(0, 255);
        }

        inventoryItem.setAlias(name != null ? name : key);
        inventoryItem.setDescription(description);
        inventoryItem.setExternalId(key);
        inventoryItem.setType(Constants.PROJECT_TYPE);

        // Override default visibility, we default to hidden if the project is not public
        if (!client.isProjectPublic(key)) {
            inventoryItem.setVisibility(SobaObject.Visibility.SELF);
        }

        inventoryItem.addHashtag(inventoryItem.getType());

        // We are unable to gather any extra hashtags to apply to Jira projects at this time
    }

    /**
     * Takes the inventory item already constructed in
     * {@link #createInventoryItem(com.streamreduce.core.model.Connection, net.sf.json.JSONObject)} and updates its
     * information with data in JSON.
     *
     * @param inventoryItem the inventory item
     * @param json the json representing the inventory item (from jclouds)
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    private void extendPingdomInventoryItem(InventoryItem inventoryItem, JSONObject json) {
        String externalId = json.getString("id");
        inventoryItem.setAlias(json.getString("name"));
        inventoryItem.setDescription(json.getString("hostname"));
        inventoryItem.setExternalId(externalId);
        inventoryItem.setType(Constants.MONITOR_TYPE);
        inventoryItem.addHashtag(json.getString("type")); // http, dns, tcp, etc...
        inventoryItem.addHashtag(inventoryItem.getType());
    }

    /**
     * Takes the inventory item already constructed in
     * {@link #createInventoryItem(com.streamreduce.core.model.Connection, net.sf.json.JSONObject)} and updates its
     * information with data in JSON.
     *
     * @param inventoryItem the inventory item
     * @param json the json representing the inventory item (from jclouds)
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    private void extendGenericInventoryItem(InventoryItem inventoryItem, JSONObject json) {
        String externalId = json.getString("inventoryItemId");
        JSONArray hashtags = json.containsKey("hashtags") ? json.getJSONArray("hashtags") : new JSONArray();

        if (!StringUtils.hasText(inventoryItem.getAlias())) {
            inventoryItem.setAlias(externalId);
        }

        if (!StringUtils.hasText(inventoryItem.getDescription())) {
            inventoryItem.setDescription("Created automatically from IMG activity message.");
        }

        inventoryItem.setExternalId(externalId);
        inventoryItem.setType(Constants.CUSTOM_TYPE); // Should we support the user specifying a type instead of hard coding?

        for (Object rawHashtag : hashtags) {
            if (rawHashtag != null && !rawHashtag.toString().equals("null")) {
                inventoryItem.addHashtag(HashtagUtil.normalizeTag(rawHashtag.toString()));
            }
        }

        inventoryItem.addHashtag(inventoryItem.getType());
    }

    /**
     * Simple helper to send hashtag events whenever
     * {@link #addHashtag(com.streamreduce.core.model.Taggable, com.streamreduce.core.model.SobaObject, String)} or
     * {@link #removeHashtag(com.streamreduce.core.model.Taggable, com.streamreduce.core.model.SobaObject, String)} is called.
     *
     * @param eventId the event id to use for the created event
     * @param inventoryItem the inventory item the event happened on
     * @param tagger the object that created the tag
     * @param tag the added/removed hashtag
     */
    private void handleHashtagEvent(EventId eventId, InventoryItem inventoryItem, SobaObject tagger, String tag) {
        // Create the event
        Map<String, Object> eventContext = new HashMap<>();

        if (eventId == EventId.HASHTAG_ADD) {
            eventContext.put("addedHashtag", tag);
        } else if (eventId == EventId.HASHTAG_DELETE) {
            eventContext.put("deletedHashtag", tag);
        }

        Event event = eventService.createEvent(eventId, inventoryItem, eventContext);

        // Create the message
        messageService.sendAccountMessage(event, tagger, inventoryItem.getConnection(),
                new Date().getTime(), MessageType.INVENTORY_ITEM, inventoryItem.getHashtags(),
                null);
    }

    private Map<String, InventoryItem> getInventoryItemMap(Connection connection) {
        List<InventoryItem> inventoryItems = getInventoryItems(connection);
        Map<String, InventoryItem> inventoryItemMap = new HashMap<>();

        for (InventoryItem inventoryItem : inventoryItems) {
            inventoryItemMap.put(inventoryItem.getExternalId(), inventoryItem);
        }

        return inventoryItemMap;
    }

    private void pullEC2CloudWatchMetrics(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException {
        // Right now our CloudWatch usage is pretty specific in that we're not exactly pulling all available AWS EC2
        // CloudWatch metrics and instead of relying on specific units for each metric name.  Eventually we could/should
        // just pull down everything available and go from there.
        if (ec2CloudWatchMetricNames == null) {
            ec2CloudWatchMetricNames = new HashMap<>();

            ec2CloudWatchMetricNames.put(EC2Constants.MetricName.CPU_UTILIZATION, Unit.PERCENT);
            ec2CloudWatchMetricNames.put(EC2Constants.MetricName.DISK_READ_BYTES, Unit.BYTES);
            ec2CloudWatchMetricNames.put(EC2Constants.MetricName.DISK_READ_OPS, Unit.COUNT);
            ec2CloudWatchMetricNames.put(EC2Constants.MetricName.DISK_WRITE_BYTES, Unit.BYTES);
            ec2CloudWatchMetricNames.put(EC2Constants.MetricName.DISK_WRITE_OPS, Unit.COUNT);
            ec2CloudWatchMetricNames.put(EC2Constants.MetricName.NETWORK_IN, Unit.BYTES);
            ec2CloudWatchMetricNames.put(EC2Constants.MetricName.NETWORK_OUT, Unit.BYTES);
        }

        try (RestContext<CloudWatchApi, CloudWatchAsyncApi> context = new AWSClient(connection).getCloudWatchServiceContext()) {
            CloudWatchApi cloudWatchClient = context.getApi();
            List<InventoryItem> inventoryItems = getInventoryItems(connection);
            String metricNamespace = Namespaces.EC2;
            Calendar cal = Calendar.getInstance();
            Date endTime = new Date();
            Date startTime;

            cal.add(Calendar.MINUTE, -30);

            startTime = cal.getTime();

            for (InventoryItem inventoryItem : inventoryItems) {
                if (!inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE)) {
                    continue;
                }

                Map<String, JSONObject> metrics = new HashMap<>();
                String nodeId = inventoryItem.getExternalId();
                Location region = getLocationByScope(inventoryItem, LocationScope.REGION);

                if (region == null) {
                    continue;
                }

                String regionId = region.getId();
                Dimension dimension = new Dimension(EC2Constants.Dimension.INSTANCE_ID, nodeId);

                for (Map.Entry<String, Unit> ec2MetricEntry : ec2CloudWatchMetricNames.entrySet()) {
                    String metricName = ec2MetricEntry.getKey();
                    Unit metricUnit = ec2MetricEntry.getValue();
                    MetricApi metricClient = cloudWatchClient.getMetricApiForRegion(regionId);
                    GetMetricStatistics requestOptions = GetMetricStatistics.builder()
                            .namespace(metricNamespace)
                            .metricName(metricName)
                            .dimension(dimension)
                            .period(60)
                            .statistics(ec2CloudWatchStatisticsSet)
                            .startTime(startTime)
                            .endTime(endTime)
                            .unit(metricUnit)
                            .build();
                    GetMetricStatisticsResponse response = metricClient.getMetricStatistics(requestOptions);

                    // Per Gustavo's code, we're only adding the last metric
                    if (response != null && response.size() > 0) {
                        metrics.put(metricName, JSONObject.fromObject(response.iterator().next()));
                    }
                }

                if (!metrics.isEmpty()) {
                    Map<String, Object> eventContext = new HashMap<>();

                    eventContext.put("payload", metrics);
                    eventContext.put("isAgentActivity", false);

                    eventService.createEvent(EventId.ACTIVITY,
                            inventoryItem, eventContext);
                }
            }
        }
    }

    private void pullGitHubActivity(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        GitHubClient client = (GitHubClient)getClient(connection);
        Map<String, InventoryItem> inventoryItemMap = getInventoryItemMap(connection);
        List<JSONObject> feedEntries = client.getActivity(inventoryItemMap.keySet());
        Date lastActivityPoll = connection.getLastActivityPollDate();
        Date lastActivity = lastActivityPoll;

        try {
            for (JSONObject entry : feedEntries) {
                String projectKey = entry.getJSONObject("repo").getString("name");
                InventoryItem inventoryItem = inventoryItemMap.get(projectKey);

                if (inventoryItem == null) {
                    continue;
                }

                Date pubDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(entry.getString("created_at"));

                // Only create messages newer than the last activity poll date
                if (pubDate.before(lastActivityPoll)) {
                    continue;
                }

                if (pubDate.after(lastActivity)) {
                    lastActivity = pubDate;
                }

                Map<String, Object> activityParts = client.getPartsForActivity(inventoryItem, entry);

                // This can happen for unknown events which we log
                if (activityParts == null) {
                    // We have ran into a GitHub activity we do not know how to handle. Log the issue with as much
                    // detail as possible.
                    String entryAsJSON = entry.toString();

                    logger.error("Unable to parse GitHub activity to create activity message: " + entryAsJSON);

                    // Submit a bug report so we are aware of it.
                    emailService.sendBugReport(Constants.NODEABLE_SUPER_USERNAME, Constants.NODEABLE_SUPER_ACCOUNT_NAME,
                                               "Unable to handle GitHub activity",
                                               "There was a GitHub activity that we currently do not handle.",
                                               entryAsJSON);

                    // Should we create some specialized error message in the stream instead?

                    // Move on to the next activity entry
                    continue;
                }

                Map<String, Object> eventContext = new HashMap<>();

                eventContext.put("activityPubDate", pubDate);
                eventContext.put("activityTitle", MessageUtils.cleanEntry((String) activityParts.get("title")));
                eventContext.put("activityContent", MessageUtils.cleanEntry((String) activityParts.get("content")));
                eventContext.put("activityHashtags", activityParts.get("hashtags"));
                eventContext.put("payload", entry.toString());

                // Create the event stream entry
                Event event = eventService.createEvent(EventId.ACTIVITY,
                                                       inventoryItem, eventContext);

                messageService.sendAccountMessage(event,
                                                  inventoryItem,
                                                  connection,
                                                  pubDate.getTime(),
                                                  MessageType.ACTIVITY,
                                                  activityParts.get("hashtags") != null ?
                                                          (Set<String>) activityParts.get("hashtags") :
                                                          null,
                                                  null);
            }
        } catch (Exception e) {
            logger.error("Unknown exception occurred while pulling GitHub activity for connection [" +
                    connection.getId() + "]: " + e, e);
        } finally {
            // Update the connection's last polling time
            connection.setLastActivityPollDate(new Date(lastActivity.getTime() + 1));
            try {
                connectionService.updateConnection(connection, true);
            } catch (Exception e) {
                // This is a silent update to only update the last polling time so this should never throw an exception
            }
        }
    }

    private void pullGoogleAnalyticsActivity(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {

        GoogleAnalyticsClient client = (GoogleAnalyticsClient)getClient(connection);
        Map<String, InventoryItem> inventoryItemMap = getInventoryItemMap(connection);
        List<JSONObject> profileMetricEntries = client.getAllProfileMetrics(inventoryItemMap.keySet());
        Date lastActivityPoll = connection.getLastActivityPollDate();
        Date lastActivity = lastActivityPoll;

        try {
            for (JSONObject entry : profileMetricEntries) {
                if (entry == null) {
                    continue;
                }

                String profileId = entry.getString("id");
                InventoryItem inventoryItem = inventoryItemMap.get(profileId);

                if (inventoryItem == null) {
                    continue;
                }

                JSONArray metrics = entry.getJSONArray("metrics");
                for (Object obj : metrics) {
                    JSONObject metric = (JSONObject) obj;

                    Map<String, Object> eventContext = new HashMap<>();
                    eventContext.put("activityTitle", String.format("%s on %s is at %s", metric.getString("metric"), inventoryItem.getAlias(), metric.get("data")));
                    JSONArray jsonHashtags = metric.getJSONArray("hashtags");
                    Set<String> hashtags = new HashSet<String>(jsonHashtags);
                    eventContext.put("activityHashtags", hashtags);
                    eventContext.put("activityPayload", metric); /* need to fix this */

                    // Create the event stream entry
                    Event event = eventService.createEvent(EventId.ACTIVITY, inventoryItem, eventContext);

                    messageService.sendAccountMessage(event, inventoryItem, connection, System.currentTimeMillis(), MessageType.ACTIVITY,
                            eventContext.get("activityHashtags") != null ? (Set<String>) eventContext.get("activityHashtags") : null,
                            null);
                }
            }
        } catch (Exception e) {
            logger.error("Unknown exception occurred while pulling Google Analytics activity for connection [" +
                    connection.getId() + "]: " + e, e);
        } finally {
            // Update the connection's last polling time
            connection.setLastActivityPollDate(new Date(lastActivity.getTime() + 1));
            try {
                connectionService.updateConnection(connection, true);
            } catch (Exception e) {
                // This is a silent update to only update the last polling time so this should never throw an exception
            }
        }
    }

    private void pullJiraActivity(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        JiraClient client = (JiraClient)getClient(connection);
        Map<String, InventoryItem> inventoryItemMap = getInventoryItemMap(connection);
        Date lastActivityPoll = connection.getLastActivityPollDate();
        Date lastActivity = lastActivityPoll;

        try {
            List<Entry> feedEntries = client.getActivity(inventoryItemMap.keySet());
            if (feedEntries == null) {
                return;
            }

            for (Entry entry : feedEntries) {
                // To map project activity in Jira to a Nodeable ProjectHostingInventoryItem, we have
                // to do some magic.  Said magic is below.
                Element activityObject = entry.getFirstChild(new QName("http://activitystrea.ms/spec/1.0/", "object",
                                                                       "activity"));
                String projectKey = client.getProjectKeyOfEntry(activityObject, inventoryItemMap.keySet());

                if (projectKey == null) {
                    // If the projectKey is null here, this means we've gotten activity for a project we're monitoring
                    // but we were unable to map said activity to the project in question.  This is a known issue and
                    // typically only seen in non-hosted Jira environments where people link their Jira project to other
                    // Atlassian products but do not use the same key for the Jira project and the project in the other
                    // Atlassian application.  (SOBA-1193)  Let's go ahead and log it so we do not forget but this is a
                    // known issue and should not become a ZenDesk ticket.
                    logger.error("Project key for Jira activity was unable to be found, possibly related to " +
                                         "SOBA-1193: " + entry.toString().substring(0, 140));

                    // Move on to the next activity entry
                    continue;
                }

                InventoryItem inventoryItem = inventoryItemMap.get(projectKey);

                // This can happen if the activity is from a project, or Jira Studio product, not associated with a
                // project in our inventory system.  (A good example of this is wiki changes.  Each Jira Studio project
                // gets its own wiki but you can create new wiki spaces that are not associated with a Jira Studio
                // project and will end up without an inventory item in our system.)
                if (inventoryItem == null) {
                    logger.error("Project with key of " + projectKey + " did not correspond with an inventory item, " +
                                         "possibley related to SOBA-1193: " + entry.toString().substring(0, 140));

                    // Move on to the next activity entry
                    continue;
                }

                Date pubDate = entry.getPublished();

                // Only create messages newer than the last activity poll date
                if (pubDate.before(lastActivityPoll)) {
                    continue;
                }

                if (pubDate.after(lastActivity)) {
                    lastActivity = pubDate;
                }

                Map<String, Object> activityParts = client.getPartsForActivity(inventoryItem, entry);

                // This can happen for unknown events which we log
                if (activityParts == null) {
                    // We have ran into a Jira activity we do not know how to handle. Log the issue with as much
                    // detail as possible.
                    String entryAsJSON = entry.toString();

                    logger.error("Unable to parse Jira activity to create activity message: " + entryAsJSON);

                    // Submit a but report so we are aware of it.
                    emailService.sendBugReport(Constants.NODEABLE_SUPER_USERNAME, Constants.NODEABLE_SUPER_ACCOUNT_NAME,
                                               "Unable to handle Jira activity",
                                               "There was a Jira activity that we currently do not handle.",
                                               entryAsJSON);

                    // Should we create some specialized error message in the stream instead?

                    // Move on to the next activity entry
                    continue;
                }

                Map<String, Object> eventContext = new HashMap<>();

                eventContext.put("activityPubDate", pubDate);
                eventContext.put("activityTitle", MessageUtils.cleanEntry((String) activityParts.get("title")));
                eventContext.put("activityContent", MessageUtils.cleanEntry((String) activityParts.get("content")));
                eventContext.put("activityHashtags", activityParts.get("hashtags"));
                eventContext.put("payload", JSONUtils.xmlToJSON(entry.toString()).toString());

                // Create the event stream entry
                Event event = eventService.createEvent(EventId.ACTIVITY,
                                                       inventoryItem,
                                                       eventContext);

                JiraActivityDetails details = getJiraActivityDetailsFromActivityParts(activityParts);

                messageService.sendAccountMessage(event,
                                                  inventoryItem,
                                                  connection,
                                                  pubDate.getTime(),
                                                  MessageType.ACTIVITY,
                                                  activityParts.get("hashtags") != null ?
                                                          (Set<String>) activityParts.get("hashtags") :
                                                          null,
                                                  details);
            }

        } catch (Exception e) {
            logger.error("Unknown exception occurred while pulling Jira activity for connection [" +
                                 connection.getId() + "]: " + e, e);
        } finally {
            // Update the connection's last polling time
            connection.setLastActivityPollDate(new Date(lastActivity.getTime() + 1));
            try {
                connectionService.updateConnection(connection, true);
            } catch (Exception e) {
                // This is a silent update to only update the last polling time so this should never throw an exception
            }
        }
    }

    private void pullFeedActivity(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        Date lastActivityPoll = connection.getLastActivityPollDate();
        Date lastActivity = lastActivityPoll;

        if (lastActivityPoll != null) {
            logger.debug("Creating feed messages for messages newer than (" + lastActivityPoll + ") for [" +
                                 connection.getId() + "]: " + connection.getAlias());
        } else {
            logger.debug("Creating feed messages for all messages [" + connection.getId() + "]: "
                                 + connection.getAlias());
        }

        try (XmlReader xmlReader = new XmlReader(URI.create(connection.getUrl()).toURL())) {
            SyndFeed rssFeed = new SyndFeedInput().build(xmlReader);
            List feedEntries = rssFeed.getEntries();

            Collections.sort(feedEntries, new Comparator<Object>() {
                @Override
                public int compare(Object first, Object second) {
                    SyndEntry firstEntry = (SyndEntry) first;
                    SyndEntry secondEntry = (SyndEntry) second;
                    Date firstDate = firstEntry.getPublishedDate() != null ?
                            firstEntry.getPublishedDate() : new Date();
                    Date secondDate = secondEntry.getPublishedDate() != null ?
                            secondEntry.getPublishedDate() : new Date();

                    return firstDate.compareTo(secondDate);
                }
            });

            for (Object rawEntry : feedEntries) {
                SyndEntry entry = (SyndEntry) rawEntry;

                //use published date if it exists... otherwise don't process the message as it is an update
                //this skips feed messages from feeds that don't include a publishedDate
                Date pubDate = entry.getPublishedDate();
                if (pubDate == null || pubDate.before(lastActivityPoll)) {
                    continue;
                }

                lastActivity = pubDate.after(lastActivity) ? pubDate : lastActivity;

                Map<String, Object> eventContext = new HashMap<>();
                String messageBodyAsJson = determineMessageBodyAsJsonFromSyndEntry(entry);


                eventContext.put("activityPubDate", pubDate);
                eventContext.put("activityTitle", entry.getTitle());
                eventContext.put("payload", messageBodyAsJson);

                Event event = eventService.createEvent(EventId.ACTIVITY,
                        connection,
                        eventContext);

                FeedEntryDetails details = new FeedEntryDetails.Builder()
                        .url(entry.getUri())
                        .title(entry.getTitle())
                        .description(entry.getDescription() != null ? entry.getDescription().getValue() : null)
                        .publishedDate(pubDate)
                        .build();

                // Create a new message to be delivered to inboxes
                messageService.sendActivityMessage(event, connection, pubDate.getTime(), details);
            }
        } catch (IOException e) {
            logger.error(String.format("Error opening the connection %s for feed %s. Returned error: %s",
                    connection.getId(), connection.getUrl(), e.getMessage()));
        } catch (Exception e) {
            logger.error(String.format("Unable to process messages for connection %s with feed %s. Returned error: %s",
                    connection.getId(), connection.getUrl(), e.getMessage()));
        } finally {
            // Update the connection's last polling timeconnection.setLastActivityPollDate(new Date(lastActivity.getTime() + 1));
            try {
                connectionService.updateConnection(connection, true);
            } catch (Exception e) {
                // This is a silent update to only update the last polling time so this should never throw an exception
            }
        }
    }

    private void pullPingdomActivity(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException {
        PingdomClient client = (PingdomClient)getClient(connection);
        Map<String,InventoryItem> inventoryItemMap = getInventoryItemMap(connection);
        Date lastActivityPoll = connection.getLastActivityPollDate();

        if (lastActivityPoll != null) {
            logger.debug("Creating Pingdom messages for messages newer than (" + lastActivityPoll + ") for [" +
                                 connection.getId() + "]: " + connection.getAlias());
        } else {
            logger.debug("Creating Pingdom messages for all messages [" + connection.getId() + "]: "
                                 + connection.getAlias());
        }

        try {
            List<JSONObject> jsonInventoryList = client.checks();

            for (Iterator<JSONObject> i = jsonInventoryList.iterator(); i.hasNext();) {
                JSONObject jsonInventory = i.next();
                InventoryItem inventoryItem = inventoryItemMap.get(jsonInventory.getString("id"));

                if (inventoryItem == null) {
                    continue;
                }

                // the connection may not have been tested yet from Pingdom
                if (!jsonInventory.containsKey("lasttesttime")) {
                    continue;
                }

                Date lastTestTime = new DateTime().withMillis(0).plusSeconds(jsonInventory.getInt("lasttesttime")).toDate();
                if (lastActivityPoll != null && lastActivityPoll.after(lastTestTime)) {
                    continue;
                }

                lastActivityPoll = lastTestTime.after(lastActivityPoll) ? lastTestTime : lastActivityPoll;

                Map<String, Object> eventContext = new HashMap<>();

                eventContext.put("activityPubDate", lastTestTime);
                eventContext.put("activityTitle", String.format("Service check %s (%s) has a response time of %dms.",
                        jsonInventory.getString("name"),
                        jsonInventory.getString("type").toUpperCase(),
                        jsonInventory.getInt("lastresponsetime")));
                eventContext.put("payload", jsonInventory);

                if (jsonInventory.containsKey("lastresponsetime")) {
                    eventContext.put("lastResponseTime", jsonInventory.getInt("lastresponsetime"));
                }

                Event event = eventService.createEvent(EventId.ACTIVITY,
                                                       inventoryItem,
                                                       eventContext);

                PingdomEntryDetails details = new PingdomEntryDetails.Builder()
                        .lastErrorTime(jsonInventory.containsKey("lasterrortime") ? jsonInventory.getInt("lasterrortime") : 0)
                        .lastResponseTime(jsonInventory.containsKey("lastresponsetime") ? jsonInventory.getInt("lastresponsetime") : 0)
                        .lastTestTime(jsonInventory.getInt("lasttesttime"))
                        .checkCreated(jsonInventory.getInt("created"))
                        .resolution(jsonInventory.getInt("resolution"))
                        .status(jsonInventory.getString("status"))
                        .build();

                // Create a new message to be delivered to inboxes
                messageService.sendAccountMessage(event,
                        inventoryItem,
                        connection,
                        lastTestTime.getTime(),
                        MessageType.ACTIVITY,
                        inventoryItem.getHashtags(),
                        details);
            }

            // Update the connection's last polling time
            connection.setLastActivityPollDate(new Date(lastActivityPoll.getTime() + 1));

            connectionService.updateConnection(connection, true);
        } catch (IOException e) {
            logger.error(String.format("Error opening the connection %s for feed %s. Returned error: %s",
                    connection.getId(), connection.getUrl(), e.getMessage()));
        } catch (Exception e) {
            logger.error(String.format("Unable to process messages for connection %s with feed %s. Returned error: %s",
                    connection.getId(), connection.getUrl(), e.getMessage()));
        }
    }

    private void pullTwitterActivity(Connection connection) {
        TwitterClient client = (TwitterClient)getClient(connection);

        try {
            JSONObject profile = client.getLoggedInProfile();

            if (profile == null) {
                logger.error("User's profile for Twitter connection %s came back null.", connection.getId());
                return;
            }

            Map<String, Object> eventContext = new HashMap<>();
            int favoritesCount = profile.containsKey("favourites_count") ?
                    profile.getInt("favourites_count") :
                    0;
            int followersCount = profile.containsKey("followers_count") ?
                    profile.getInt("followers_count") :
                    0;
            int friendsCount = profile.containsKey("friends_count") ?
                    profile.getInt("friends_count") :
                    0;
            int listedCount = profile.containsKey("listed_count") ?
                    profile.getInt("listed_count") :
                    0;
            int statusesCount = profile.containsKey("statuses_count") ?
                    profile.getInt("statuses_count") :
                    0;
            String screenName = profile.containsKey("screen_name") ?
                    profile.getString("screen_name") :
                    "unknown";
            String name = profile.containsKey("name") ?
                    profile.getString("name") :
                    "Unknown";

            // Create a "title" for the activity (Shouldn't be here but without refactoring message transformation, it
            // is what it is.)
            String activityTitle = String.format("Twitter user stats for %s (%s)", screenName, name);
            String activityContent = String.format("Following %d users, has %d followers, has tweeted %d times, has " +
                    "created %d favorites and has been added to %d lists.",
                                                   friendsCount, followersCount, statusesCount, favoritesCount,
                                                   listedCount);

            eventContext.put("activityTitle", activityTitle);
            eventContext.put("activityContent", activityContent);
            eventContext.put("payload", profile);

            Event event = eventService.createEvent(EventId.ACTIVITY, connection, eventContext);
            TwitterActivityDetails details = new TwitterActivityDetails.Builder()
                    .favorites(favoritesCount)
                    .followers(followersCount)
                    .friends(friendsCount)
                    .profile(profile)
                    .statuses(statusesCount)
                    .build();

            messageService.sendActivityMessage(event, connection, new Date().getTime(), details);
        } catch (Exception e) {
            logger.error(String.format("Error getting the user's profile for Twitter connection %s. Returned error: %s",
                                       connection.getId(), e.getMessage()));
        }
    }

    /**
     * Extracts a message body from a Rome SyndEntry object.  This handles calculating a message body from SyndEntry
     * objects that might either represent an entry in an RSS feed or an Atompub feed.
     *
     * @param entry SyndEntry to extract a message body from
     * @return JSON mapping of the description if it exists, or the first content element if there are contents.
     *         Otherwise an empty string is mapped to Json
     * @throws IOException when the SyndEntry fields can't be read or parsed to JSON.
     */
    private String determineMessageBodyAsJsonFromSyndEntry(SyndEntry entry) throws IOException {
        String body = ""; //default Body
        ObjectMapper om = new ObjectMapper();
        SyndContent desc = entry.getDescription();

        if (desc != null) {
            body = om.writeValueAsString(desc);
        } else if (entry.getContents().size() > 0) {
            body = om.writeValueAsString(entry.getContents().get(0)); //grab the first content item and use as body
        }

        return body;
    }

    /**
     * Creates a JiraActivityDetails object from the activityParts Map.  Specifically, this looks up the rawContent
     * part of Jira activity and stores in the html field of JiraActivities for display in rich messages.  Since it is
     * possible for jira messages to be extremely long (for instance, a commit message of a branch
     * that copies several thousand files will result in an html payload of tens of thousand of characters), this method
     * will return null so that activity details are not kept and clients will resort to non-richly formatted content.
     *
     * @param activityParts activityParts created from polling a jira instance
     * @return JiraActivityDetails with an html field if activityParts is not null, its "rawContent" key has a non-null
     * value, and the value of "rawContent" does not exceed a predefined number of characters.
     */
    private JiraActivityDetails getJiraActivityDetailsFromActivityParts(Map<String, Object> activityParts) {
        if (activityParts == null) { return null;}
        String rawContent = (String) activityParts.get("rawContent");
        if (rawContent == null || rawContent.length() > Constants.MAX_MESSAGE_LENGTH) { return null; }
        return new JiraActivityDetails.Builder()
                .html((rawContent))
                .build();
    }

    private ExternalIntegrationClient getClient(Connection connection) {
        ExternalIntegrationClient client;
        client = externalClientCache.getIfPresent(connection.getId());
        if (client == null) {
            client = connectionProviderFactory.externalIntegrationConnectionProviderFromId(connection.getProviderId())
                    .getClient(connection);
            externalClientCache.put(connection.getId(), client);
        }

        return client;
    }
}
