package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Transient;
import com.google.common.collect.Lists;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>Encapsulation of several general attributes used to define how messages and payloads are sent outbound from
 * Nodeable.  An OutboundConfiguration is meant to always be associated with an inbound Connection and
 * represents the destination where @{@link Event}s and {@link com.streamreduce.core.model.messages.SobaMessage}s created in
 * Nodeable that are related to the Connection are also sent to.</p>
 *
 *<p>Specifically an OutboundConfiguration contains a reference to ConnectionCredentials, a Connection where payloads
 * originate from, and also the following fields that configure the nature of the OutboundConfiguration:</p>
 *
 * <ul>
 *     <li><b>protocol (required)</b> - The protocol used for the connection.  For instance an AWS Connection might have
 *     "s3" or "dynamodb" as the protocol.</li>
 *     <li><b>destination</b> - A destination within the connection accessed through the protocol where
 *     the messages will go. This field is may be optional, depending on the protocol, and a sane default for
 *     destination will be used if necessary. Examples of a destination would be an S3 region or a DynamoDB database.
 *     </li>
 *     <li><b>namespace</b> - A location within a destination where events and messages will be placed.  This field is
 *     optional and a sane default for namespace will be used if necessary.  Examples of a namespace would be an S3
 *     bucket or a DynamoDB table.</li>
 *     <li><b>dataTypes</b> -  The type of messages/events/payloads that will be delivered to this OutboundConfiguration.
 *     All available datatypes are listed as enum values on {@link OutboundDataType}</li>
 * </ul>
 *
 * @see com.streamreduce.core.model.Connection#getOutboundConfigurations()
 */
public class OutboundConfiguration {
    @NotEmpty
    private String protocol;
    private String destination;
    private String namespace;
    @Embedded
    @Valid
    private ConnectionCredentials credentials;
    private Set<OutboundDataType> dataTypes; // user configured values they want to receive

    @Transient
    private transient Connection originatingConnection;

    private OutboundConfiguration() {}

    /**
     * The protocol supported by a specific Connection used.  This is a required attribute and is guaranteed
     * to be non-null and non-blank.
     *
     * @return The protocol supported by a specific Connection used.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * The destination where messages will be sent on the Connection/protocol.  This is an optional field.
     * @return - The string identifying the destination, or an empty String if the destination is not set.
     */
    public String getDestination() {
        if (StringUtils.isNotBlank(destination)) { return destination;}
        else { return "";}
    }

   /**
    * The namespace underneath the destination within a Connection/protocol where messages will be sent.
    * This is an optional field.
    * @return - The string identifying the namespace, or an empty String if the namespace is not set.
    */
    public String getNamespace() {
        if (StringUtils.isNotBlank(namespace)) { return namespace;}
        else { return "";}
    }

    public Set<OutboundDataType> getDataTypes() {
        return dataTypes;
    }

    /**
     * Returns a reference to the Connection that represents the inbound Connection feeding payloads to this Outbound
     * Connection
     * @return The inbound Connection
     */
    public Connection getOriginatingConnection() {
        return originatingConnection;
    }

    /**
     * Returns the {@link ConnectionCredentials} used for authentication by this OutboundConfiguration.
     * @return The ConnectionCredentials.
     */
    public ConnectionCredentials getCredentials() {
        return credentials;
    }

    /**
     * Sets the inbound Connection that is feeding payloads to this OutboundConfiguration.  This field is marked with
     * package-private access as it should only be set by the containing inbound Connection.
     * @param originatingConnection - The inbound Connection
     */
    void setOriginatingConnection(Connection originatingConnection) {
        this.originatingConnection = originatingConnection;
    }



    /**
     * Merges the following properties from a JSON object in to the Connection object:
     * <ul>
     * <li>credentials</li>
     * <li>destination</li>
     * <li>namespace</li>
     * <li>dataTypes - overwrite only</li>
     * </ul>
     *
     * @param json {@link JSONObject} used to defined properties of this Connection.
     */
    public void mergeWithJSON(JSONObject json) {
        destination = json.containsKey("destination") ? json.getString("destination") : destination;
        namespace = json.containsKey("namespace") ? json.getString("namespace") : namespace;

        if (json.containsKey("credentials")) {
            JSONObject credentialsJSON = json.getJSONObject("credentials");
            if (credentialsJSON.containsKey("username")) {
                credentials.setIdentity(credentialsJSON.getString("username"));
            }
            if (credentialsJSON.containsKey("password")) {
                credentials.setCredential(credentialsJSON.getString("password"));
            }
        }
        if (json.containsKey("dataTypes")) {
            JSONArray dataTypesJSON = json.getJSONArray("dataTypes");
            if (dataTypesJSON.size() > 0) {
                dataTypes.clear();
                for (Iterator iter = dataTypesJSON.iterator(); iter.hasNext();) {
                    String dataType = ((String) iter.next()).toUpperCase();
                    dataTypes.add(OutboundDataType.valueOf(dataType));
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OutboundConfiguration) {
            OutboundConfiguration that = (OutboundConfiguration) o;
            return new EqualsBuilder()
                    .append(this.protocol, that.protocol)
                    .append(this.destination, that.destination)
                    .append(this.namespace, that.namespace)
                    .append(this.dataTypes, that.dataTypes)
                    .append(this.credentials, that.credentials)
                    .isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(protocol)
                .append(destination)
                .append(namespace)
                .append(dataTypes)
                .append(credentials)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "OutboundConfiguration {" +
                "protocol='" + protocol + '\'' +
                ", destination='" + destination + '\'' +
                ", namespace='" + namespace + '\'' +
                ", credentials.identity=" + credentials.getIdentity() +
                ", dataTypes=" + dataTypes +
                ", originatingConnection.id=" + originatingConnection.toString() +
                '}';
    }


    public static class Builder {
        private OutboundConfiguration theObject;

        public Builder() {
            theObject = new OutboundConfiguration();
        }

        public OutboundConfiguration build() {
            if (StringUtils.isBlank(theObject.protocol) || CollectionUtils.isEmpty(theObject.dataTypes) ||
                    theObject.credentials == null || StringUtils.isBlank(theObject.credentials.getIdentity())) {

                throw new IllegalStateException("OutboundConfiguration must have a non-blank protocol, " +
                        "a non-empty set of dataTypes, and non-null credentials with the identity set.");
            }

            return theObject;
        }

        public Builder destination(String destination) {
            theObject.destination = destination;
            return this;
        }

        public Builder protocol(String protocol) {
            theObject.protocol = protocol;
            return this;
        }

        public Builder namespace(String namespace) {
            theObject.namespace = namespace;
            return this;
        }

        public Builder dataTypes(OutboundDataType... dataTypes) {
            if (dataTypes.length > 0) {
                theObject.dataTypes = EnumSet.copyOf(Lists.newArrayList(dataTypes));
            }
            return this;
        }

        public Builder credentials(ConnectionCredentials credentials) {
            theObject.credentials = credentials;
            return this;
        }

        public Builder originatingConnection(Connection connection) {
            theObject.originatingConnection = connection;
            return this;
        }
    }
}
