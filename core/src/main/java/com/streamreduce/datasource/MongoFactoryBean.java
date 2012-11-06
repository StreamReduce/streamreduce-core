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

package com.streamreduce.datasource;

import com.mongodb.Mongo;
import com.mongodb.MongoOptions;

import com.mongodb.ServerAddress;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.StringUtils;

public class MongoFactoryBean extends AbstractFactoryBean<Mongo> {

    private String hostname;
    private int port = 27017;
    private boolean autoConnectRetry = true;
    private int connectionsPerHost = 50;
    private int connectionTimeout = 15000;
    private int socketTimeout = 60000;
    private int threadsAllowedToBlockForConnectionMultiplier = 1000;

    @Override
    public Class<?> getObjectType() {
        return Mongo.class;
    }

    @Override
    protected Mongo createInstance() throws Exception {
        if (StringUtils.hasText(this.hostname )) {
            return new Mongo(new ServerAddress(hostname,port), getMongoOptions());
        } else {
            return new Mongo();
        }
    }

    private MongoOptions getMongoOptions() {
        MongoOptions options = new MongoOptions();
        options.autoConnectRetry = autoConnectRetry;
        options.connectionsPerHost = connectionsPerHost;
        options.connectTimeout = connectionTimeout;
        options.socketTimeout = socketTimeout;
        options.threadsAllowedToBlockForConnectionMultiplier = threadsAllowedToBlockForConnectionMultiplier;
        return options;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAutoConnectRetry(boolean autoConnectRetry) {
        this.autoConnectRetry = autoConnectRetry;
    }

    public void setConnectionsPerHost(int connectionsPerHost) {
        this.connectionsPerHost = connectionsPerHost;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setThreadsAllowedToBlockForConnectionMultiplier(int threadsAllowedToBlockForConnectionMultiplier) {
        this.threadsAllowedToBlockForConnectionMultiplier = threadsAllowedToBlockForConnectionMultiplier;
    }
}
