package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.streamreduce.util.JSONUtils;
import com.streamreduce.util.SecurityUtil;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.ScriptAssert;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ScriptAssert.List({
        @ScriptAssert(script = "_this.userStatus.toString() == 'PENDING' || (_this.alias != undefined && _this.alias.trim().length() > 0)",
                lang = "javascript",
                message = "alias may not be empty"), // Alias is required for non-pending users
        @ScriptAssert(script = "_this.userStatus.toString() == 'PENDING' || (_this.fullname != undefined && _this.fullname.trim().length() > 0)",
                lang = "javascript",
                message = "fullname may not be empty"), // Fullname is required for non-pending users
        @ScriptAssert(script = "_this.userStatus.toString() == 'PENDING' || _this.account != undefined",
                lang = "javascript",
                message = "account may not be empty") // Account is required for non-pending users
})
@Entity(value = "users", noClassnameStored = true)
public class User extends SobaObject {

    private static final long serialVersionUID = -8309096404012045338L;
    private static final ImmutableSet<String> REQUIRED_CONFIG_KEYS = new ImmutableSet.Builder<String>().add(
            ConfigKeys.GRAVATAR_HASH,
            ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS,
            ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS)
            .build();


    @Indexed(unique = true)
    @Email
    @NotEmpty
    protected String username;
    @NotEmpty
    private String password;
    // @NotNull <<< Handled by ScriptAssert above
    private String fullname;
    private String secretKey;  // to activate account, password change key, whatever is needed
    @NotEmpty
    private String fuid;  // for internal use only
    private boolean accountOriginator;
    @Reference
    @NotNull
    private Set<Role> roles = new HashSet<Role>();
    private boolean userLocked = true;
    @NotNull
    @SuppressWarnings("unchecked")
    private Map<String,Object> userConfig = Maps.newHashMap(new ImmutableMap.Builder()
            .put(ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, true)
            .put(ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS, true).build());

    @Transient
    @JsonIgnore
    protected User user; // to avoid a jackson self reference we override and ignore here: SOBA-680
    // for UI rendering purposes (it just means they completed their setup process)
    private UserStatus userStatus = UserStatus.PENDING;
    // the "api key" for API requests
    @Valid
    @Embedded
    private APIAuthenticationToken authenticationToken;
    private Date lastActivity;

    public enum UserStatus {
        PENDING,
        ACTIVATED,
        DISABLED
    }

    /**
     * Constant values used in the userConfig map.
     */
    public static class ConfigKeys {

        public static final String GRAVATAR_HASH = "gravatarHash";
        public static final String ICON = "icon";
        public static final String LAST_READ_TIMESTAMP = "lastReadTS";
        public static final String RECEIVES_COMMENT_NOTIFICATIONS = "commentNotifications";
        public static final String RECEIVES_NEW_MESSAGE_NOTIFICATIONS = "newMessageNotifications";

    }

    private User() {
    }


    @PrePersist
    public void createFUIDIfMissing() {
        if (fuid == null) {
            fuid = SecurityUtil.createNodeableFUID(getUsername(), getCreated());
        }
    }


    @PrePersist
    public void createDefaultConfig() {
        if (userConfig == null) {
            userConfig = Maps.newHashMap(new ImmutableMap.Builder()
                    .put(ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, true)
                    .put(ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS, true).build());
        } else {
            if (!userConfig.containsKey(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS)) {
                userConfig.put(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS, true);
            }
            if (!userConfig.containsKey(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS)) {
                userConfig.put(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, true);
            }
        }
    }

    @PrePersist
    public void createGravatarhash() {
        // Create a gravatarHash user configuration entry
        userConfig.put(ConfigKeys.GRAVATAR_HASH, DigestUtils.md5Hex(getUsername().trim().toLowerCase()));
    }

    private String encryptPassword(String password) {
        return SecurityUtil.passwordEncrypt(password);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String clearTextPassword) {
        this.password = encryptPassword(clearTextPassword);
    }

