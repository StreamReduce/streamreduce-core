package com.streamreduce.core.model.dto;

import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import net.sf.json.JSONObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class OutboundConfigurationWithPayloadDTO {

    OutboundDataType dataType;
    OutboundConfigurationServiceDTO outboundConfiguration;
    String payload;

    @SuppressWarnings("unused") //required for JSON deserialization
    private OutboundConfigurationWithPayloadDTO() {}

    public OutboundConfigurationWithPayloadDTO (OutboundConfiguration outboundConfiguration, String payload,
                                                OutboundDataType dataType) {
            this(new OutboundConfigurationServiceDTO(outboundConfiguration),payload,dataType);
    }

    public OutboundConfigurationWithPayloadDTO(OutboundConfigurationServiceDTO outboundConfiguration,
                                               String payload, OutboundDataType dataType) {
        if (outboundConfiguration == null || payload == null  || dataType == null) {
            throw new IllegalArgumentException("outboundConfiguration, payload, and dataType must be non-null");
        }
        try {
            JSONObject.fromObject(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("payload must be a valid json object");
        }

        this.outboundConfiguration = outboundConfiguration;
        this.payload = payload;
        this.dataType = dataType;
    }

    public OutboundConfigurationServiceDTO getOutboundConfiguration() {
        return outboundConfiguration;
    }

    public String getPayload() {
        return payload;
    }

    public OutboundDataType getDataType() {
        return dataType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(outboundConfiguration)
                .append(payload)
                .append(dataType)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OutboundConfigurationWithPayloadDTO) {
            OutboundConfigurationWithPayloadDTO that  = (OutboundConfigurationWithPayloadDTO) o;

            return new EqualsBuilder()
                    .append(this.outboundConfiguration, that.outboundConfiguration)
                    .append(this.payload, that.payload)
                    .append(this.dataType, that.dataType)
                    .isEquals();
        }
        return false;
    }
}
