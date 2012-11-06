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
