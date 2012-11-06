package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.core.validation.ValidConnectionProviderId;
import com.streamreduce.core.validation.ValidConnectionType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

import static com.streamreduce.ProviderIdToTypeMapping.PROVIDER_IDS_TO_TYPES_MAP;

@Entity(value = "connections", noClassnameStored = true)
public class Connection extends AbstractActivityPollingSobaObject {

    private static final long serialVersionUID = -2385566052567775030L;
    @NotEmpty
    @ValidConnectionProviderId
    private String providerId;
    @NotEmpty
    @ValidConnectionType
    private String type; // TODO: why is this a String and not enum?
    @URL
    private String url;
    @Embedded
    @Valid
    private ConnectionCredentials credentials;
    @NotNull
    private AuthType authType;
    private Set<OutboundConfiguration> outboundConfigurations;
    private boolean pollingInProgress;
    private long pollingLastExecutionTime;
    private long pollingFailedCount;
    private boolean broken;
    private String lastErrorMessage;
    private boolean disabled = false;
//    @Embedded
//    private APIAuthenticationToken authenticationToken; // TODO: for IMG?

    // All Connection instantiation must occur through Connection.Builder
    private Connection() {
    }

    /**
     * <p>An identifying string value that represents the provider.  An example of a providerId might be "github", or "jira".
     * A providerId must correspond to a type of connection as well.  For instance, a providerId of "github" implies
     * a type of "projecthosting".</p>
     * <p>All eligible values for ProviderId can be found in {@link com.streamreduce.ProviderIdConstants}.</p>
     *
     * @return String representing the provider id.
     */
    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId can't be null");
        }
        this.providerId = providerId;
    }

    /**
     * A string that represents the classification of the connection.  Examples of type might be "cloud" or
     * "projecthosting".
     *
     * @return String representing the type.
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("type can't be null");
        }
        this.type = type;
    }

    /**
     * A (base) Url used as a location for the Connection.  Some connection providers may have a static or default URL
     * and as such this field may be null and not used or required.
     *
     * @return A string representing the (base) Url for the Connection.
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        //Validity of url will be determined by @URL validator on the url field.
        this.url = StringUtils.trim(url);
    }

    /**
     * @return Returns a instance of {@link ConnectionCredentials} used to authenticate to the Connection.
     */
    public ConnectionCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(ConnectionCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Represents the type of authentication used to auth to the Connection.
     *
     * @return an {@link AuthType}
     */
    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        if (authType == null) {
            throw new IllegalArgumentException("authType can't be null");
        }
        this.authType = authType;
    }

    /**
     * A Set of {@link OutboundConfiguration} describing where messages generated for this Connection are sent to.
     *
     * @return A Set of OutboundConfigurations for this connection.
     */
    public Set<OutboundConfiguration> getOutboundConfigurations() {
        if (outboundConfigurations == null) {
            this.outboundConfigurations = new HashSet<OutboundConfiguration>();
        }
        return this.outboundConfigurations;
    }

    public void setOutboundConfigurations(Set<OutboundConfiguration> outboundConfigurations) {
        HashSet<OutboundConfiguration> filteredOutboundConfigs = new HashSet<OutboundConfiguration>();

        if (!CollectionUtils.isEmpty(outboundConfigurations)) {
            for (OutboundConfiguration outboundConfiguration : outboundConfigurations) {
                if (outboundConfiguration != null) {
                    outboundConfiguration.setOriginatingConnection(this);
                    filteredOutboundConfigs.add(outboundConfiguration);
                }
            }
        }

        this.outboundConfigurations = filteredOutboundConfigs;
    }

    /**
     * Adds an outbound configuration to the existing set of configs.
     *
     * @param outboundConfiguration outbound configuration to associate with this connection.
     */
    public void addOutboundConfiguration(OutboundConfiguration outboundConfiguration) {
        if (outboundConfiguration != null) {
            outboundConfiguration.setOriginatingConnection(this);
            getOutboundConfigurations().add(outboundConfiguration);
        }
    }

    /**
     * A boolean flag that signals whether a Connection can't be used for an indefinite amount of time until the
     * Connection is updated or the external provider can be connected and authenticated to successfully again.
     * Scenarios in which a Connection may be broken are when credentials have been changed on the external provider,
     * but the Connection's ConnectionCredentials have not been updated to reflect that change.
     *
     * @return boolean representing whether the connection is broken or not.
     */
    public boolean isBroken() {
        return broken;
    }

    /**
     * Flags a connection as being broken with a specific error message.  Calling this method causes the broken property
     * to be true and the lastErrorMessage property to be the passed in errorMessage.
     *
     * @param errorMessage - Error message detailing why this connection is broken.
     */
    public void setAsBroke(String errorMessage) {
        this.broken = true;
        this.lastErrorMessage = errorMessage;
    }


    public void setAsUnbroke() {
        lastErrorMessage = null;
        this.broken = false;
    }

    /**
     * Returns true if this connection has been disabled (likely by an admin).
     *
     * @return boolean
     */
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @return a String representing the last recorded error message on the Connection when it is broken.
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public boolean isPollingInProgress() {
        return pollingInProgress;
    }

    public void setPollingInProgress(boolean pollingInProgress) {
        this.pollingInProgress = pollingInProgress;
    }

    public long getPollingLastExecutionTime() {
        return pollingLastExecutionTime;
    }

    public void setPollingLastExecutionTime(long pollingLastExecutionTime) {
        this.pollingLastExecutionTime = pollingLastExecutionTime;
    }

    public long getPollingFailedCount() {
        return pollingFailedCount;
    }

    public void setPollingFailedCount(long pollingFailedCount) {
        this.pollingFailedCount = pollingFailedCount;
    }

