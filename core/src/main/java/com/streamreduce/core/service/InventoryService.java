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

import java.io.IOException;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.core.CommandNotAllowedException;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.TaggableService;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.core.service.exception.InventoryItemNotFoundException;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;

/**
 * Interface for working with {@link com.streamreduce.core.model.InventoryItem} objects.
 */
public interface InventoryService extends TaggableService<InventoryItem> {

    /**
     * Returns the raw object payload/metadata for the inventory item.
     *
     * @param inventoryItem the inventory item whose metadata to retrieve
     *
     * @return the raw metadata or null
     */
    BasicDBObject getInventoryItemPayload(InventoryItem inventoryItem);

    /**
     * Creates an inventory item within the connection passed in and as described by the JSON provided.
     *
     * * The inventory items metadata will be automatically created and the metadataId will be updated accordingly.
     *
     * @param connection the connection to create the inventory item in
     * @param json the raw external representation to be stored
     *
     * @return the created inventory item
     *
     * @throws ConnectionNotFoundException if the connection referenced by the inventory item does not exist
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     * @throws IOException if a communication issue happens between Nodeable and the external service
     */
    InventoryItem createInventoryItem(Connection connection, JSONObject json)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException;

    /**
     * Updates the inventory item as described by its raw external representation passed in as a {@link JSONObject}.
     *
     * @param inventoryItem the inventory item to update
     * @param json the raw external representation to be stored
     *
     * @return the updated inventory item
     *
     * @throws ConnectionNotFoundException if the connection referenced by the inventory item does not exist
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     * @throws IOException if a communication issue happens between Nodeable and the external service
     * @throws NullPointerException if the inventory item is null or the JSON is null
     */
    InventoryItem updateInventoryItem(InventoryItem inventoryItem, JSONObject json)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException;

    /**
     * Updates an inventory item.
     *
     * * Note: Unlike {@link #updateInventoryItem(com.streamreduce.core.model.InventoryItem, net.sf.json.JSONObject)}, this
     * method only updates the database record and sends an event, if it's not a silent update.
     *
     * @param inventoryItem the inventory item to be updated
     * @param silentUpdate whether or not to suppress the event/message
     *
     * @return the updated inventory item
     *
     * @throws NullPointerException if the inventory item is null or the JSON is null
     */
    InventoryItem updateInventoryItem(InventoryItem inventoryItem, boolean silentUpdate);

    /**
     * Removed the inventory item.
     *
     * @param inventoryItem the inventory item to remove
     *
     * @throws NullPointerException if the inventory item is null
     */
    void deleteInventoryItem(InventoryItem inventoryItem);

    /**
     * Marks an inventory item as deleted.
     *
     * * Using this instead of {@link #deleteInventoryItem(com.streamreduce.core.model.InventoryItem)} is preferred but
     * since we need to really delete inventory items sometimes, like when a connection is deleted permanently, the
     * aforementioned method exists as well.
     *
     * @param inventoryItem the inventory item to mark as deleted
     */
    void markInventoryItemDeleted(InventoryItem inventoryItem);

    /**
     * Returns all inventory items for a connection.
     *
     * @param connection the connection we're interested in
     *
     * @return the list of inventory items or an empty list if there are none
     */
    List<InventoryItem> getInventoryItems(Connection connection);

    /**
     * Returns all inventory items for a connection.
     *
     * @param connectionId the connection id we're interested in
     *
     * @return the list of inventory items or an empty list if there are none
     */
    List<InventoryItem> getInventoryItems(ObjectId connectionId);

    /**
     * Return all inventory items, across all accounts, with the given external id.
     *
     * @param externalId the external id of the inventory item we're interested in
     *
     * @return the list of inventory items with the same external id
     */
    List<InventoryItem> getInventoryItemsForExternalId(String externalId);

    /**
     * Returns the inventory item in the connection for the externalId provided.
     *
     * @param connection the connection we're interested in
     * @param externalId the external id we're interested in
     *
     * @return the inventory item
     *
     * @throws IllegalArgumentException if connection or externalId is null
     * @throws InventoryItemNotFoundException if there is no inventory item found
     */
    InventoryItem getInventoryItemForExternalId(Connection connection, String externalId)
            throws InventoryItemNotFoundException;

    /**
     * Returns all inventory items for a connection visible by the user.
     *
     * @param connection the connection we're interested in
     * @param user the user we're interested in
     *
     * @return the list of inventory items or an empty list if there are none
     */
    List<InventoryItem> getInventoryItems(Connection connection, User user);

