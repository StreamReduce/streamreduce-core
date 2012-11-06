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
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.streamreduce.core.CollectionObjectNotFoundException;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Jeremy Whitlock <jeremy@nodeable.com> is the coolest guy in the world!!!  Love, Mark
 */
@Repository("generalCollectionDAO")
public class GenericCollectionDAO {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier(value = "businessDBDatastore")
    private Datastore businessDatastore;

    @Autowired
    @Qualifier(value = "messageDBDatastore")
    private Datastore messageDatastore;

    public DB getDatabase(DAODatasourceType datasourceType) {
        switch (datasourceType) {
            case BUSINESS:
                return businessDatastore.getDB();
            case MESSAGE:
                return messageDatastore.getDB();
            default:
                throw new RuntimeException("Invalid datasource type!");
        }
    }

    public BasicDBObject removeCollectionEntry(SobaObject sobaObject, ObjectId id) {
        DB db = getDatabase(DAODatasourceType.BUSINESS);

        String collectionName = getCollectionNameFromType(sobaObject);

        DBCollection collection = db.getCollection(collectionName);
        BasicDBObject payload = getById(DAODatasourceType.BUSINESS, collectionName, id);
        if (payload != null) {
            collection.remove(payload);
        }
        return payload;
    }

    private String getCollectionNameFromType(SobaObject sobaObject) {
        if (sobaObject instanceof InventoryItem) {
            return "inventoryItems";
        }
        if (sobaObject instanceof Connection) {
            return "connections";
        }
        if (sobaObject instanceof User) {
            return "users";
        }
        return null;
    }

    public BasicDBObject createCollectionEntry(DAODatasourceType datasourceType, String collectionName,
                                               String payloadAsJson) {
        return createCollectionEntry(datasourceType, collectionName, (BasicDBObject) JSON.parse(payloadAsJson));
    }

    public BasicDBObject createCollectionEntry(DAODatasourceType datasourceType, String collectionName,
                                               BasicDBObject payloadObject) {
        DB db = getDatabase(datasourceType);
        DBCollection collection = db.getCollection(collectionName);
        collection.insert(payloadObject);

        return payloadObject;
    }

    public BasicDBObject updateCollectionEntry(DAODatasourceType datasourceType, String collectionName, ObjectId id,
                                               String json) throws CollectionObjectNotFoundException {
        DB db = getDatabase(datasourceType);
        DBCollection collection = db.getCollection(collectionName);
        BasicDBObject newPayloadObject = (BasicDBObject) JSON.parse(json);
        BasicDBObject oldPayloadObject = (BasicDBObject) collection.findOne(new BasicDBObject("_id", id));

        if (oldPayloadObject == null) {
            throw new CollectionObjectNotFoundException(datasourceType, collectionName, id);
        }

        newPayloadObject.put("_id", id);

        collection.save(newPayloadObject);

        return newPayloadObject;
    }


    public BasicDBObject getById(DAODatasourceType datasourceType, String collectionName, ObjectId id) {
        DB db = getDatabase(datasourceType);
        DBCollection collection = db.getCollection(collectionName);
        BasicDBObject searchById = new BasicDBObject("_id", id);
        return (BasicDBObject) collection.findOne(searchById);
    }

    public BasicDBObject removeCollectionEntry(DAODatasourceType datasourceType, String collectionName, ObjectId id) {
        DB db = getDatabase(datasourceType);
        DBCollection collection = db.getCollection(collectionName);
        BasicDBObject payload = getById(datasourceType, collectionName, id);
        if (payload != null) {
            collection.remove(payload);
        }
        return payload;
    }

}