    public void setPasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void addRole(Role role) {
        if (!roles.contains(role)) {
            roles.add(role);
        }
    }

    public void addRoles(Role... role) {
        Set<Role> set = new HashSet<Role>();
        set.addAll(Arrays.asList(role));
        this.roles = set;
    }

    public boolean isUserLocked() {
        return userLocked;
    }

    public void setUserLocked(boolean userLocked) {
        this.userLocked = userLocked;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(@Nullable String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Gets a copy of the configuration for this user.  Modifying the copy returned does not affect the user's config.
     * In order to modify the configuration, use one of the overloaded setConfigValue methods or
     * {@link User#appendToConfig(java.util.Map)}.
     * @return Map representing a copy of this user's config.
     */
    public Map<String,Object> getConfig() {
        return Maps.newHashMap(userConfig);
    }

    /**
     * Sets a number value identified by the passed in key in the user's config.
     *
     * @param key
     * @param value
     */
    public void setConfigValue(String key, Number value) {
        this.userConfig.put(key, value);
    }

    /**
     * Sets a String value identified by the passed in key in the user's config.
     *
     * @param key   String key for config value
     * @param value
     */
    public void setConfigValue(String key, String value) {
        this.userConfig.put(key, value);
    }


    /**
     * Appends all entries in the passed in Map to this config, and overwrites values if a key in the passed in
     * configMap exists already in the current config.
     * <p/>
     * Because JSONObject is untyped, if there is a non-string key present, that key is ignored.
     *
     * @param configMap - the new keys/values to be added/modified in the current config.
     */
    public void appendToConfig(Map<String,Object> configMap) {
        if (configMap == null) { return ;}
        userConfig.putAll(configMap);
    }

    /**
     * Sets a String value identified by the passed in key in the user's config.
     *
     * @param key   String key for config value
     * @param value
     */
    public void setConfigValue(String key, Boolean value) {
        this.userConfig.put(key, value);
    }

    /**
     * Sets a Map value identified by the passed in key in the user's config.
     *
     * @param key   String key for config value
     * @param value
     */
    public void setConfigValue(String key, Map<String,Object> value) {
        this.userConfig.put(key,JSONUtils.replaceJSONNullsFromMap(value));
    }

    /**
     * Sets a List value identified by the passed in key in the user's config.
     *
     * @param key   String key for config value
     * @param value
     */
    public void setConfigValue(String key, List<Object> value) {
        this.userConfig.put(key,JSONUtils.replaceJSONNullsFromList(value));
    }

    /**
     * Removes a config value from the User's config.
     *
     * @param key
     */
    public void removeConfigValue(String key) {
        if (REQUIRED_CONFIG_KEYS.contains(key)) {
            throw new IllegalArgumentException("Unable to removed required configuration key.  Required keys are: " +
                    REQUIRED_CONFIG_KEYS);
        }
        userConfig.remove(key);
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public String getFuid() {
        return fuid;
    }

    public void setFuid(String fuid) {
        this.fuid = fuid;
    }

    public boolean isAccountOriginator() {
        return accountOriginator;
    }

    public void setAccountOriginator(boolean accountOriginator) {
        this.accountOriginator = accountOriginator;
    }

    public APIAuthenticationToken getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(APIAuthenticationToken authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

    @Override
    @JsonIgnore
    public User getUser() {
        return this;
    }

    @Override
    @JsonIgnore
    public void setUser(User user) {
    }


    @Override
    public void mergeWithJSON(JSONObject json) {
        super.mergeWithJSON(json);

        if (json != null) {
            if (json.containsKey("fullname")) {
                setFullname(json.getString("fullname"));
            }
        }
    }

    /* Extending this class is disabled on purpose.  If you need to extend the builder, please look at
       InventoryItem.Builder to see how to do it properly.
     */
    public static final class Builder extends SobaObject.Builder<User, Builder> {

        public Builder() {
            super(new User());
        }

        public Builder(User u) {
            super(new User());
            if (u != null) {
                //Create a shallow copy using direct field access and make defensive copies of mutable references
                theObject.account = u.account;
                theObject.alias = u.alias;
                theObject.created = u.created;
                theObject.description = u.description;
                theObject.fuid = u.fuid;
                theObject.fullname = u.fullname;
                theObject.hashtags = Sets.newHashSet(u.hashtags);
                theObject.id = u.id;
                theObject.modified = u.modified;
                theObject.password = u.password;
                theObject.roles = Sets.newHashSet(u.roles);
                theObject.secretKey = u.secretKey;
                theObject.silentUpdate = u.silentUpdate;
                theObject.updatedViaREST = u.updatedViaREST;
                theObject.user = u.user != null ? new Builder(u).build() : null;
                theObject.userConfig = u.getConfig(); //create a copy of the config
                theObject.userLocked = u.userLocked;
                theObject.username = u.username;
                theObject.userStatus = u.userStatus;
                theObject.version = u.version;
                theObject.visibility = u.visibility;
            }
        }

        public Builder username(String username) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.username = username;
            return this;
        }

        public Builder password(String password) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setPassword(password);
            return this;
        }

        public Builder secretKey(String secretKey) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setSecretKey(secretKey);
            return this;
        }

        public Builder fullname(String fullname) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.fullname = fullname;
            return this;
        }

        public Builder userStatus(UserStatus userStatus) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.userStatus = userStatus;
            return this;
        }

        public Builder roles(Set<Role> roles) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.roles = roles;
            return this;
        }

