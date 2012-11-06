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
