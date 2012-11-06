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

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.util.MessageUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

/**
 * Methods in this class should not be publically available. They are for internal and super-user use only!
 */
@Component
@Path("admin/message")
public class AdminMessageResource extends AbstractAdminResource {


    /**
     * Add a global message so *everyone* will get it. This is the Nodeable "wall" command. The message is copied
     * to all account inboxes. If no username is provided it will use the Nodebelly.
     *
     * @param username -- allow to send as a specified user
     * @param json     - global message to inject into the stream
     * @return - http status code
     * @resource.representation.201 if the messages was created ok
     * @resource.representation.406 if the username provided is not valid
     * @resource.representation.500 returned if invalid params are provided
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addGlobalMessage(@QueryParam("username") String username, JSONObject json) {

        String message = getJSON(json, "message");
        JSONArray tags = json.has("tags") && json.getJSONObject("tags").isArray() ?
                json.getJSONArray("tags") :
                new JSONArray();

        if (isEmpty(message)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD + " message", Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            User sender;
            if (isEmpty(username)) {
                sender = applicationManager.getUserService().getSuperUser();
            } else {
                sender = applicationManager.getUserService().getUser(username);
            }

            MessageUtils.ParsedMessage parsedMessage = MessageUtils.parseMessage(message);
            Set<String> hashtags = parsedMessage.getTags();

            for (Object rawHashtag : tags) {
                hashtags.add((String) rawHashtag);
            }

            Map<String, Object> eventMetadata = new HashMap<String, Object>();

            eventMetadata.put("message", parsedMessage);
            eventMetadata.put("messageHashtags", hashtags);
            eventMetadata.put("payload", json);
            eventMetadata.put("senderId", sender.getId());
            eventMetadata.put("senderAccountId", sender.getAccount().getId());

            Event event = applicationManager.getEventService().createEvent(EventId.CREATE_GLOBAL_MESSAGE,
                    null, eventMetadata);

            applicationManager.getMessageService().sendNodebellyGlobalMessage(event, sender, null, new Date().getTime(),
                    MessageType.SYSTEM, hashtags);
        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_ACCEPTABLE));
        }
        return Response
                .status(Response.Status.CREATED)
                .build();
    }

    /**
     * Add a message to just one account inbox. This is the Nodeable "account wall" command.
     *
     * @param accountId - the account to send this too
     * @param json      - global message to inject into the stream of the account
     * @return - http status code
     * @resource.representation.201 if the messages was created ok
     * @resource.representation.406 if the username provided is not valid
     * @resource.representation.500 returned if invalid params are provided
     */
    @POST
    @Path("{accountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addGlobalAccountMessage(@PathParam("accountId") String accountId, JSONObject json) {

        String message = getJSON(json, "message");
        JSONArray tags = json.has("tags") && json.getJSONObject("tags").isArray() ?
                json.getJSONArray("tags") :
                new JSONArray();

        if (isEmpty(message)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD + " message", Response.status(Response.Status.BAD_REQUEST));
        }

        Account account;
        try {

            // this will allow the message to be saved in the inbox of the account
            account = applicationManager.getUserService().getAccount(new ObjectId(accountId));

            MessageUtils.ParsedMessage parsedMessage = MessageUtils.parseMessage(message);
            Set<String> hashtags = parsedMessage.getTags();

            for (Object rawHashtag : tags) {
                hashtags.add((String) rawHashtag);
            }

            Map<String, Object> eventMetadata = new HashMap<String, Object>();
            eventMetadata.put("message", message);
            eventMetadata.put("payload", json);

            Event event = applicationManager.getEventService().createEvent(EventId.CREATE_GLOBAL_MESSAGE, null,
                    eventMetadata);

            applicationManager.getMessageService().sendNodeableAccountMessage(event, account, hashtags);

        } catch (AccountNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }
        return Response
                .status(Response.Status.CREATED)
                .build();
    }


}
