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

package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Transient;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;

import javax.persistence.PostPersist;
import java.io.Serializable;
import java.util.Date;

/**
 * Base class used for all Nodeable models with an auto-generated identifier.
 */
public abstract class ObjectWithId implements Serializable {

    private static final long serialVersionUID = 3223021273884437957L;
    @Id
    protected ObjectId id;
    protected Long created;
    protected Long modified;
    protected int version = 0;

    // Used to validate specially when updated via REST
    @Transient
    protected boolean updatedViaREST = false;
    @Transient
    protected boolean silentUpdate = false;

    // never start with null values
    protected ObjectWithId() {
        if (created == null) {
            created = new Date().getTime();
        }
    }

    /**
     * Merge this with the contents of the JSON object, typically used for REST EPs that update an object.
     * <p/>
     * * Note: This is a base implementation and must be overridden to work as this implementation
     * will just return this unmodified.
     *
     * @param json the JSON whom to merge with
     */
    public void mergeWithJSON(JSONObject json) {
        // The default is to do nothing but the default implementation is here for those subclasses
        // that are not changeable via REST.

        // Mark the object as having been updated via REST.
        updatedViaREST = true;
    }

    @PrePersist
    public void createCreatedAndModifiedIfMissing() {
        if (created == null) {
            created = new Date().getTime();
            modified = created;
        }
    }

    @PrePersist
    public void persistModified() {
        modified = new Date().getTime();
    }

    @PrePersist
    public void incrementVersion() {
        if (!silentUpdate) {
            version++;
        }
    }

    @PostPersist
    public void postPersist() {
        // Reset the value
        updatedViaREST = false;
        silentUpdate = false;
    }

    public boolean getUpdatedViaREST() {
        return updatedViaREST;
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

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isSilentUpdate() {
        return silentUpdate;
    }

    public void setSilentUpdate(boolean silentUpdate) {
        this.silentUpdate = silentUpdate;
    }

}
