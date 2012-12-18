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

import com.google.common.collect.ImmutableMap;
import com.streamreduce.Constants;
import com.streamreduce.InvalidUserAliasException;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;
import com.streamreduce.rest.dto.response.UserResponseDTO;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.Roles;
import com.streamreduce.util.JSONObjectBuilder;
import com.streamreduce.util.SecurityUtil;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

@Component
@Path("api/user")
public class UserResource extends AbstractTagableSobaResource {

    @Autowired
    UserService userService;

    /**
     * Returns the currently logged in user.
     *
     * @response.representation.200.doc The currently logged in user is returned.
     * @response.representation.200.mediaType application/json
     *
     * @return The requested item.
     */
    @GET
    public Response getCurrentUser() {
        User currentUser = securityService.getCurrentUser();
        UserResponseDTO userResponseDTO = toFullDTO(currentUser);

        return Response.ok(userResponseDTO).build();
    }

    /**
     * Returns a user looked up by ID or username.
     *
     * @response.representation.200.doc The requested user is returned
     * @response.representation.200.mediaType application/json
     * @response.representation.404.doc Returned if no such user is found
     * @response.representation.404.mediaType text/plain
     *
     * @param idOrUsername the requested ID or username
     * @return The requested user or null
     */
    @GET
    @Path("{idOrUsername}")
    public Response getUser(@PathParam("idOrUsername") String idOrUsername) {

        User currentUser = securityService.getCurrentUser();
        User requestedUser;

        try {
            if (ObjectId.isValid(idOrUsername)) {
                requestedUser = userService.getUserById(new ObjectId(idOrUsername), currentUser.getAccount());
            } else {
                requestedUser = userService.getUser(idOrUsername, currentUser.getAccount());
            }
        } catch (UserNotFoundException unfe) {
            return error("No user found with the following id: " + idOrUsername, Response.status(Response.Status.NOT_FOUND));
        }

        UserResponseDTO userResponseDTO = toFullDTO(requestedUser);
        return Response.ok(userResponseDTO).build();
    }

    /**
     * The user associated to the auth token is logged out of the application and the token is invalidated.
     *
     * @response.representation.200.doc Success status is always returned.
     * @response.representation.200.mediaType text/plain
     *
     * @return response
     */
    @GET
    @Path("logout")
    public Response logout(@HeaderParam(Constants.NODEABLE_AUTH_TOKEN) String authToken) {
        securityService.logoutCurrentUser(authToken);
        return Response
                .ok()
                .build();
    }

