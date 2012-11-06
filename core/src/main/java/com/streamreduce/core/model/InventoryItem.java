package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Reference;
import com.mongodb.DBObject;

import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;

@Entity(value = "inventoryItems", noClassnameStored = true)
public class InventoryItem extends SobaObject {

    private static final long serialVersionUID = 6833894297362799580L;
    @Reference(ignoreMissing = true)
    @NotNull
    private Connection connection;
    private boolean deleted;
    @NotNull
    private String externalId;
    @NotEmpty
    private String type;
    @NotNull
    private ObjectId metadataId;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ObjectId getMetadataId() {
        return metadataId;
    }

    public void setMetadataId(ObjectId metadataId) {
        this.metadataId = metadataId;
    }

    @SuppressWarnings("rawtypes")
    public static class Builder extends SobaObject.Builder<InventoryItem, Builder> {

        public Builder() {
            super(new InventoryItem());
        }

        public Builder connection(Connection connection) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setConnection(connection);
            // inherit the connections visibility -- can you override.
            theObject.setVisibility(connection.getVisibility());
            return getRealBuilder();
        }

        public Builder deleted(boolean deleted) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setDeleted(deleted);
            return getRealBuilder();
        }

        public Builder externalId(String externalId) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setExternalId(externalId);
            return getRealBuilder();
        }

        public Builder type(String type) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setType(type);
            return getRealBuilder();
        }

        public Builder metadata(DBObject metadata) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setMetadataId((ObjectId) metadata.get("_id"));
            return getRealBuilder();
        }

        public Builder metadataId(ObjectId metadataId) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setMetadataId(metadataId);
            return getRealBuilder();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder getRealBuilder() {
            return this;
        }

    }

}
