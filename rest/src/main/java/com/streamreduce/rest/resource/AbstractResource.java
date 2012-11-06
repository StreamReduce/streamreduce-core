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

package com.streamreduce.rest.resource;

import com.google.gson.Gson;
import com.streamreduce.core.ApplicationManager;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.rest.dto.response.AccountResponseDTO;
import com.streamreduce.rest.dto.response.RoleResponseDTO;
import com.streamreduce.rest.dto.response.SobaObjectResponseDTO;
import com.streamreduce.rest.dto.response.UserResponseDTO;
import com.streamreduce.security.Roles;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Produces(MediaType.APPLICATION_JSON)
public abstract class AbstractResource {

    @Autowired
    protected ApplicationManager applicationManager;
    @Autowired
    public SecurityService securityService;

    public transient Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Helper method to render error messages as the Http Response Entity
     *
     * @param string  - the error message
     * @param builder ResponseBuilder
     * @return Http Response with the error string as the entity
     */
    public Response error(String string, ResponseBuilder builder) {
        return builder
                .entity(new ErrorMessage(string))
                .build();
    }

    /**
     * Helper method to render error messages with a valid http status code. There is not entity value set.
     *
     * @param status - a valid http Response.Status code
     * @return Http Response with the error string as the entity
     */
    public Response error(Response.Status status) {
        return Response
                .status(status)
                .build();
    }

    protected boolean isEmpty(String s) {
        return !StringUtils.hasText(s);
    }

    // object or string helper
    protected boolean isNullOrEmpty(Object o) {
        if (o instanceof String) {
            return isEmpty((String) o);
        }
        return o == null;
    }

    protected String getJSON(JSONObject json, String value) {
        if (json == null) {
            return null;
        }
        return json.containsKey(value) ? json.getString(value).trim() : null;
    }

    protected  <T> T getJSON(JSONObject json, String value, Class<T> classOfT) {
        if (json == null) {
            return null;
        }
        if(json.containsKey(value)) {
            return new Gson().fromJson(json.getString(value).trim(),(Type) classOfT);
        }
        return null;
    }

    /**
     * Helper method that can be used as a security check.
     * Tests if the current logged in user has the same Id as the User object passed in the method param
     *
     * @param user - the User object to test against
     * @return true if it matches, false if not
     */
    protected boolean isOwner(User user) {
        User currentUser = securityService.getCurrentUser();
        return (user.getId().equals(currentUser.getId()));
    }

    /**
     * Helper method that can be used as a security check.
     * Tests if the current logged in user is the Owner or Account Admin
     *
     * @param user    - the User object to test against
     * @param account - the Account object to test against
     * @return true if it matches, false if not
     */
    protected boolean isOwnerOrAdmin(User user, Account account) {
        return (isOwner(user) ||
                (user.getAccount().getId().equals(account.getId()) &&
                        securityService.hasRole(Roles.ADMIN_ROLE)));
    }

    /**
     * Helper method that can be used as a security check. You can test if the current logged in user is in the account
     * you are passing as a param
     *
     * @param account - the account to test to see if the User is in.
     * @return - true if they are, false if they are not
     */
    protected boolean isInAccount(Account account) {
        User currentUser = securityService.getCurrentUser();
        return account.getId().equals(currentUser.getAccount().getId());
    }

    protected <T extends SobaObjectResponseDTO> T toBaseDTO(SobaObject sobaObject, T dto) {
        dto.setAccountId(sobaObject.getAccount().getId());
        dto.setAlias(sobaObject.getAlias());
        dto.setDescription(sobaObject.getDescription());
        dto.setHashtags(sobaObject.getHashtags());
        dto.setUserId(sobaObject.getUser().getId());
        dto.setCreated(sobaObject.getCreated());
        dto.setId(sobaObject.getId());
        dto.setModified(sobaObject.getModified());
        dto.setVisibility(sobaObject.getVisibility());
        dto.setVersion(sobaObject.getVersion());
        return dto;
    }

    protected RoleResponseDTO toDTO(Role role) {
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setDescription(role.getDescription());
        dto.setName(role.getName());
        dto.setPermissions(role.getPermissions());
        dto.setId(role.getId());
        return dto;
    }

    protected AccountResponseDTO toDTO(Account account) {
        AccountResponseDTO dto = new AccountResponseDTO();
//        dto.setBillingAddress(account.getBillingAddress());
        dto.setDescription(account.getDescription());
        dto.setFuid(account.getFuid());
        dto.setName(account.getName());
        dto.setUrl(account.getUrl());
        dto.setId(account.getId());
        return dto;
    }

    protected List<UserResponseDTO> toFullDTO(List<User> users) {
        List<UserResponseDTO> allUsers = new ArrayList<UserResponseDTO>();
        for (User user : users) {
            allUsers.add(toFullDTO(user));
        }
        return allUsers;
    }

    protected UserResponseDTO toFullDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();

        toBaseDTO(user, dto);

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
