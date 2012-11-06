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

import com.streamreduce.Constants;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.rest.dto.response.RoleResponseDTO;
import com.streamreduce.rest.dto.response.RolesResponseDTO;
import com.streamreduce.rest.resource.AbstractResource;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.Roles;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Path("api/account")
public class AccountResource extends AbstractResource {


    /**
     * Get the available account details. This does not require any kind of special user permission.
     *
     * @return - http status code and account DTO
     * @resource.representation.200.doc if the operation was a success
     * @resource.representation.404.doc returned if the account to get users for is not found
     */
    @GET
    public Response getAccount() {
        User currentUser = applicationManager.getSecurityService().getCurrentUser();

        Account account;
        try {
            account = applicationManager.getUserService().getAccount(currentUser.getAccount().getId());
        } catch (AccountNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response.ok(toDTO(account)).build();
    }

    /**
     * A list of all the users in the account. This does not require any kind of special user permission.
     * This does not return disabled users in the list, but does include pending users.
     *
     * @return - http status code and of users in the account
     * @resource.representation.200.doc if the operation was a success
     * @resource.representation.404.doc returned if the account to get users for is not found
     */
    @GET
    @Path("users")
    public Response getAccountUsers() {

        User currentUser = applicationManager.getSecurityService().getCurrentUser();
        List<User> users;
        try {
            Account account = applicationManager.getUserService().getAccount(currentUser.getAccount().getId());
            users = applicationManager.getUserService().allEnabledUsersForAccount(account);
        } catch (AccountNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response.ok(toFullDTO(users)).build();
    }


    /**
     * A list of users that have done something in the last 60 seconds. We define active as hitting the API in any way. Of course,
     * as we implement caching via memcached or something, we will need a heartbeat API method of some sort, since the "getMessages"
     * API that we rely on now won't be hit nearly as much.
     *
     * @return - http status code and list of the active users in the account
     * @resource.representation.200.doc if the operation was a success
     * @resource.representation.404.doc returned if the account to get users for is not found
     */
    @GET
    @Path("users/active")
    public Response getActiveLoggedInUsers() {
        User currentUser = applicationManager.getSecurityService().getCurrentUser();
        Set<User> theUsers;
        try {
            Account account = applicationManager.getUserService().getAccount(currentUser.getAccount().getId());
            theUsers = applicationManager.getSecurityService().getActiveUsers(account, (Constants.PERIOD_MINUTE * 3));
        } catch (AccountNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
        List<User> users = new ArrayList<User>(theUsers);

        return Response.ok(toFullDTO(users)).build();

    }

    /**
     * Get a list of available roles. Currently we have global roles and permissions, but eventually we will allow accounts to create their own.
     *
     * @return - http status code and list of available roles
     * @resource.representation.200.doc if the operation was a success
     * @resource.representation.401.doc returned if current logged in user making the request is not in the admin role
     * @resource.representation.404.doc returned if the account to get roles for is not found
     */
    @GET
    @Path("roles")
    public Response getAccountRoles() {

        // require admin role
        if (!applicationManager.getSecurityService().hasRole(Roles.ADMIN_ROLE)) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.UNAUTHORIZED));
        }

        User currentUser = applicationManager.getSecurityService().getCurrentUser();

        Set<Role> roles;
        try {
            roles = applicationManager.getUserService().getAccountRoles(currentUser.getAccount().getId());
        } catch (AccountNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        RolesResponseDTO dto = new RolesResponseDTO();
        List<RoleResponseDTO> roleDTOs = new ArrayList<RoleResponseDTO>();

        for (Role role : roles) {
            roleDTOs.add(toDTO(role));
        }

        dto.setRoles(roleDTOs);

        return Response.ok(dto).build();
    }

}
