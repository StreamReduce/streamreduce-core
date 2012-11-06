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

package com.streamreduce.rest.resource.admin;

import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.rest.dto.response.RoleResponseDTO;
import com.streamreduce.rest.dto.response.UserResponseDTO;
import com.streamreduce.rest.resource.AbstractResource;

import java.util.HashSet;
import java.util.Set;

/**
 * Methods in this class should not be publically available. They are for internal and super-user use only!
 */
public abstract class AbstractAdminResource extends AbstractResource {

    protected UserResponseDTO toDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();

        // do not use the toBaseDTO() since accountId could be null.
        if (user.getAccount() != null) {
            dto.setAccountId(user.getAccount().getId());
        }
        dto.setAlias(user.getAlias());
//        dto.setDescription(user.getDescription());
        dto.setHashtags(user.getHashtags());
        dto.setUserId(user.getUser().getId());
        dto.setCreated(user.getCreated());
        dto.setId(user.getId());
        dto.setModified(user.getModified());
        dto.setVisibility(user.getVisibility());

        dto.setAccountOriginator(user.isAccountOriginator());
        dto.setFullname(user.getFullname());
        Set<RoleResponseDTO> rolesDTOs = new HashSet<RoleResponseDTO>();
        for (Role role : user.getRoles()) {
            rolesDTOs.add(toDTO(role));
        }
        dto.setRoles(rolesDTOs);
        dto.setStatus(user.getUserStatus());
        dto.setUserConfig(user.getConfig());
        dto.setUsername(user.getUsername());
        return dto;
    }

}
