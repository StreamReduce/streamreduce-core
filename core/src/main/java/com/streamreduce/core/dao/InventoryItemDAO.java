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

package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.common.base.Preconditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository("inventoryItemDAO")
public class InventoryItemDAO extends ValidatingDAO<InventoryItem> {

    @Autowired
    protected InventoryItemDAO(@Qualifier(value = "businessDBDatastore") Datastore ds) {
        super(ds);
    }

    /**
     * Returns all inventory items for the given connection that the user can see.
     * <p/>
     * * Note: Does not return any inventory items marked as deleted.
     *
     * @param connection the connection whose inventory items we're interested in
     * @param user       the user who we're checking visibility against
     * @return the list of inventory items the user can see
     * @throws IllegalArgumentException if connection is null
     */
    public List<InventoryItem> getInventoryItems(Connection connection, @Nullable User user) {
        Preconditions.checkNotNull(connection, "connection cannot be null.");

        Query<InventoryItem> query = createQuery();

        query.field("connection").equal(connection);
        query.field("deleted").equal(false);

        if (user != null) {
            query.or(
                    query.criteria("user").equal(user),
                    query.and(
                            query.criteria("account").equal(user.getAccount()),
                            query.criteria("visibility").equal(SobaObject.Visibility.ACCOUNT)
                    ),
                    query.criteria("visibility").equal(SobaObject.Visibility.PUBLIC)
            );
        }

        return query.asList();
    }

    /**
     * Returns the inventory item for the given connection and external id.
     *
     * @param connection the connection whose inventory we want to search
     * @param externalId the external id we're interested in
     * @return the inventory item
     * @throws IllegalArgumentException if connection is null or if externalId is null
     */
    public InventoryItem getInventoryItem(Connection connection, String externalId) {
        Preconditions.checkNotNull(connection, "connection cannot be null.");
        Preconditions.checkNotNull(externalId, "externalId cannot be null.");

        Query<InventoryItem> query = createQuery();

        query.field("connection").equal(connection);
        query.field("externalId").equal(externalId);

        return query.get();
    }

    /**
     * Returns the inventory items for the given connection id.
     *
     * @param connectionId the id of the connection whose inventory items we're interested in
     * @return the list of inventory items or an empty list if there are none
     * @throws IllegalArgumentException if connectionId is null
     */
    public List<InventoryItem> getInventoryItems(ObjectId connectionId) {
        Preconditions.checkNotNull(connectionId, "connectionId cannot be null.");

        DBCollection collection = getDatastore().getDB().getCollection("inventoryItems");
        BasicDBObject query = new BasicDBObject();
        DBCursor cursor;

        query.put("connection.$id", connectionId);

        cursor = collection.find(query);

        List<InventoryItem> inventoryItems = new ArrayList<InventoryItem>();

        try {
            while (cursor.hasNext()) {
                BasicDBObject rawInventoryItem = (BasicDBObject) cursor.next();

                inventoryItems.add(get((ObjectId) rawInventoryItem.get("_id")));
            }
        } finally {
            cursor.close();
        }

        return inventoryItems;
    }

    /**
     * Returns all inventory items having the external id provided and is not marked as deleted.
     *
     * @param externalId the external id we're interested in
     * @return list of inventory items or am empty list if there are none
     * @throws IllegalArgumentException if externalId is null
     */
    public List<InventoryItem> getInventoryItemsForExternalId(String externalId) {
        Preconditions.checkNotNull(externalId, "externalId cannot be null.");

        Query<InventoryItem> query = createQuery();

        query.field("externalId").equal(externalId);
        query.field("deleted").equal(false);

        return query.asList();
    }

}