    /**
     * Returns the inventory item for the given object id.
     *
     * @param objectId the object id of the inventory item we're interested in
     *
     * @return the inventory item
     *
     * @throws InventoryItemNotFoundException if there is no inventory item with the given id
     */
    InventoryItem getInventoryItem(ObjectId objectId) throws InventoryItemNotFoundException;

    /**
     * Refreshes the internal inventory item cache from the external inventory representation.
     *
     * @param connection the connection whose inventory we want to refresh
     *
     * @throws NullPointerException if client is null
     * @throws ConnectionNotFoundException if the connection referenced by the inventory item does not exist
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     * @throws IOException if a communication issue happens between Nodeable and the external service
     */
    void refreshInventoryItemCache(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException;

    /**
     * Pulls the activities for the connection, or its inventory items.
     *
     * @param connection the connection who we want to pull activities for
     *
     * @throws NullPointerException if client is null
     * @throws ConnectionNotFoundException if the connection referenced by the inventory item does not exist
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     * @throws IOException if a communication issue happens between Nodeable and the external service
     */
    void pullInventoryItemActivity(Connection connection)
            throws ConnectionNotFoundException, InvalidCredentialsException, IOException;

    /* HELPER METHODS */

    /**
     * Returns the IP address of the inventory item.
     *
     * * Note: This only returns the first IP address so if there are more than one and you need all, fix this
     *
     * @param inventoryItem the inventory item we're interested in
     *
     * @return the IP address or null if one cannot be found
     *
     * @throws NullPointerException if the inventory item is null
     * @throws IllegalArgumentException if the inventory item is not a compute instance
     */
    String getComputeInstanceIPAddress(InventoryItem inventoryItem);

    /**
     * Returns the IP address of the inventory item.
     *
     * * Note: This only returns the first IP address so if there are more than one and you need all, fix this
     *
     * @param json the raw JSON representation of the inventory item (from jclouds)
     *
     * @return the IP address or null if one cannot be found
     *
     * @throws NullPointerException if the JSON is null
     * @throws IllegalArgumentException if the JSON is for a type other than NODE
     */
    String getComputeInstanceIPAddress(JSONObject json);

    /**
     * Returns the OS name of the inventory item.
     *
     * @param inventoryItem the inventory item we're interested in
     *
     * @return the OS name or null if one cannot be found
     *
     * @throws NullPointerException if the inventory item is null
     * @throws IllegalArgumentException if the inventory item is not a compute instance
     */
    String getComputeInstanceOSName(InventoryItem inventoryItem);

    /**
     * Returns the OS name of the inventory item.
     *
     * @param json the raw JSON representation of the inventory item (from jclouds)
     *
     * @return the OS name or null if one cannot be found
     *
     * @throws NullPointerException if the JSON is null
     * @throws IllegalArgumentException if the JSON is for a type other than NODE
     */
    String getComputeInstanceOSName(JSONObject json);

    /**
     * Returns the location for the scope provided of the inventory item.
     *
     * @param inventoryItem the inventory item we're interested in
     * @param scope the scope we're interested in
     *
     * @throws NullPointerException if the inventory item is null or scope is null
     */
    Location getLocationByScope(InventoryItem inventoryItem, LocationScope scope);

    /**
     * Returns the location for the scope provided of the {@link JSONObject}.
     *
     * @param json the raw JSON representation of the inventory item (from jclouds)
     * @param scope the scope we're interested in
     *
     * @throws NullPointerException if the JSON is null or the scope is null
     * @throws IllegalArgumentException if the JSON does not contain a location
     */
    Location getLocationByScope(JSONObject json, LocationScope scope);

    /**
     * Reboots the compute instance.
     *
     * @param inventoryItem the compute inventory item to reboot
     *
     * @throws CommandNotAllowedException if the instance is already terminated
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    void rebootComputeInstance(InventoryItem inventoryItem)
            throws CommandNotAllowedException, InvalidCredentialsException;

    /**
     * Destroys the compute instance.
     *
     * @param inventoryItem the compute inventory item to destroy
     *
     * @throws CommandNotAllowedException if the instance is already terminated
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    void destroyComputeInstance(InventoryItem inventoryItem)
            throws CommandNotAllowedException, InvalidCredentialsException;

}
