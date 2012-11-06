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

package com.streamreduce.storm;

import com.mongodb.*;
import com.streamreduce.Constants;
import com.streamreduce.core.metric.NodeableMetric;
import com.streamreduce.util.PropertiesOverrideLoader;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A specialized MongoDB client specifically for Nodeable's needs.
 */
public class MongoClient {

    public static final String BUSINESSDB_CONFIG_ID = "business";
    public static final String MESSAGEDB_CONFIG_ID = "message";
    public static final Logger logger = Logger.getLogger(MongoClient.class);

    private final Map<String, DB> dbMap = new HashMap<String, DB>();
    private String host;
    private int port;
    private String username;
    private String password;
    private Mongo mongo;
    private static Properties databaseProperties;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        databaseProperties = PropertiesOverrideLoader.loadProperties("database.properties");
    }

    /**
     * Connect to the Nodeable MongoDB instance using a properties file (<b>TEST-database.properties</b>)
     * that uses the following naming convention for specifying the MongoDB host, port,
     * username and password:
     * <p/>
     * Host    : {configId}.database.host
     * Port    : {configId}.database.port
     * Username: {configId}.database.username
     * Password: {configId}.database.password
     * <p/>
     * <b>configId</b> is a simple id to find a configuration by.
     *
     * @param configId the database configuration id
     */
    public MongoClient(String configId) {

        String usernameKey = configId + ".database.user";
        String passwordKey = configId + ".database.password";

        init(databaseProperties.getProperty(configId + ".database.host"),
                Integer.valueOf(databaseProperties.getProperty(configId + ".database.port")),
                (databaseProperties.containsKey(usernameKey) ?
                        databaseProperties.getProperty(usernameKey) :
                        null),
                (databaseProperties.containsKey(passwordKey) ?
                        databaseProperties.getProperty(passwordKey) :
                        null));
    }

    /**
     * Connect to the Nodeable MongoDB instance using the values passed in.  (Note: Username and password are used
     * only when connecting to a specific database so please use an admin database or a user that is available
     * across all the databases you need.
     *
     * @param host     the host MongoDB is expected to be running on
     * @param port     the port MongoDB is expected to be listening on
     * @param username the username to authenticate as
     * @param password the password to the username above
     */
    public MongoClient(String host, int port, String username, String password) {
        init(host, port, username, password);
    }

    /**
     * Returns the list of events in the Nodeable datastore after the date given, of
     * all events if the date is null.
     *
     * @param since the date to get the events after (null means get all events)
     * @return the list of events or an empty list if there are none
     */
    public List<BasicDBObject> getEvents(Date since) {
        DB connectionsDb = getDB("nodeablemsgdb");
        BasicDBObject query = new BasicDBObject();

        if (since != null) {
            query.put("timestamp", new BasicDBObject("$gt", since.getTime()));
        }

        return asList(connectionsDb.getCollection("eventStream").find(query));
    }

    /**
     * Returns the event for a particular object and version.
     *
     * @param targetId the object id of the target we're intersted in
     * @param version  the version of the object
     * @return the object representing the event or null
     */
    public Map<String, Object> getEventForTargetAndVersion(String targetId, int version) {
        // Quick return
        if (targetId == null) {
            return null;
        }

        DB connectionsDb = getDB("nodeablemsgdb");
        BasicDBObject query = new BasicDBObject();

        query.put("targetId", new ObjectId(targetId));
        query.put("metadata.targetVersion", version);

        DBObject result = connectionsDb.getCollection("eventStream").findOne(query);
        mapMongoToPlainJavaTypes(result);
        return result != null ? result.toMap() : null;
    }

    /**
     * Returns the list of events in the Nodeable datastore between the Dates specified by the since and before
     * parameters.
     *
     * @param since the date (exclusive) to get the events after.  A null means this parameter is ignored.
     * @param until the date (inclusive) to get the events before.  A null means this parameter is ignored.
     * @return the list of events or an empty list if there are none
     */
    public List<BasicDBObject> getEvents(Date since, Date until) {
        DB connectionsDb = getDB("nodeablemsgdb");

        QueryBuilder queryBuilder = QueryBuilder.start();
        if (since != null) {
            queryBuilder.and("timestamp").greaterThan(since.getTime());
        }
        if (until != null) {
            queryBuilder.and("timestamp").lessThanEquals(until.getTime());
        }

        DBObject query = queryBuilder.get();
        return asList(connectionsDb.getCollection("eventStream").find(query));
    }


    /**
     * Returns the event with the given id.
     *
     * @param eventId the event id
     * @return the {@link BasicDBObject} representing the event or null if not found
     */
    public BasicDBObject getEvent(String eventId) {
        DB connectionsDb = getDB("nodeablemsgdb");
        DBCollection eventCollection = connectionsDb.getCollection("eventStream");
        return (BasicDBObject) eventCollection.findOne(new ObjectId(eventId));
    }

    /**
     * Returns the list of connections in the Nodeable datastore.
     *
     * @return the list of connections or an empty list if there are none
     */
    public List<BasicDBObject> getConnections() {
        DB connectionsDb = getDB("nodeabledb");

        return asList(connectionsDb.getCollection("connections").find());
    }

    /**
     * Returns a single connection with the given id.
     *
     * @param connectionId the id of the connection to be returned
     * @return {@link BasicDBObject}
     */
    public BasicDBObject getConnection(String connectionId) {
        DB connectionsDb = getDB("nodeablemsgdb");
        DBCollection eventCollection = connectionsDb.getCollection("connections");
        return (BasicDBObject) eventCollection.findOne(new ObjectId(connectionId));
    }

    /**
     * Returns the inventory items for the cloud connection with the given id.
     *
     * @param connectionId the cloud connection id
     * @return a list of {@link BasicDBObject} representing each cloud inventory item
     */
    public List<BasicDBObject> getCloudInventoryItems(String connectionId) {
        return getInventoryItems("cloudInventoryItems", connectionId);
    }

    /**
     * Returns the inventory items for the project hosting connection with the given id.
     *
     * @param connectionId the project hosting connection id
     * @return a list of {@link BasicDBObject} representing each project hosting inventory item
     */
    public List<BasicDBObject> getProjectHostingInventoryItems(String connectionId) {
        return getInventoryItems("projectHostingInventoryItems", connectionId);
    }

    /**
     * Reads the last processed event date of the spout.
     *
     * @param spoutName name of the spout
     * @return last processed event date
     */
    public long readLastProcessedEventDate(String spoutName) {
        DB connectionsDb = getDB("nodeablemsgdb");
        DBCollection eventCollection = connectionsDb.getCollection("spoutLastProcessedDate");
        BasicDBObject query = new BasicDBObject();
        query.put("spoutName", spoutName);
        DBObject obj = eventCollection.findOne(query);
        if (obj != null) {
            return (Long) obj.get("lastProcessedEventDate");
        } else {
            return -1;
        }
    }

    /**
     * Updates the collection that tracks the last processed event date of the spouts.
     *
     * @param spoutName              name of the spout
     * @param lastProcessedEventDate last processed event date
     */
    public void updateLastProcessedEventDate(String spoutName, long lastProcessedEventDate) {
        DB connectionsDb = getDB("nodeablemsgdb");
        DBCollection eventCollection = connectionsDb.getCollection("spoutLastProcessedDate");
        BasicDBObject query = new BasicDBObject();
        query.put("spoutName", spoutName);
        BasicDBObject update = new BasicDBObject();
        update.put("spoutName", spoutName);
        update.put("lastProcessedEventDate", lastProcessedEventDate);
        eventCollection.findAndModify(query, null, null, false, update, false, true);
    }

    /**
     * Takes the information passed in and creates a {@link BasicDBObject} and writes it to the appropriate
     * account-specific inbox.
     *
     * @param metricAccount     the metric account
     * @param metricName        the metric name
     * @param metricType        the metric type
     * @param metricTimestamp   the metric timestamp
     * @param metricValue       the metric value
     * @param metricGranularity the metric granularity
     * @param metricCriteria    the metric criteria
     * @param metricAVGY        the metric average/mean
     * @param metricSTDDEV      the metric standard deviation
     * @param metricDIFF        the metric diff
     * @param metricMIN         the metric minimum value seen
     * @param metricMAX         the metric maximum value seen
     * @return Map containing a single entry where the key is the collection the metric was created and the value
     *         being the newly created {@link BasicDBObject}
     */
    public Map<String, BasicDBObject> writeMetric(String metricAccount, String metricName, String metricType,
                                                  Long metricTimestamp, Float metricValue, Long metricGranularity,
                                                  Map<String, String> metricCriteria,
                                                  Float metricAVGY, Float metricSTDDEV, Float metricDIFF,
                                                  Float metricMIN, Float metricMAX, Boolean metricIsAnomaly) {
        DB metricsDB = getDB("nodeablemsgdb");
        String collectionName = Constants.METRIC_COLLECTION_PREFIX + metricAccount;
        DBCollection metricsCollection = metricsDB.getCollection(collectionName);
        BasicDBObject metric = new BasicDBObject();

        metric.put("metricName", metricName);
        metric.put("metricType", metricType);
        metric.put("metricTimestamp", metricTimestamp);
        metric.put("metricValue", metricValue);
        metric.put("metricGranularity", metricGranularity);
        metric.put("metricCriteria", metricCriteria);
        metric.put("metricAVGY", metricAVGY);
        metric.put("metricSTDDEV", metricSTDDEV);
        metric.put("metricDIFF", metricDIFF);
        metric.put("metricMIN", metricMIN);
        metric.put("metricMAX", metricMAX);
        metric.put("metricIsAnomaly", metricIsAnomaly);

        metricsCollection.insert(metric);

        Map<String, BasicDBObject> result = new HashMap<String, BasicDBObject>();

        result.put(collectionName, metric);

        return result;
    }

    /**
     * Persists the <code>metric</code> to the account-specific collection as a BasicDBObject.
     *
     * @param metric persisted metric object
     * @return Map containing the collection name as the key and the BasicDBObject representation of the metric as the value
     */
    public Map<String, BasicDBObject> writeMetric(NodeableMetric metric) {
        DB metricsDB = getDB("nodeablemsgdb");
        String collectionName = Constants.METRIC_COLLECTION_PREFIX + metric.getStream().getAccountId();
        DBCollection metricsCollection = metricsDB.getCollection(collectionName);
        BasicDBObject persistedMetric = new BasicDBObject();

        persistedMetric.append("timestamp", metric.getTimestamp())
                .append("stream",
                        new BasicDBObject()
                                .append("accountId", metric.getStream().getAccountId())
                                .append("connectionId", metric.getStream().getConnectionId())
                                .append("inventoryItemId", metric.getStream().getInventoryItemId()))
                .append("type", metric.getType().getId().toString())
                .append("anomaly", metric.isAnomaly())
                .append("granularity", metric.getGranularity().name())
                .append("metricValue",
                        new BasicDBObject()
                            .append("mode", metric.getValue().getMode().name())
                            .append("type", metric.getValue().getType().name())
                            .append("value", metric.getValue().getValue())
                            .append("stddev", metric.getValue().getStddev())
                            .append("mean", metric.getValue().getMean())
                            .append("diff", metric.getValue().getDiff())
                            .append("min", metric.getValue().getMin())
                            .append("max", metric.getValue().getMax())
                );

        metricsCollection.insert(persistedMetric);

        Map<String, BasicDBObject> result = new HashMap<String, BasicDBObject>();
        result.put(collectionName, persistedMetric);
        return result;
    }

    /**
     * Returns the metrics for a given account.
     *
     * @param metricAccount the metric account
     * @return the metrics
     */
    public List<BasicDBObject> getMetrics(String metricAccount) {
        DB metricsDB = getDB("nodeablemsgdb");
        String collectionName = Constants.METRIC_COLLECTION_PREFIX + metricAccount;
        DBCollection metricsCollection = metricsDB.getCollection(collectionName);

        return asList(metricsCollection.find());
    }

    /**
     * Returns the last two metrics for a given account, metricName and granularity.
     * eg: db.Inbox_4f8c34d3cea02afbc4aa8ce8.find({"metricName":"INVENTORY_ITEM_RESOURCE_USAGE.4f8c3e50cea02afbc4aa8f49.NetworkOut.average","metricGranularity":86400000}).sort({"metricTimestamp":-1}).limit(2);
     *
     * @param metricAccount     the metric account
     * @param metricName        the metric name
     * @param metricGranularity the metric granularity
     * @return the last 2 metrics
     */
    public List<Map<String, Object>> getLastTwoTuples(String metricAccount, String metricName, long metricGranularity) {
        DB metricsDB = getDB("nodeablemsgdb");
        String collectionName = Constants.METRIC_COLLECTION_PREFIX + metricAccount;
        DBCollection metricsCollection = metricsDB.getCollection(collectionName);

        BasicDBObject query = new BasicDBObject();
        query.put("metricName", metricName);
        query.put("metricGranularity", metricGranularity);

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        DBCursor cursor = metricsCollection.find(query).sort(new BasicDBObject("metricTimestamp", -1)).limit(2);
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            mapMongoToPlainJavaTypes(obj);
            list.add(obj.toMap());
        }
        return list;
    }

    /**
     * Simple helper to create a list of {@link BasicDBObject} from a {@link DBCursor}.
     *
     * @param cursor the cursor to create a list from
     * @return the list of items or an empty list of the query returned zero results
     */
    private List<BasicDBObject> asList(DBCursor cursor) {
        List<BasicDBObject> list = new ArrayList<BasicDBObject>();

        while (cursor.hasNext()) {
            list.add((BasicDBObject) cursor.next());
        }
        cursor.close();
        return list;
    }


    /**
     * Returns the list of inventory items in the datastore based on the
     * connection id and collection name.
     *
     * @param collectionName the collection name
     * @param connectionId   the connection id
     * @return the list of inventory items, an empty list if there are no inventory items
     */
    private List<BasicDBObject> getInventoryItems(String collectionName, String connectionId) {
        DB db = getDB("nodeabledb");
        BasicDBObject query = new BasicDBObject();

        query.put("connection.$id", new ObjectId(connectionId));

        return asList(db.getCollection(collectionName).find(query));
    }

    /**
     * Initializes the {@link Mongo} object.
     *
     * @param host     the host MongoDB is expected to be running on
     * @param port     the port MongoDB is expected to be listening on
     * @param username the username to authenticate as
     * @param password the password to the username above
     */
    private void init(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        logger.info("Mongo Client Connecting to " + host + ":" + port);

        try {
            MongoOptions options = new MongoOptions();
            options.autoConnectRetry = true;
            //options.connectionsPerHost = 50;
            options.connectTimeout = 1500;
            options.socketTimeout = 60000;
            options.threadsAllowedToBlockForConnectionMultiplier = 1000;
            //options.connectionsPerHost = 50; // * 5, so up to 250 can wait before it dies
            this.mongo = new Mongo(new ServerAddress(host, port), options);
        } catch (UnknownHostException e) {
            MongoException me = new MongoException("Unable to connect to Mongo at " +
                    host + ":" + port, e);
            logger.error(me.getMessage(), me);
            throw me;
        }
    }

    /**
     * Returns the {@link DB} object associated with the database name and
     * configuration id or null if one does not exist.  Authentication is
     * performed on the {@link DB} prior to returning if necessary.
     *
     * @param dbName the database name
     * @return the database for the given name
     */
    private DB getDB(String dbName) {
        DB db = dbMap.get(dbName);

        if (db != null) {
            return db;
        } else {
            db = mongo.getDB(dbName);
        }

        db = authenticateToDbIfNecessary(dbName, db);
        dbMap.put(dbName, db);

        return db;
    }

    private DB authenticateToDbIfNecessary(String dbName, DB db) {
        // Authenticate if necessary
        if (username != null && password != null) {
            if (db.authenticate(username, password.toCharArray())) {
                logger.debug("Successfully authenticated to MongoDB database (" + dbName +
                        ") as " + username + " on " + host + ":" + port);
            } else {
                db = mongo.getDB("admin");

                if (db.authenticate(username, password.toCharArray())) {
                    logger.debug("Successfully authenticated to MongoDB database (" + dbName +
                            ") as admin " + username + " on " + host + ":" + port);
                    db = mongo.getDB(dbName);
                } else {
                    throw new MongoException("Unable to authenticate to MongoDB using " +
                            username + "@" + dbName + " or " +
                            username + "@admin" + " on " + host +
                            ":" + port + ".");
                }
            }
        }
        return db;
    }

    public static void mapMongoToPlainJavaTypes(BSONObject obj) {
        if (obj == null) {
            return;
        }
        for (String key : obj.keySet()) {
            Object val = obj.get(key);
            if (val instanceof ObjectId) {
                obj.put(key, ((ObjectId) val).toString());
            } else if (val instanceof BasicDBObject) {
                mapMongoToPlainJavaTypes((BasicDBObject) val);
                obj.put(key, ((BasicDBObject) val).toMap());
            } else if (val instanceof BasicDBList) {
                mapMongoToPlainJavaTypes((BasicDBList) val);
                obj.put(key, new HashSet<Object>(((BasicDBList) val).toMap().values()));
            }
        }
    }



}