    /**
     * Updates password for the currently logged in user.
     *
     * @response.representation.204.doc Returned if the password was updated successfully.
     * @response.representation.400.doc Returned if the password is invalid.
     * @response.representation.400.mediaType text/plain
     *
     * @return the response object
     *
     */
    @PUT
    @Path("password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(JSONObject json) {
        // update just the password
        User u = securityService.getCurrentUser();
        String password = getJSON(json, "password");

        // validate password
        if (!SecurityUtil.isValidPassword(password)) {
            return error(ErrorMessages.INVALID_PASSWORD_ERROR, Response.status(Response.Status.BAD_REQUEST));
        }

        u.setPassword(password);
        userService.updateUser(u);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    /**
     * Updates the currently logged in user profile.
     *
     * @response.representation.204.doc Returned if the password was updated successfully.
     * @response.representation.400.doc If the request is empty, or if the user alias contains invalid characters.
     * @response.representation.400.mediaType text/plain
     * @response.representation.409.doc If the request is empty, or if the user alias is already in use.
     * @response.representation.409.mediaType text/plain
     * @response.representation.500.doc Returned if a general error occurs while updating the user profile.
     * @response.representation.500.mediaType text/plain
     *
     * @return the response object
     */
    @PUT
    @Path("profile")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserProfile(JSONObject json) {
        User user = securityService.getCurrentUser();

        if (isNullOrEmpty(json)) {
            return error("JSON Payload Missing.", Response.status(Response.Status.BAD_REQUEST));
        }

        // Handle alias changes
        if (json.containsKey("alias")) {
            String alias = json.getString("alias");

            if (!alias.equalsIgnoreCase(user.getAlias())) {
                if (!(userService.isAliasAvailable(user.getAccount(), alias))) {
                    ConstraintViolationExceptionResponseDTO dto = new ConstraintViolationExceptionResponseDTO();
                    dto.setViolations(ImmutableMap.of("alias", json.getString("alias") + " is already in use"));
                    return Response.status(Response.Status.CONFLICT).entity(dto).build();
                }
            }
        }

        user.mergeWithJSON(json);

        try {
            userService.updateUser(user);
        } catch (InvalidUserAliasException e) {
            ConstraintViolationExceptionResponseDTO dto = new ConstraintViolationExceptionResponseDTO();
            dto.setViolations(ImmutableMap.of("alias", e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(dto).build();
        } catch (Exception e) {
            //Something unexpected, so return a 500.
            return error(e.getMessage(), Response.status(Response.Status.INTERNAL_SERVER_ERROR));
        }

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();

    }

    /**
     * Updates the account profile associated to the currently logged in user.
     *
     * @response.representation.204.doc Returned if the password was updated successfully.
     * @response.representation.401.doc Returned if the user is not an account administrator.
     * @response.representation.401.mediaType text/plain
     *
     * @param json
     * @return the response object
     */
    @PUT
    @Path("account/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccountProfile(JSONObject json) {

        // require admin role
        if (!securityService.hasRole(Roles.ADMIN_ROLE)) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.UNAUTHORIZED));
        }

        User user = securityService.getCurrentUser();
        Account account = user.getAccount();

        account.mergeWithJSON(json);

        userService.updateAccount(account);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    /**
     * Deletes a specific user.
     *
     * @response.representation.400.doc Returned if the user tries to delete his/her own account.
     * @response.representation.400.mediaType text/plain
     * @response.representation.401.doc Returned if the user is not an account administrator.
     * @response.representation.401.mediaType text/plain
     * @response.representation.404.doc Returned if the user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @return the response object
     */
    @DELETE
    @Path("{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("userId") ObjectId userId) {

        User currentUser = securityService.getCurrentUser();

        // require admin role
        if (!securityService.hasRole(Roles.ADMIN_ROLE)) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.UNAUTHORIZED));
        }

        try {
            User user = userService.getUserById(userId, currentUser.getAccount());

            if (securityService.getCurrentUser().getId().equals(userId)) {
                return error("You can not delete yourself. Things will get better, hang in there.", Response.status(Response.Status.BAD_REQUEST));
            }

            userService.deleteUser(user);
        } catch (UserNotFoundException e) {
            return error("User not found.", Response.status(Response.Status.NOT_FOUND));
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Disables a user account.
     *
     * @response.representation.204.doc If the user was successfully disabled.
     * @response.representation.400.doc Returned if the user tries to disable himself.
     * @response.representation.400.mediaType text/plain
     * @response.representation.401.doc Returned if the user is not an account administrator.
     * @response.representation.401.mediaType text/plain
     * @response.representation.404.doc Returned if the user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @return the response object
     */
    @PUT
    @Path("{userId}/disable")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response disableUser(@PathParam("userId") ObjectId userId) {

        User currentUser = securityService.getCurrentUser();

        // require admin role
        if (!securityService.hasRole(Roles.ADMIN_ROLE)) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.UNAUTHORIZED));
        }

        try {
            User user = userService.getUserById(userId, currentUser.getAccount());
            user.setUserStatus(User.UserStatus.DISABLED);
            user.setUserLocked(true);

            // can't disable yourself
            if (user.getId().equals(currentUser.getId())) {
                return error("You can't disable yourself", Response.status(Response.Status.BAD_REQUEST));
            }

            // TODO: need to lock this user somehow... this will do for now
            if (user.getUsername().equals(Constants.NODEABLE_SUPER_USERNAME)) {
                return error("Can't delete super user", Response.status(Response.Status.BAD_REQUEST));
            }

            userService.updateUser(user);
        } catch (UserNotFoundException e) {
            return error("User not found.", Response.status(Response.Status.NOT_FOUND));
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Enables a previously disabled user account
     *
     * @response.representation.204.doc If the user was successfully enabled.
     * @response.representation.401.doc Returned if the user is not an account administrator.
     * @response.representation.401.mediaType text/plain
     * @response.representation.404.doc Returned if the user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @return
     */
    @PUT
    @Path("{userId}/enable")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enableUser(@PathParam("userId") ObjectId userId) {

        User currentUser = securityService.getCurrentUser();

        // require admin role
        if (!securityService.hasRole(Roles.ADMIN_ROLE)) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.UNAUTHORIZED));
        }
        try {
            User user = userService.getUserById(userId, currentUser.getAccount());
            user.setUserStatus(User.UserStatus.ACTIVATED);
            user.setUserLocked(false);
            userService.updateUser(user);
        } catch (UserNotFoundException e) {
            return error("User not found.", Response.status(Response.Status.NOT_FOUND));
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Returns the currently logged in user's configuration.
     *
     * @response.representation.200.doc Returns the user's configuration preferences.
     * @response.representation.200.mediaType application/json
     *
     * @return
     */
    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() {
        User currentUser = securityService.getCurrentUser();
        return Response.ok(currentUser.getConfig()).build();
    }

    /**
     * Sets the currently logged in user's configuration preferences.
     *
     * @response.representation.200.doc Returns the user's updated configuration preferences.
     * @response.representation.200.mediaType application/json
     * @response.representation.400.doc Returned if a general exception occurs.
     * @response.representation.400.mediaType text/plain
     *
     * @param jsonObject
     * @return
     */
    @POST
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setConfigValues(JSONObject jsonObject) {
        User currentUser = securityService.getCurrentUser();
        try {
            currentUser.appendToConfig(jsonObject);
            userService.updateUser(currentUser);
            return Response.ok(currentUser.getConfig()).build();
        } catch (Exception e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }
    }

    /**
     * Removes a configuration preference for the currently logged in user.
     *
     * @response.representation.201.doc Returns the user's updated configuration preferences.
     * @response.representation.400.doc Returned if a general exception occurs.
     * @response.representation.400.mediaType text/plain
     * @response.representation.404.doc Returned if the configuration preference does not exist.
     * @response.representation.404.mediaType text/plain
     *
     * @param key
     * @return the response object
     */
    @DELETE
    @Path("config/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConfigKey(@PathParam("key") String key) {
        User currentUser = securityService.getCurrentUser();
        Map<String,Object> userConfig = currentUser.getConfig();
        if (!userConfig.containsKey(key)) {
            return error(key + " does not exist", Response.status(Response.Status.NOT_FOUND));
        }
        try {
            currentUser.removeConfigValue(key);
            userService.updateUser(currentUser);
            return Response.noContent().build();
        } catch (Exception e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }
    }

    /**
     * Returns configuration preference values for a specific key.
     *
     * @response.representation.200.doc The configuration preference value.
     * @response.representation.200.mediaType application/json
     * @response.representation.404.doc Returned if the configuration preference does not exist.
     * @response.representation.404.mediaType text/plain
     *
     * @param key
     * @return
     */
    @GET
    @Path("config/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigValueByKey(@PathParam("key") String key) {
        User currentUser = securityService.getCurrentUser();
        Map<String,Object> userConfig = currentUser.getConfig();
        if (userConfig.containsKey(key)) {
            return Response.ok(
                    new JSONObjectBuilder().add(key, userConfig.get(key)).build()
            ).build();
        } else {
            return error(key + " does not exist", Response.status(Response.Status.NOT_FOUND));
        }
    }

    /**
     * Adds hashtags to a given user account.
     *
     * @response.representation.204.doc Successful update.
     * @response.representation.400.doc Returned if the hashtag payload is empty
     * @response.representation.400.mediaType text/plain
     * @response.representation.404.doc Returned if the user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @param json
     * @return the response object
     */
    @POST
    @Path("{userId}/hashtag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public Response addTag(@PathParam("userId") String userId, JSONObject json) {

        String hashtag = getJSON(json, HASHTAG);

        User currentUser = securityService.getCurrentUser();

        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            User user = userService.getUserById(new ObjectId(userId), currentUser.getAccount());
            user.addHashtag(hashtag);
            userService.updateUser(user);

        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
        return Response.noContent().build();
    }

    /**
     * Returns the hashtags associated with a given account.
     *
     * @response.representation.200.doc Hashtags associated with the user.
     * @response.representation.200.mediaType text/plain
     * @response.representation.404.doc Returned if the user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @return
     */
    @GET
    @Path("{userId}/hashtag")
    @Override
    public Response getTags(@PathParam("userId") String userId) {

        User currentUser = securityService.getCurrentUser();

        Set<String> tags;
        try {
            User user = userService.getUserById(new ObjectId(userId), currentUser.getAccount());
            tags = user.getHashtags();

        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .ok(tags)
                .build();

    }

    /**
     * Removes a hashtag from a user account.
     *
     * @response.representation.200.doc Hashtags associated with the user.
     * @response.representation.200.mediaType text/plain
     * @response.representation.400.doc Returned if the user is not found.
     * @response.representation.400.mediaType text/plain
     * @response.representation.404.doc Returned if the user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @param hashtag
     * @return
     */
    @DELETE
    @Path("{userId}/hashtag/{tagname}")
    @Override
    public Response removeTag(@PathParam("userId") String userId, @PathParam("tagname") String hashtag) {

        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }
        User currentUser = securityService.getCurrentUser();

        try {
            User user = userService.getUserById(new ObjectId(userId), currentUser.getAccount());
            user.removeHashtag(hashtag);
            userService.updateUser(user);

        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
        return Response.ok().build();
    }

    /**
     * Adds a role to a given user.
     *
     * @response.representation.201.doc Role added.
     * @response.representation.201.mediaType text/plain
     * @response.representation.400.doc If the role identifier is empty.
     * @response.representation.400.mediaType text/plain
     * @response.representation.401.doc If the requesting user does not have the admin role.
     * @response.representation.401.mediaType text/plain
     * @response.representation.404.doc If the target user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @param roleId
     * @return the response object
     */
    @POST
    @Path("{userId}/roles/{roleId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRole(@PathParam("userId") ObjectId userId, @PathParam("roleId") ObjectId roleId) {

        if (isNullOrEmpty(roleId)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        // require admin role
        if (!securityService.hasRole(Roles.ADMIN_ROLE)) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.UNAUTHORIZED));
        }

        User currentUser = securityService.getCurrentUser();

        try {

            User user = userService.getUserById(userId, currentUser.getAccount());
            userService.addRole(user, roleId);

        } catch (UserNotFoundException e) {
            return error("User not found.", Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .status(Response.Status.CREATED)
                .build();
    }

    /**
     * Removes a role from a target user.
     *
     * @response.representation.201.doc Role added.
     * @response.representation.201.mediaType text/plain
     * @response.representation.400.doc If the role identifier is empty, or if the user tries to remove the admin role from himself/herself.
     * @response.representation.400.mediaType text/plain
     * @response.representation.401.doc If the requesting user does not have the admin role.
     * @response.representation.401.mediaType text/plain
     * @response.representation.404.doc If the target user is not found.
     * @response.representation.404.mediaType text/plain
     *
     * @param userId
     * @param roleId
     * @return the response object
     */
    @DELETE
    @Path("{userId}/roles/{roleId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeRole(@PathParam("userId") ObjectId userId, @PathParam("roleId") ObjectId roleId) {

        if (isNullOrEmpty(roleId)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        // require admin role
        if (!securityService.hasRole(Roles.ADMIN_ROLE)) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.UNAUTHORIZED));
        }

        User currentUser = securityService.getCurrentUser();

        try {

            Role adminRole = securityService.findRole(Roles.ADMIN_ROLE);
            if (currentUser.getId().equals(userId) && adminRole.getId().equals(roleId)) {
                // you can't remove the admin role from yourself!
                return error("You can not remove the administrator role from your own user.", Response.status(Response.Status.BAD_REQUEST));
            }

            User user = userService.getUserById(userId, currentUser.getAccount());
            userService.removeRole(user, roleId);

        } catch (UserNotFoundException e) {
            return error("User not found.", Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .ok()
                .build();
    }

}
