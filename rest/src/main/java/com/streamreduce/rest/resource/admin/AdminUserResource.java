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
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.util.SecurityUtil;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Methods in this class should not be publically available. They are for internal and super-user use only!
 */
@Component
@Path("admin/user")
public class AdminUserResource extends AbstractAdminResource {

    @Autowired
    private UserService userService;

    /**
     * Usernames are unique in the DB. This is a convenience method to determine if a username already exists in the system.
     *
     * @param username - username to check
     * @return - http status code
     * @response.representation.200.doc Returned if the username is available
     * @response.representation.400.doc if the username is null or empty
     * @response.representation.409.doc Returned if the username is NOT available
     */
    @HEAD
    @Path("available/{username}")
    public Response checkUsernameAvailability(@PathParam("username") String username) {
        if (isEmpty(username)) {
            return error("Username path param can not be empty.", Response.status(Response.Status.BAD_REQUEST));
        }
        logger.debug("Check email as username availability for " + username);
        if (userService.isUsernameAvailable(username)) {
            return Response.ok().build();
        } else {
            return error("Username not available", Response.status(Response.Status.CONFLICT));
        }
    }

    /**
     * Get the User object given the username. ObjectId is not supported, only a valid username
     *
     * @param username - a valid nodeable username
     * @return - a single user DTO
     * @response.representation.200.doc Returned if the user is found based on the username
     * @response.representation.404.doc Returned if the username is not found
     * @response.representation.400.doc if the username is null or empty
     */
    @GET
    @Path("{username}")
    public Response getUser(@PathParam("username") String username) {

        if (isEmpty(username)) {
            return error("Username path param can not be empty.", Response.status(Response.Status.BAD_REQUEST));
        }
        logger.debug("Get user by username " + username);
        User user;
        try {
            user = userService.getUser(username);
        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
        return Response.ok(toDTO(user)).build();
    }

    /**
     * Creates a new user, represented by the JSON payload, for the given account ID.
     *
     * @response.representation.201.doc Returned if the username is successfully created
     * @response.representation.409.doc Returned if the username is NOT available
     *
     * @param accountId
     * @param json
     * @return the response object
     */
    @POST
    @Path("{accountId}")
    public Response createUser(@PathParam("accountId") String accountId, JSONObject json) {
        String username = getJSON(json, "username");
        if (isEmpty(username)) {
            return error("Username cannot be empty.", Response.status(Response.Status.BAD_REQUEST));
        }

        Account account = null;
        try {
            account = userService.getAccount(new ObjectId(accountId));
        }
        catch (AccountNotFoundException anfe) {
            return error("No account found with specified ID.", Response.status(Response.Status.BAD_REQUEST));
        }

        if (!userService.isUsernameAvailable(username)) {
            return error("Username '" + username + "' is currently in use.", Response.status(Response.Status.BAD_REQUEST));
        }

        boolean adminRole = (json.containsKey("role") && json.getString("role").equals("admin"));

        User user = new User.Builder()
                .fullname(getJSON(json, "fullname"))
                .password(getJSON(json, "password"))
                .username(username)
                .roles(adminRole ? userService.getAdminRoles() : userService.getUserRoles())
                .account(account)
                .build();

        // create the base nodeable user -- no account yet
        userService.createUser(user);

        return Response.status(Response.Status.CREATED).entity(toDTO(user)).build();
    }

    /**
     * Resend the new user request email if the user has not already been activated.
     * All this really does is refire the event to trigger the email service.
     *
     * @param username - the Nodeable username to resend the invite too
     * @return http status code
     * @response.representation.200.doc Returned if the email was resent successfully
     * @response.representation.400.doc Returned if the user has already been activated
     * @response.representation.404.doc if the username is not found
     */
    @GET
    @Path("resend/{username}")
    public Response recreateUserRequest(@PathParam("username") String username) {

        if (isEmpty(username)) {
            return error("Username path param can not be empty.", Response.status(Response.Status.BAD_REQUEST));
        }
        try {
            User user = userService.getUser(username);
            if (user.getUserStatus().equals(User.UserStatus.ACTIVATED)) {
                return error("User has already been activated.", Response.status(Response.Status.BAD_REQUEST));
            }
            userService.recreateUserRequest(user);
        } catch (UserNotFoundException e) {
            return error("User not found.", Response.status(Response.Status.NOT_FOUND));
        }

        return Response.ok().build();
    }


    /**
     * Send the password reset email for the specified username if the user has been activated already.
     *
     * @param username - the email/username to send the email too
     * @param mobile   - if it's a mobile device request, this should be set to true
     * @return - http status code
     * @response.representation.200.doc Returned if the email was resent successfully
     * @response.representation.404.doc Returned if the user is not found
     * @response.representation.400.doc Returned if the user has not been activated yet
     */
    @GET
    @Path("email/password/{username}")
    public Response sendPasswordResetEmail(@PathParam("username") String username,
                                           @DefaultValue("false") @QueryParam("mobile") boolean mobile) {

        if (isEmpty(username)) {
            return error("Username path param can not be empty.", Response.status(Response.Status.BAD_REQUEST));
        }
        User user;
        try {
            user = userService.getUser(username);
            // this can only be done for for active users
            if (!user.getUserStatus().equals(User.UserStatus.ACTIVATED)) {
                return error("User has not been activated, perhaps you need to resend the activation email?", Response.status(Response.Status.BAD_REQUEST));
            }

            // TODO: SOBA-421 check the activity log to see how many times this has been done.
            // if it's > N in the last X hours, return an error

            // create a secret key for the email
            // it will allow us to confirm and identify the user
            String secretKey = (SecurityUtil.generateRandomString());
            user.setSecretKey(secretKey);

            // email will be sent on event
            userService.resetUserPassword(user, mobile);

        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
        return Response.ok().build();
    }

    /**
     * Delete a user and all the resources they own. This operation can not be undone.
     *
     * @param userId - the ObjectId of the user to remove
     * @return - http status code
     * @response.representation.200.doc Returned if the user was deleted successfully
     * @response.representation.404.doc Returned if the user is not found
     */
    @DELETE
    @Path("{id}")
    public Response removeUser(@PathParam("id") ObjectId userId) {
        try {
            User user = userService.getUserById(userId);

            if (user.getUserStatus().equals(User.UserStatus.PENDING)) {
                // lightweight delete since user hasn't been totally setup
                userService.deletePendingUser(user);
            } else {
                userService.deleteUser(user);
            }
        } catch (UserNotFoundException e) {
            return error("User not found.", Response.status(Response.Status.NOT_FOUND));
        }
        return Response.ok().build();
    }

}
