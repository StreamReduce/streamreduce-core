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

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ConnectionProviderResponseDTO {

    private static String OAUTH_ENDPOINT_PATTERN = "/api/oauth/providers/%s";

    private String id;
    private String type;
    private String displayName;
    private AuthTypeResponsesDTO authTypes;

    private ConnectionProviderResponseDTO() {}

    /**
     * Converts a given ConnectionProvider into a DTO used for marshaling.
     *
     * @param connectionProvider The ConnectionProvider to be converted.
     * @param showAuthTypes      boolean value that specifies whether details about supported authentication types are included
     * @return ConnectionProviderResponseDTO that represents the ConnectionProvider
     */
    public static ConnectionProviderResponseDTO toDTO(ConnectionProvider connectionProvider, boolean showAuthTypes) {
        ConnectionProviderResponseDTO providerDTO = new ConnectionProviderResponseDTO();
        providerDTO.displayName = connectionProvider.getDisplayName();
        providerDTO.id = connectionProvider.getId();
        providerDTO.type = connectionProvider.getType();

        if (showAuthTypes) {
            addAuthTypesToConnectionProvidersResponseDTO(connectionProvider, providerDTO);
        }

        return providerDTO;
    }

    private static void addAuthTypesToConnectionProvidersResponseDTO(ConnectionProvider connectionProvider, ConnectionProviderResponseDTO providerDTO) {
        //TODO: (@W) This needs some serious refactoring
        List<AuthTypeResponseDTO> authTypeResponseDTOList = new ArrayList<>();
        for (AuthType supportedAuthType : connectionProvider.getSupportedAuthTypes()) {
            AuthTypeResponseDTO.Builder authTypeDTOBuilder = new AuthTypeResponseDTO.Builder();
            authTypeDTOBuilder.type(supportedAuthType.toString());
            if (supportedAuthType.equals(AuthType.USERNAME_PASSWORD)) {
                authTypeDTOBuilder.usernameLabel(connectionProvider.getUsernameLabel());
                authTypeDTOBuilder.passwordLabel(connectionProvider.getPasswordLabel());
            }
            if (supportedAuthType.equals(AuthType.OAUTH)) {
                authTypeDTOBuilder.oauthEndpoint(String.format(OAUTH_ENDPOINT_PATTERN,connectionProvider.getId()));
                authTypeDTOBuilder.commandLabel("Connect to " + connectionProvider.getDisplayName());
            }
            authTypeResponseDTOList.add(authTypeDTOBuilder.build());
        }
        AuthTypeResponsesDTO authTypeResponsesDTO = new AuthTypeResponsesDTO();
        authTypeResponsesDTO.setAuthTypes(authTypeResponseDTOList);
        providerDTO.authTypes =  authTypeResponsesDTO;
    }

    /**
     * Converts a given ConnectionProvider into a DTO used for marshaling.  By default this does not include
     * details about authentication types supported by a ConnectionProvider in the ResponseDTO.
     *
     * @param connectionProvider The ConnectionProvider to be converted.
     * @return ConnectionProviderResponseDTO that represents the ConnectionProvider
     */
    protected static ConnectionProviderResponseDTO toDTO(ConnectionProvider connectionProvider) {
        return toDTO(connectionProvider, false);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AuthTypeResponsesDTO getAuthTypes() {
        return authTypes;
    }
}
