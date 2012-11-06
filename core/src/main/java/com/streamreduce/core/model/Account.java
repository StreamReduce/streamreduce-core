package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.PrePersist;
import com.streamreduce.util.SecurityUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.Size;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;


@Entity(value = "accounts", noClassnameStored = true)
public class Account extends ObjectWithId {

    private static final long serialVersionUID = -4090080979360697012L;
    @NotEmpty
    @Size(min = 2, max = 64)
    private String name;
    @Size(max = 256)
    private String description;
    @URL
    private String url;
    private String fuid; // internal UniqueId (name + creation_time)
    private Set<ObjectId> publicConnectionBlacklist; // SOBA-1885, ability to blacklist PUBLIC or bootstrapped connections

    // boolean flags for different config and notification settings
    private Map<ConfigKey, Boolean> configMap = new HashMap<ConfigKey, Boolean>();

    // all values will default to false if null, so make sure that's a logical when you add new ones
    public enum ConfigKey {
        RECIEVED_INSIGHTS, // used to toggle if they should get an insight email
        ACCOUNT_LOCKED,   // ability to block *all* users
        DISABLE_INBOUND_API,  // SOBA-1684
        RECEIVED_TRIAL_EXPIRING_NOTIFICATION,
        RECEIVED_TRIAL_EXPIRED_NOTIFICATION
    }

    private Account() {
    }

    @PrePersist
    public void createFUIDIfMissing() {
        if (fuid == null) {
            fuid = SecurityUtil.createNodeableFUID(getName(), getCreated());
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFuid() {
        return fuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFuid(String fuid) {
        this.fuid = fuid;
    }

    public Set<ObjectId> getPublicConnectionBlacklist() {
        if (publicConnectionBlacklist == null) {
            // do this so we don't persist an empty list
            // not do we have stupid null checks.
            return new HashSet<ObjectId>();
        }
        return publicConnectionBlacklist;
    }

    public void appendToPublicConnectionBlacklist(ObjectId connectionId) {
        if (publicConnectionBlacklist == null) {
            publicConnectionBlacklist = new HashSet<ObjectId>();
        }
        publicConnectionBlacklist.add(connectionId);
    }

    @Override
    public void mergeWithJSON(JSONObject json) {
        super.mergeWithJSON(json);

        if (json != null) {
            if (json.containsKey("name")) {
                setName(json.getString("name"));
            }
            if (json.containsKey("description")) {
                setDescription(json.getString("description"));
            }
            if (json.containsKey("url")) {
                setUrl(json.getString("url"));
            }
        }
    }

    public static final class Builder {

        private Account theObject;
        private boolean isBuilt;

        public Builder() {
            theObject = new Account();
        }

        public Builder name(String name) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.name = name;
            return this;
        }

        public Builder description(String description) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.description = description;
            return this;
        }

        public Builder url(String url) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.url = url;
            return this;
        }



        public Account build() {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }

            if (StringUtils.isBlank(theObject.name)) {
                throw new IllegalStateException("name must not be blank");
            }

            isBuilt = true;
            return theObject;
        }

    }

    public boolean getConfigValue(ConfigKey key) {
        Boolean value = configMap.get(key);
        return value != null && value;
    }

    public void setConfigValue(ConfigKey key, boolean value) {
        configMap.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        return !(fuid != null ? !fuid.equals(account.fuid) : account.fuid != null);

    }

    @Override
    public int hashCode() {
        return fuid != null ? fuid.hashCode() : 0;
    }


}

