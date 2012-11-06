package com.streamreduce.rest.dto.response;

import com.streamreduce.core.model.dto.ObjectIdSerializer;

import org.bson.types.ObjectId;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class ObjectWithIdResponseDTO {

    private ObjectId id;
    private Long created;
    private Long modified;
    private int version;

    @JsonSerialize(using=ObjectIdSerializer.class)
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getModified() {
        return modified;
    }

    public void setModified(Long modified) {
        this.modified = modified;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

}
