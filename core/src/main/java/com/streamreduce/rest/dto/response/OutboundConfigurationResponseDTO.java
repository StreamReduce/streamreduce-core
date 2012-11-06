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

package com.streamreduce.rest.dto.response;

import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.dto.BaseOutboundConfigurationDTO;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class OutboundConfigurationResponseDTO extends BaseOutboundConfigurationDTO {
    private String identity;

    public static OutboundConfigurationResponseDTO toDTO(OutboundConfiguration outboundConfiguration) {
        OutboundConfigurationResponseDTO dto = new OutboundConfigurationResponseDTO(outboundConfiguration);
        return dto;
    }

    public OutboundConfigurationResponseDTO(OutboundConfiguration outboundConfiguration) {
        super(outboundConfiguration);
        ConnectionCredentials creds = outboundConfiguration.getCredentials();
        if (creds != null && StringUtils.isNotBlank(creds.getIdentity())) {
            this.identity = creds.getIdentity();
        }
    }

    public static List<OutboundConfigurationResponseDTO> toDTOs(Collection<OutboundConfiguration> outboundConfigurations) {
        List<OutboundConfigurationResponseDTO> outboundDTOs = new ArrayList<OutboundConfigurationResponseDTO>();
        for (OutboundConfiguration outboundConfiguration : outboundConfigurations) {
            if (outboundConfiguration == null) { continue; }
                outboundDTOs.add(toDTO(outboundConfiguration));
        }
        return outboundDTOs;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
