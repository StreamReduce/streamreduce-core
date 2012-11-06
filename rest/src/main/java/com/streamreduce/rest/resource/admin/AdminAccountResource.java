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

import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Methods in this class should not be publically available. They are for internal and super-user use only!
 */
@Component
@Path("admin/account")
public class AdminAccountResource extends AbstractAdminResource {

    @Autowired
    private UserService userService;
    @Autowired
    private ConnectionService connectionService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(JSONObject json) {
        if (getJSON(json, "name") == null) {
            return error("Accounts must have a name", Response.status(Response.Status.BAD_REQUEST));
        }

        Account account = new Account.Builder()
                .name(getJSON(json, "name"))
                .description(getJSON(json, "description"))
                .url(getJSON(json, "url"))
                .build();
        userService.createAccount(account);

        return Response.status(Response.Status.CREATED).entity(toDTO(account)).build();
    }

    /**
     * Get the full Account DTO object
     *
     * @param accountId - a valid accountId
     * @return Account DTO
     * @resource.representation.200 Returned when the account is found
     * @resource.representation.404 Returned when the id is not found
     */
    @GET
    @Path("{id}")
    public Response getAccount(@PathParam("id") ObjectId accountId) {
        logger.debug("Get accountId " + accountId);
        Account account;
        try {
            account = userService.getAccount(accountId);
        } catch (AccountNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response.ok(toDTO(account)).build();
    }

    /**
     * A User alias is unique within an account, so check if it's already in use in the given AccountId
     *
     * @param id    - the AccountId
     * @param alias - the alias to test
     * @return http status code
     * @resource.representation.200 return if the alias is available
     * @resource.representation.409 return if the alias is already in use
     * @resource.representation.500 returned if the accountId is invalid
     */
    @HEAD
    @Path("{id}/alias/{alias}")
    public Response checkAliasAvailability(@PathParam("id") String id, @PathParam("alias") String alias) {

        if (isEmpty(alias)) {
            return error("alias path param can not be empty.", Response.status(Response.Status.BAD_REQUEST));
        }

        logger.debug("Check alias availability for " + alias);

        Account account = null;
        try {
            userService.getAccount(new ObjectId(id));
        } catch (AccountNotFoundException e) {
            return error("Account not found", Response.status(Response.Status.BAD_REQUEST));
        }

        if (userService.isAliasAvailable(account, alias)) {
            return Response.ok().build();
        } else {
            return error(Response.Status.CONFLICT);
        }
    }

    /**
     * Disable a Nodeable account, this sets the boolean flag on the account that will block all User login attempts.
     * It does NOT delete the account.
     *
     * @param id - the AccountId
     * @return http status code
     * @resource.representation.204 if the operation was a success
     * @resource.representation.500 returned if the accountId is invalid
     */
    @PUT
    @Path("{id}/disable")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response disableAccount(@PathParam("id") ObjectId id) {

        try {
            Account account = userService.getAccount(id);

            // TODO: need a better way to lock this...
            User superUser = userService.getSuperUser();
            if (superUser.getAccount().getId().equals(id)) {
                return error("Cannot disable Nodeable account.", Response.status(Response.Status.BAD_REQUEST));
            }

            account.setConfigValue(Account.ConfigKey.ACCOUNT_LOCKED, true);
            userService.updateAccount(account);
        } catch (AccountNotFoundException e) {
            return error("Account not found.", Response.status(Response.Status.BAD_REQUEST));
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Enable a Nodeable account, this sets the boolean flag on the account that will enable a disaled account.
     * It does NOT delete the account.
     *
     * @param id - the AccountId
     * @return http status code
     * @resource.representation.204 if the operation was a success
     * @resource.representation.500 returned if the accountId is invalid
     */
    @PUT
    @Path("{id}/enable")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enableAccount(@PathParam("id") ObjectId id) {

        try {
            Account account = userService.getAccount(id);
            account.setConfigValue(Account.ConfigKey.ACCOUNT_LOCKED, false);
            userService.updateAccount(account);
        } catch (AccountNotFoundException e) {
            return error("Account not found.", Response.status(Response.Status.BAD_REQUEST));
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Disable inbound gateway use for this account
     *
     * @param id - the AccountId
     * @return http status code
     * @resource.representation.204 if the operation was a success
     * @resource.representation.500 returned if the accountId is invalid
     */
    @PUT
    @Path("{id}/disable/api")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response disableInboundAPIForAccount(@PathParam("id") ObjectId id) {

        try {
            Account account = userService.getAccount(id);

            // TODO: need a better way to lock this...
            User superUser = userService.getSuperUser();
            if (superUser.getAccount().getId().equals(id)) {
                return error("Can not disable Nodeable account.", Response.status(Response.Status.BAD_REQUEST));
            }

            account.setConfigValue(Account.ConfigKey.DISABLE_INBOUND_API, true);
            userService.updateAccount(account);
        } catch (AccountNotFoundException e) {
            return error("Account not found.", Response.status(Response.Status.BAD_REQUEST));
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Enable inbound gateway use for this account
     *
     * @param id - the AccountId
     * @return http status code
     * @resource.representation.204 if the operation was a success
     * @resource.representation.500 returned if the accountId is invalid
     */
    @PUT
    @Path("{id}/enable/api")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enableInboundAPIForAccount(@PathParam("id") ObjectId id) {

        try {
            Account account = userService.getAccount(id);
            account.setConfigValue(Account.ConfigKey.DISABLE_INBOUND_API, false);
            userService.updateAccount(account);
        } catch (AccountNotFoundException e) {
            return error("Account not found.", Response.status(Response.Status.BAD_REQUEST));
        }
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    /**
     * Remove an account from the Nodeable platform and all associated Users.
     * Messages or resource related payloads are not deleted.
     *
     * @param accountId - the accountId to remove
     * @return a valid http status code
     * @resource.representation.200 Returned when the account is successfully deleted
     */
    @DELETE
    @Path("{id}")
    public Response removeAccount(@PathParam("id") ObjectId accountId) {
        Account account = null;
        try {
            account = userService.getAccount(accountId);
        } catch (AccountNotFoundException e) {
            return error("Account not found.", Response.status(Response.Status.BAD_REQUEST));
        }
        /* all this used to happen in an event listener,,,, */

        List<Connection> connectionList = connectionService.getAccountConnections(account);
        for (Connection c : connectionList) {
            // this should also handle jobs and inventory items
            connectionService.deleteConnection(c);
        }
        // remove users and then finally the account
        userService.deleteUsersForAccount(account);
        userService.deleteAccount(accountId);

        return Response.ok().build();
    }


}


