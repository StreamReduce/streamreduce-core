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

package com.streamreduce.rest.dto.response;

import com.mongodb.BasicDBObject;
import com.streamreduce.core.model.dto.ObjectIdSerializer;

import org.bson.types.ObjectId;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public abstract class AbstractConnectionInventoryItemResponseDTO extends AbstractOwnableResponseSobaDTO {

    private ObjectId connectionId;
    private String connectionAlias;
    private String connectionType;
    private String connectionProviderId;
    private BasicDBObject payload;

    public String getConnectionAlias() {
        return connectionAlias;
    }

    public void setConnectionAlias(String connectionAlias) {
        this.connectionAlias = connectionAlias;
    }

    @JsonSerialize(using = ObjectIdSerializer.class)
    public ObjectId getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(ObjectId connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getConnectionProviderId() {
        return connectionProviderId;
    }

    public void setConnectionProviderId(String connectionProviderId) {
        this.connectionProviderId = connectionProviderId;
    }

    public BasicDBObject getPayload() {
        return payload;
    }

    public void setPayload(BasicDBObject payload) {
        this.payload = payload;
    }

}
