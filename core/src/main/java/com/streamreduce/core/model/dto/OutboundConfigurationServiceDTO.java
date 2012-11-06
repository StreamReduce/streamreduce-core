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
