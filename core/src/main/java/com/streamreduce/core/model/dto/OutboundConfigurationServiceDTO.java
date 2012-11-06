package com.streamreduce.core.model.dto;

import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * DTO for OutboundConfiguration meant for use to be passed between service calls across different Nodeable systems
 * instead of being returned a REST responses for end user clients.
 *
 * This DTO encrypts the identity/credential in the embedded credentials and also includes the ObjectId of the
 * originating Connection for.
 */
public class OutboundConfigurationServiceDTO extends BaseOutboundConfigurationDTO {

    @JsonSerialize(using=ConnectionCredentialsSerializer.class)
    @JsonDeserialize(using=ConnectionCredentialsDeserializer.class)
    private ConnectionCredentials credentials;

    @JsonSerialize(using=ObjectIdSerializer.class)
    private ObjectId originatingConnectionId;

    @SuppressWarnings("unused") //Used by Jackson
    private OutboundConfigurationServiceDTO() {}

    public OutboundConfigurationServiceDTO(OutboundConfiguration outboundConfiguration) {
        super(outboundConfiguration);
        if (outboundConfiguration.getOriginatingConnection() != null) {
            originatingConnectionId = outboundConfiguration.getOriginatingConnection().getId();
        }
        credentials = ConnectionCredentials.copyOf(outboundConfiguration.getCredentials());
    }

    public ObjectId getOriginatingConnectionId() {
        return originatingConnectionId;
    }

    public void setOriginatingConnectionId(ObjectId originatingConnectionId) {
        this.originatingConnectionId = originatingConnectionId;
    }

    public ConnectionCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(ConnectionCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OutboundConfigurationServiceDTO) {
            OutboundConfigurationServiceDTO that = (OutboundConfigurationServiceDTO) o;
            return new EqualsBuilder()
                    .append(this.credentials, that.credentials)
                    .append(this.originatingConnectionId, that.originatingConnectionId)
                    .appendSuper(super.equals(o))
                    .isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.credentials)
                .append(this.originatingConnectionId)
                .appendSuper(super.hashCode())
                .toHashCode();
    }
}