        public Builder roles(Role... role) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            Set<Role> set = new HashSet<Role>();
            set.addAll(Arrays.asList(role));
            theObject.roles = set;
            return this;
        }

        public Builder accountLocked(boolean isLocked) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.userLocked = isLocked;
            return this;
        }

        public Builder userConfig(Map<String,Object> config) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.userConfig = config;
            return this;
        }

        public Builder accountOriginator(boolean accountOriginator) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.accountOriginator = accountOriginator;
            return this;
        }

        public Builder fuid(String fuid) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setFuid(fuid);
            return this;
        }

        /**
         * Allow account to be null here...
         *
         * @return
         */
        @Override
        public User build() {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }

            if (StringUtils.isBlank(theObject.username)) {
                throw new IllegalStateException("username must not be blank");
            }

            // your default alias is your username...
            if (theObject.getAlias() == null) {
                if (User.isValidUserAlias(theObject.getUsername())) {
                    theObject.setAlias(theObject.getUsername());
                } else {
                    theObject.setAlias(User.formatStringForUserAlias(theObject.getUsername()));
                }
            }

            if (theObject.getPassword() == null) {
                // random password just so it's not null
                theObject.setPassword(SecurityUtil.passwordEncrypt(SecurityUtil.generateRandomString()));
            }

            if (theObject.getSecretKey() == null) {
                // random secret key just so it's not null
                theObject.setSecretKey(SecurityUtil.generateRandomString());
            }
            // create the API token
            if (theObject.authenticationToken == null) {
                theObject.setAuthenticationToken(new APIAuthenticationToken());
            }

            return theObject;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder getRealBuilder() {
            return this;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    static public boolean isValidUserAlias(String alias) {
        String regex = "^[-\\w]+$";
        return alias.matches(regex);
    }

    static public String formatStringForUserAlias(String alias) {
        String aliasWithRemovedAtSymbol = alias.replaceAll("@", "_at_");
        String aliasWithNonAlphaNumericsOrDashesReplacedWithUnderscores =
                aliasWithRemovedAtSymbol.replaceAll("[^-\\w]", "_");
        return aliasWithNonAlphaNumericsOrDashesReplacedWithUnderscores;
    }
}