//    public APIAuthenticationToken getAuthenticationToken() {
//        return authenticationToken;
//    }
//
//    public void setAuthenticationToken(APIAuthenticationToken authenticationToken) {
//        this.authenticationToken = authenticationToken;
//    }

    /**
     * Merges the following properties from a JSON object in to the Connection object:
     * <ul>
     * <li>credentials</li>
     * <li>authType</li>
     * <li>alias</li>
     * <li>description</li>
     * <li>visibility</li>
     * <li>url</li>
     * </ul>
     *
     * @param json {@link JSONObject} used to defined properties of this Connection.
     */
    @Override
    public void mergeWithJSON(JSONObject json) {
        super.mergeWithJSON(json);

        // Allow changing credentials
        if (json.containsKey("credentials")) {
            JSONObject rawNewCredentials = json.getJSONObject("credentials");

            // If rawNewCredentials is empty, we risk clobbering the old credentials. Which is bad.
            if (!rawNewCredentials.isEmpty()) {
                ConnectionCredentials newCredentials = new ConnectionCredentials();

                if (rawNewCredentials.containsKey("identity") && rawNewCredentials.getString("identity") != null) {
                    newCredentials.setIdentity(rawNewCredentials.getString("identity"));
                }

                if (rawNewCredentials.containsKey("credential") && rawNewCredentials.getString("credential") != null) {
                    newCredentials.setCredential(rawNewCredentials.getString("credential"));
                }

                if (rawNewCredentials.containsKey("api_key") && rawNewCredentials.getString("api_key") != null) {
                    newCredentials.setApiKey(rawNewCredentials.getString("api_key"));
                }

                if (rawNewCredentials.containsKey("oauthToken") && rawNewCredentials.getString("oauthToken") != null) {
                    newCredentials.setOauthToken(rawNewCredentials.getString("oauthToken"));
                }

                if (rawNewCredentials.containsKey("oauthTokenSecret") && rawNewCredentials.getString("oauthTokenSecret") != null) {
                    newCredentials.setOauthTokenSecret(rawNewCredentials.getString("oauthTokenSecret"));
                }

                if (rawNewCredentials.containsKey("oauthVerifier") && rawNewCredentials.getString("oauthVerifier") != null) {
                    newCredentials.setOauthVerifier(rawNewCredentials.getString("oauthVerifier"));
                }

                setCredentials(newCredentials);
            }
        }

        if (json.containsKey("url")) {
            setUrl(StringUtils.trim(json.getString("url")));
        }

        if (json.containsKey("authType")) {
            setAuthType(AuthType.valueOf(json.getString("authType")));
        }

        if (json.containsKey("broken")) {
            broken = json.getBoolean("broken");
        }

        if (json.containsKey("lastErrorMessage")) {
            lastErrorMessage = json.getString("lastErrorMessage");
        }

    }

    /* Extending this class is disabled on purpose.  If you need to extend the builder, please look at
      InventoryItem.Builder to see how to do it properly.
    */
    public static final class Builder extends SobaObject.Builder<Connection, Builder> {
        public Builder() {
            super(new Connection());
        }

        public Builder(JSONObject json) {
            super(new Connection());
            theObject.mergeWithJSON(json);
            if (json.containsKey("providerId")) {
                theObject.providerId = json.getString("providerId");
                theObject.type = PROVIDER_IDS_TO_TYPES_MAP.get(theObject.providerId);
            }
        }

        /**
         * Sets both the provider and type of this connection from the supplied {@link ConnectionProvider}
         *
         * @param provider ConnectionProvider that represents the provider for this Connection.
         * @return The {@link Builder} currently being built up.
         */
        public Builder provider(ConnectionProvider provider) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setProviderId(provider.getId());
            // autotag based on the provider
            theObject.addHashtag(provider.getId());
            theObject.setType(provider.getType());
            return this;
        }

        public Builder authType(AuthType authType) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setAuthType(authType);
            return this;
        }

        public Builder url(String url) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setUrl(StringUtils.trim(url));
            return this;
        }

        public Builder credentials(ConnectionCredentials credentials) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setCredentials(credentials);
            return this;
        }

        public Builder mergeWithJSON(JSONObject jsonObject) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.mergeWithJSON(jsonObject);
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            theObject.setMetadata(metadata);
            return this;
        }

        public Builder outboundConfigurations(OutboundConfiguration... outboundConfigurations) {
            Set<OutboundConfiguration> outboundConfigurationSet = Sets.newHashSet(outboundConfigurations);
            if (outboundConfigurationSet.contains(null)) {
                outboundConfigurationSet.remove(null);
            }
            theObject.setOutboundConfigurations(outboundConfigurationSet);
            return this;
        }

        @Override
        public Connection build() {
            if (theObject.getAuthType() == null) {
                throw new IllegalStateException("Connection object must have an authType set.");
            }
            sanitizeProperties();
            addInboundConnectionOnOutboundConfigurations();

            // TODO: if IMG create an API key

            return super.build();
        }

        /**
         * Adds a reference to theObject being built as the inboundConnection property on all of theObject's
         * OutboundConnections
         */
        private void addInboundConnectionOnOutboundConfigurations() {
            if (theObject.outboundConfigurations != null) {
                for (OutboundConfiguration outboundConfiguration : theObject.outboundConfigurations) {
                    outboundConfiguration.setOriginatingConnection(theObject);
                }
            }
        }

        /**
         * Ensure that properties on the Connection used for comparison aren't created with leading/trailing
         * whitespace.
         */
        private void sanitizeProperties() {
            theObject.setUrl(theObject.getUrl() != null ? theObject.getUrl().trim() : null);
            theObject.setAlias(theObject.getAlias() != null ? theObject.getAlias().trim() : null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder getRealBuilder() {
            return this;
        }
    }

    @PrePersist
    @SuppressWarnings("unused") //Used by Morphia
    void setDefaultAuthTypeIfMissing() {
        if (authType != null) { return;}

        //Set default authType if one isn't set already for any providers that existed prior to
        //authType being introduced
        if (this.getProviderId().equals(ProviderIdConstants.JIRA_PROVIDER_ID)) {
            authType = AuthType.USERNAME_PASSWORD;
        } else if (this.getProviderId().equals(ProviderIdConstants.GITHUB_PROVIDER_ID)) {
            authType = AuthType.USERNAME_PASSWORD;
        } else if (this.getProviderId().equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
            authType = AuthType.USERNAME_PASSWORD;
        } else if (this.getProviderId().equals(ProviderIdConstants.FEED_PROVIDER_ID)) {
            authType = AuthType.NONE;
        } else {
            throw new IllegalStateException("Connection cannot be persisted.  It has a ProviderType without an" +
                    " established default AuthType");
        }
    }


    @PostLoad
    @SuppressWarnings("unused")
        //Used by morphia
    void setAllOutboundConnectionsToReferenceThisAsInbound() {
        if (CollectionUtils.isNotEmpty(outboundConfigurations)) {
            for (OutboundConfiguration outboundConfiguration : outboundConfigurations) {
                outboundConfiguration.setOriginatingConnection(this);
            }
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof Connection) {
            Connection that = (Connection) o;
            return new EqualsBuilder()
                    .append(this.providerId, that.providerId)
                    .append(this.type, that.type)
                    .append(this.url, that.url)
                    .append(this.credentials, that.credentials)
                    .append(this.authType, that.authType)
                    .append(this.alias, that.alias)
                    .append(this.description, that.description)
                    .append(this.account, that.account)
                    .append(this.user, that.user)
                    .append(this.visibility, that.visibility)
                    .append(this.hashtags, that.hashtags)
                    .append(this.id, that.id)
                    .append(this.outboundConfigurations, that.outboundConfigurations)
                    .append(this.broken, that.broken)
                    .append(this.lastErrorMessage, that.lastErrorMessage)
                    .isEquals();

        }
        return false;
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.providerId)
                .append(this.type)
                .append(this.url)
                .append(this.credentials)
                .append(this.authType)
                .append(this.alias)
                .append(this.description)
                .append(this.account)
                .append(this.user)
                .append(this.visibility)
                .append(this.hashtags)
                .append(this.id)
                .append(this.outboundConfigurations)
                .append(this.broken)
                .append(this.lastErrorMessage)
                .toHashCode();
    }
}
