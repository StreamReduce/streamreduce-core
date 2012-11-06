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

import com.google.common.collect.ImmutableMap;
import com.streamreduce.InvalidUserAliasException;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.util.SecurityUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Methods in this class should not be publically available. They are for internal and super-user use only!
 */
@Component
@Path("admin/user/verify")
public class AdminUserVerificationResource extends AbstractAdminResource {

    @Autowired
    UserService userService;

    /**
     * Helper method that is a simple test to confirm that the key and userId are valid -- not required to use before the actually reset process takes place, as
     * the check will be performed again in that method.
     *
     * @param key    - the key to match
     * @param userId - id if the User to load and test the key against
     * @return - the username (email address) associated with the key
     * @resource.representation.404 returned if the user is not found
     * @resource.representation.500 returned if invalid params are provided or if the key does not match what
     * we have for that User
     */
    @GET
    @Path("password/{key}/{userId}")
    public Response verifyPasswordResetIdentity(@PathParam("key") String key,
                                                @PathParam("userId") String userId) {

        if (isEmpty(key) || isEmpty(userId)) {
            return error("Path params missing", Response.status(Response.Status.BAD_REQUEST));
        }

        User user;
        try {
            user = applicationManager.getUserService().getUserById(new ObjectId(userId));

        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        // verify the secret key matches
        if (!(user.getSecretKey().equals(key))) {
            logger.debug("Password identity confirmation failure: userId and secret key pair does NOT match.");
            return error("Password Identity Confirmation Failed", Response.status(Response.Status.BAD_REQUEST));
        }

        // send the password back to the client.
        return Response
                .ok()
                .entity(user.getUsername())
                .build();
    }

    /**
     * User is confirming the password reset confirmation link and selecting a new password. The new password is passed
     * in the JSON payload. The key and userId combinations are validated here.
     *
     * @param key    - auto-generated key from the email
     * @param userId - userId
     * @param json   - the new password
     * @return - http status code
     * @resource.representation.201 returned if a new password was created for the User
     * @resource.representation.404 returned if the user is not found
     * @resource.representation.500 returned if invalid params are provided or if the key does not match
     */
    @POST
    @Path("password/{key}/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response completePasswordResetProcess(@PathParam("key") String key,
                                                 @PathParam("userId") String userId,
                                                 JSONObject json) {

        User user;
        try {
            user = applicationManager.getUserService().getUserById(new ObjectId(userId));
        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        // verify the secret key matches
        if (!(user.getSecretKey().equals(key))) {
            logger.debug("Password change confirmation failure: userId and secret key pair does NOT match.");
            return error("Password Change Confirmation Failed", Response.status(Response.Status.BAD_REQUEST));
        }

        String password = getJSON(json, "password");

        // validate password
        if (!SecurityUtil.isValidPassword(password)) {
            return error(ErrorMessages.INVALID_PASSWORD_ERROR, Response.status(Response.Status.BAD_REQUEST));
        }

        // we want to make this a one time thing, blow away the secret key
        user.setSecretKey(null);

        // update the password in the db
        user.setPassword(password);
        applicationManager.getUserService().updateUser(user);

        // send the password back to the client.
        return Response
                .status(Response.Status.CREATED)
                .build();
    }


    /**
     * Helper method that simply confirms the key and accountId association is valid -- this is not a required step for
     * a newly invited user.
     *
     * @param inviteKey - the invite key from the email
     * @param accountId - the accountId of the User
     * @return - http status code and the username (email) of user
     * @resource.representation.200 returned if the values match
     * @resource.representation.404 returned if the user is not found
     * @resource.representation.406 returned if the user is already activated
     * @resource.representation.500 returned if invalid params are provided or if the key does not match
     */
    @GET
    @Path("invite/{inviteKey}/{accountId}")
    public Response verifyUserInviteIdentity(@PathParam("inviteKey") String inviteKey,
                                             @PathParam("accountId") String accountId) {
        // check for missing required values
        if (isEmpty(inviteKey) || isEmpty(accountId)) {
            return error("Required path params missing", Response.status(Response.Status.BAD_REQUEST));
        }
        User user;
        try {

            user = applicationManager.getUserService().getUserFromInvite(inviteKey, accountId);

            // this account is already active!
            if (user.getUserStatus().equals(User.UserStatus.ACTIVATED)) {
                return error("User is already activated", Response.status(Response.Status.NOT_ACCEPTABLE));
            }

        } catch (UserNotFoundException e) {
            return error("Invalid Invite " + inviteKey + ":" + accountId, Response.status(Response.Status.NOT_FOUND));
        }

        // user created, they can now login
        return Response
                .ok()
                .entity(user.getUsername())
                .build();
    }

    /**
     * Resource to complete the setup process of an invited user. This moves the user from a pending state to a fully
     * built state. The user can only be associated with the Account they were invited to join.
     *
     * @param inviteKey - auto-generated key contained in the email
     * @param accountId - the accountId (invites require an Account) used only for verification
     * @param json      - invited User DTO with required fields such as password, alias and fullname
     * @return - http status code
     * @resource.representation.301 returned if successful
     * @resource.representation.404 returned if the user is not found
     * @resource.representation.406 returned if the user is already activated or if the alias is already taken
     * @resource.representation.500 returned if invalid params are provided
     */
    @POST
    @Path("invite/{inviteKey}/{accountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response completeUserInviteProcess(@PathParam("inviteKey") String inviteKey,
                                              @PathParam("accountId") String accountId,
                                              JSONObject json) {

        String password = getJSON(json, "password");
        String alias = getJSON(json, "alias");
        String fullname = getJSON(json, "fullname");

        if (isEmpty(password) || isEmpty(alias) || isEmpty(fullname)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        User user;

        try {

            // validate password
            if (!SecurityUtil.isValidPassword(password)) {
                return error(ErrorMessages.INVALID_PASSWORD_ERROR, Response.status(Response.Status.BAD_REQUEST));
            }

            user = userService.getUserFromInvite(inviteKey, accountId);

            // this account is already active!
            // FIXME: this status code sucks here
            if (user.getUserStatus().equals(User.UserStatus.ACTIVATED)) {
                return error("User is already activated", Response.status(Response.Status.NOT_ACCEPTABLE));
            }

            // verify alias is unique
            if (!userService.isAliasAvailable(user.getAccount(), alias)) {
                return error("Alias is not available", Response.status(Response.Status.NOT_ACCEPTABLE));
            }

            user.setPassword(password);
            user.setFullname(fullname);
            user.setAlias(alias);
            // we already know their company..so no need to set it

            // user can login
            user.setUserLocked(false);

            // we want to make this a one time thing, blow away the secret key
            user.setSecretKey(null);

            userService.createUser(user);

        } catch (UserNotFoundException e) {
            return error("Invalid Invite " + inviteKey + ":" + accountId, Response.status(Response.Status.NOT_FOUND));
        } catch (InvalidUserAliasException e) {
            ConstraintViolationExceptionResponseDTO dto = new ConstraintViolationExceptionResponseDTO();
            dto.setViolations(ImmutableMap.of("alias", e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(dto).build();
        }

        // user created, they can now login
        return Response
                .status(Response.Status.CREATED)
                .entity(user.getUsername())
                .build();
    }


    /**
     * A Helper method that can be used to verify the email sign-up key and userId are a valid combination. This is not
     * a required step during the sign-up process as validation will once again be performed in the POST method.
     *
     * @param signupKey - unique to a user and sent via email
     * @param userId    - the userId
     * @return - http status code and the username (email) of user
     * @resource.representation.200 returned if the values match
     * @resource.representation.404 returned if the user is not found
     * @resource.representation.406 returned if the user is already activated
     * @resource.representation.500 returned if invalid params are provided or if the key does not match
     */
    @GET
    @Path("signup/{signupKey}/{userId}")
    public Response verifyUserSignupIdentity(@PathParam("signupKey") String signupKey,
                                             @PathParam("userId") String userId) {

        // check for missing required values
        if (isEmpty(signupKey) || isEmpty(userId)) {
            return error("Required path params missing", Response.status(Response.Status.BAD_REQUEST));
        }

        User user;
        try {

            user = applicationManager.getUserService().getUserFromSignupKey(signupKey, userId);

            // this account is already active!
            if (user.getUserStatus().equals(User.UserStatus.ACTIVATED)) {
                return error("User is already activated", Response.status(Response.Status.NOT_ACCEPTABLE));
            }

        } catch (UserNotFoundException e) {
            return error("Invalid Signup " + signupKey + ":" + userId, Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .ok()
                .entity(user.getUsername())
                .build();
    }


    /**
     * A new user is completing the sign-up process. This moves the user from a pending state to a complete state. A confirmation
     * email should be sent out once this process is complete.
     *
     * @param signupKey - auto-generated key from the email
     * @param userId    - the userId
     * @param json      - invited User DTO with required fields such as password, alias, accountName and fullname
     * @return - http status code and username as the created entity on success
     * @resource.representation.301 returned if successful
     * @resource.representation.404 returned if the user is not found
     * @resource.representation.406 returned if the user is already activated or if the alias is already taken
     * @resource.representation.500 returned if invalid params are provided
     */
    @POST
    @Path("signup/{signupKey}/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response completeUserSignupProcess(@PathParam("signupKey") String signupKey,
                                              @PathParam("userId") String userId,
                                              JSONObject json) {

        String password = getJSON(json, "password");
        String alias = getJSON(json, "alias");
        String fullname = getJSON(json, "fullname");
        String accountName = getJSON(json, "accountName");

        if (isEmpty(password) || isEmpty(alias) || isEmpty(fullname) || isEmpty(accountName)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        User user;

        try {

            // validate password
            if (!SecurityUtil.isValidPassword(password)) {
                return error(ErrorMessages.INVALID_PASSWORD_ERROR, Response.status(Response.Status.BAD_REQUEST));
            }

            user = userService.getUserFromSignupKey(signupKey, userId);

            // this account is already active!
            if (user.getUserStatus().equals(User.UserStatus.ACTIVATED)) {
                return error("User is already activated", Response.status(Response.Status.NOT_ACCEPTABLE));
            }

            user.setPassword(password);
            user.setFullname(fullname);
            user.setAlias(alias); // no need to check, you are the first user

            // user can now login, the account is "activated"
            user.setUserLocked(false);

            // we want to make this a one time thing, blow away the secret key
            user.setSecretKey(null);

            Account account = new Account.Builder()
                    .name(accountName)
                    .build();

            // create the account and fire events
            userService.createAccount(account);
            user.setAccount(account);

            // create the user and fire events
            userService.createUser(user);

        } catch (UserNotFoundException e) {
            return error("Invalid Signup " + signupKey + ":" + userId, Response.status(Response.Status.NOT_FOUND));
        } catch (InvalidUserAliasException e) {
            ConstraintViolationExceptionResponseDTO dto = new ConstraintViolationExceptionResponseDTO();
            dto.setViolations(ImmutableMap.of("alias", e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(dto).build();
        }

        return Response
                .status(Response.Status.CREATED)
                .entity(user.getUsername()) // This should probably be some sort of response DTO but due to time this is what you'll get.
                .build();
    }

}
