package com.streamreduce.core.model.dto;

import com.google.common.collect.Lists;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.List;

public abstract class BaseOutboundConfigurationDTO {

    private String protocol;
    private String destination;
    private String namespace;
    private List<OutboundDataType> dataTypes;

    BaseOutboundConfigurationDTO() {
    }

    public BaseOutboundConfigurationDTO(OutboundConfiguration outboundConfiguration) {
        if (outboundConfiguration == null) {
            throw new IllegalArgumentException("outboundConfiguration can't be null");
        }
        this.destination = outboundConfiguration.getDestination();
        this.protocol = outboundConfiguration.getProtocol();
        this.namespace = outboundConfiguration.getNamespace();
        this.dataTypes = Collections.unmodifiableList(
                Lists.newArrayList(outboundConfiguration.getDataTypes()));
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<OutboundDataType> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(List<OutboundDataType> dataTypes) {
        this.dataTypes = dataTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BaseOutboundConfigurationDTO) {
            BaseOutboundConfigurationDTO that = (BaseOutboundConfigurationDTO) o;
            return new EqualsBuilder()
                    .append(this.dataTypes, that.dataTypes)
                    .append(this.destination, that.destination)
                    .append(this.namespace, that.namespace)
                    .append(this.protocol, that.protocol)
                    .isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.dataTypes)
                .append(this.destination)
                .append(this.namespace)
                .append(this.protocol)
                .toHashCode();

    }
}
