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

package com.streamreduce.rest.resource.api;

import com.mongodb.BasicDBObject;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.connections.GatewayProvider;
import com.streamreduce.rest.dto.response.AbstractOwnableResponseSobaDTO;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.InventoryItemResponseDTO;
import com.streamreduce.rest.dto.response.OutboundConfigurationResponseDTO;

public abstract class AbstractOwnableResource extends AbstractTagableSobaResource {


    protected <T extends AbstractOwnableResponseSobaDTO> T toOwnerDTO(SobaObject sobaObject, T baseResponseDTO) {
        super.toBaseDTO(sobaObject, baseResponseDTO);

        User currentUser = securityService.getCurrentUser();
        baseResponseDTO.setOwner(sobaObject.getUser().getId().equals(currentUser.getId()));

        return baseResponseDTO;
    }

    protected InventoryItemResponseDTO toFullDTO(InventoryItem inventoryItem) {
        InventoryItemResponseDTO dto = new InventoryItemResponseDTO();
        Connection connection = inventoryItem.getConnection();
        BasicDBObject payload = applicationManager.getInventoryService().getInventoryItemPayload(inventoryItem);

        super.toBaseDTO(inventoryItem, dto);

        dto.setOwner(inventoryItem.getUser().getId().equals(securityService.getCurrentUser().getId()));

        dto.setConnectionAlias(connection.getAlias());
        dto.setConnectionId(connection.getId());
        dto.setConnectionType(connection.getType());
        dto.setConnectionProviderId(connection.getProviderId());

        dto.setExternalId(inventoryItem.getExternalId());
        dto.setType(inventoryItem.getType());

        // Prune any sensitive information from the payload
        if (!dto.isOwner()) {
            if (payload.containsField("adminPassword")) {
                payload.removeField("adminPassword");
            }
            if (payload.containsField("credentials")) {
                payload.removeField("credentials");
            }
        }

        dto.setPayload(payload);

        return dto;
    }

    protected ConnectionResponseDTO toFullDTO(Connection connection) {
        ConnectionResponseDTO dto = new ConnectionResponseDTO();

        // set user
        this.toOwnerDTO(connection, dto);

        dto.setType(connection.getType());
        dto.setUrl(connection.getUrl());
        dto.setProviderId(connection.getProviderId());
        dto.setAuthType(connection.getAuthType());
        dto.setOutboundConfigurations(OutboundConfigurationResponseDTO.toDTOs(connection.getOutboundConfigurations()));

        // return the identity for GMG connections only
        // TODO: this is really just to get the Autogenerated key... better way to do this?
        if (connection.getCredentials() != null && GatewayProvider.TYPE.equals(connection.getType()) && dto.isOwner()) {
            dto.setIdentity(connection.getCredentials().getIdentity());
        }
        return dto;
    }
}
