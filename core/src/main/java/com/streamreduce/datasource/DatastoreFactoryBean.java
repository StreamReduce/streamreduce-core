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

/**
 * Copyright (C) 2010 SarathOnline.com.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamreduce.datasource;

import com.google.code.morphia.AdvancedDatastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.StringUtils;

public class DatastoreFactoryBean extends AbstractFactoryBean<AdvancedDatastore> {

    private Morphia morphia;
    private Mongo mongo;
    private String dbName;
    private String user;
    private String password;
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Class<?> getObjectType() {
        return AdvancedDatastore.class;
    }

    @Override
    protected AdvancedDatastore createInstance() throws Exception {
        AdvancedDatastore datastore;

        if (StringUtils.hasText(user)) {
            // If there is a username supplied, we need to authenticate.  Morphia does not
            // allow us to use a MongoDB admin user so we need to do some setup work prior
            // to involving Morphia.  To do this, we will first try to authenticate as a
            // database user and if that fails, we will try to authenticate as an admin
            // user.  If both fail, we let the MongoException bubble up as it is unhandleable.
            logger.debug("Create the morphia datastore with credentials");

            // Try to login as a database user first and fall back to admin user if login fails
            if (mongo.getDB(dbName).authenticate(user, password.toCharArray())) {
                logger.debug("Successfully authenticated to MongoDB database (" + dbName +
                        ") as " + user);
            } else {
                if (mongo.getDB("admin").authenticate(user, password.toCharArray())) {
                    logger.debug("Successfully authenticated to MongoDB database (" + dbName +
                            ") as admin " + user);
                } else {
                    throw new MongoException("Unable to authenticate to MongoDB using " +
                            user + "@" + dbName + " or " + user + "@admin.");
                }
            }
        } else {
            logger.debug("Create the morphia datastore WITHOUT credentials");
        }

        datastore = (AdvancedDatastore) morphia.createDatastore(mongo, dbName);

        datastore.ensureCaps();
        datastore.ensureIndexes();

        return datastore;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (mongo == null) {
            throw new IllegalStateException("mongo is not set");
        }
        if (morphia == null) {
            throw new IllegalStateException("morphia is not set");
        }
    }

    public void setMorphia(Morphia morphia) {
        this.morphia = morphia;
    }

    public void setMongo(Mongo mongo) {
        this.mongo = mongo;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
