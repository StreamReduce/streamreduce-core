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

package com.streamreduce.rest.resource.appcelerator;

import com.google.common.collect.ImmutableSet;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.dao.AccountDAO;
import com.streamreduce.core.dao.UserDAO;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.*;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.EventService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.ConnectionExistsException;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.core.service.exception.InventoryItemNotFoundException;
import com.streamreduce.rest.resource.ErrorMessage;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.appcelerator.Constants.*;

/**
 * REST resource for consuming all events from Appcelerator.  The idea is that event structures, documented
 * <a href="https://wiki.appcelerator.org/display/cls/Example+Analytics+JSON+payloads">here</a>, will be parsed and
 * mapped to internal StreamReduce objects (Accounts, Users, Connections, etc.) on an as-needed basis.
 */
@Component
@DependsOn("bootstrapDatabaseDataPopulator")
@Path("appcelerator")
public class AppceleratorEventsResource implements InitializingBean {

    private final String INVALID_REQUEST_PREFIX = "Invalid event structure: ";
    private final SimpleDateFormat TIMESTAMP_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Autowired
    private AccountDAO accountDAO;
    @Autowired
    ConnectionProviderFactory connectionProviderFactory;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private EventService eventService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserDAO userDAO;

    private Account appceleratorAccount;
    private User appceleratorUser;

    /**
     * Consumes raw JSON events from Appcelerator and creates Nodeable events as a result.
     *
     * @param eventPayload the JSON representing the event payload
     *
     * @resource.representation.200 Returned when the event is processed successfully
     * @resource.representation.405 Returned whenever the event payload is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleEvent(JSONObject eventPayload) {
        // The process for consuming Appcelerator events works like this:
        //
        //   1) Parse out the necessary pieces available in the payload:
        //     * aguid: Application id
        //     * mid  : Machine/Device ID
        //   2) Lookup Nodeable objects based on the Appcelerator ids
        //   3) Create necessary objects to correspond with the Appcelerator ids (if necessary)
        //   4) Create event
        //   5) Return response

        // Quick return if the Appcelerator account is null
        if (appceleratorAccount == null) {
            return createError(Response.Status.INTERNAL_SERVER_ERROR, "Appcelerator account not bootstrapped.");
        }

        // Quick return if the Appcelerator user is null
        if (appceleratorUser == null) {
            return createError(Response.Status.INTERNAL_SERVER_ERROR, "Appcelerator user not bootstrapped.");
        }

        // Quick return if we know the request is guaranteed to be invalid.
        if (eventPayload == null || eventPayload.size() == 0) {
            return createInvalidRequestError("Empty event structure");
        }

        // Based on the wiki referenced in the class' javadocs, 'aguid' is guaranteed to always be in the payload and is
        // used to map to a Connection object.
        if (!eventPayload.containsKey("aguid")) {
            return createInvalidRequestError("'aguid' is a required event property");
        }

        // Based on the wiki referenced in the class' javadocs, 'mid' is guaranteed to always be in the payload and is
        // used to map to an InventoryItem object.
        if (!eventPayload.containsKey("mid")) {
            return createInvalidRequestError("'mid' is a required event property");
        }

        String appceleratorEvent = eventPayload.containsKey("event") ? eventPayload.getString("event") : "unknown";
        String rawTimestamp = eventPayload.containsKey("ts") ? eventPayload.getString("ts") : null;
        String aguid = eventPayload.getString("aguid");
        List<Connection> connections = connectionService.getConnectionsByExternalId(aguid, appceleratorUser);
        String mid = eventPayload.getString("mid");
        Connection connection = null;
        InventoryItem inventoryItem;

        if (connections != null && connections.size() > 0) {
            // Pluck the first connection (Not sure how better to handle this)
            connection = connections.get(0);
        } else {
            // Create a new connection
            try {
                connection = connectionService.createConnection(new Connection.Builder()
                        .authType(AuthType.API_KEY)
                        .provider(connectionProviderFactory.connectionProviderFromId(
                                ProviderIdConstants.CUSTOM_PROVIDER_ID)
                        )
                        .account(appceleratorAccount)
                        .user(appceleratorUser)
                        .alias("Mobile Application (" + aguid + ")")
                        .description("Mobile Appcelerator application.")
                        .externalId(aguid)
                        .hashtags(ImmutableSet.of("#appcelerator", "#app"))
                        .build());
            } catch (ConnectionExistsException e) {
                // Should never happen
                return createError(Response.Status.INTERNAL_SERVER_ERROR,
                        "Connection already exists for mobile application (" + aguid + ").");
            } catch (InvalidCredentialsException e) {
                // Should never happen
                return createError(Response.Status.INTERNAL_SERVER_ERROR, "Invalid connection credentials.");
            } catch (IOException e) {
                // Should never happen
                return createError(Response.Status.INTERNAL_SERVER_ERROR, "I/O exception: " + e.getMessage());
            }
        }

        try {
            inventoryItem = inventoryService.getInventoryItemForExternalId(connection, mid);
        } catch (InventoryItemNotFoundException e) {
            // Create an inventory item automatically just like the IMG does
            try {
                JSONObject inventoryItemAsJSON = new JSONObject();

                // Mirror an IMG inventory item (Only hashtags and inventoryItemId are used)
                inventoryItemAsJSON.put("hashtags", ImmutableSet.of("#appcelerator", "#device"));
                inventoryItemAsJSON.put("inventoryItemId", mid);

                inventoryItem = inventoryService.createInventoryItem(connection, inventoryItemAsJSON);
            } catch (ConnectionNotFoundException e1) {
                // Should never happen
                return createError(Response.Status.INTERNAL_SERVER_ERROR,
                        "Connection not found for external id (" + mid + ")");
            } catch (InvalidCredentialsException e1) {
                // Should never happen
                return createError(Response.Status.INTERNAL_SERVER_ERROR, "Invalid connection credentials.");
            } catch (IOException e1) {
                // Should never happen
                return createError(Response.Status.INTERNAL_SERVER_ERROR, "I/O exception: " + e1.getMessage());
            }
        }

        // Create the event stream entry
        Map<String, Object> eventContext = new HashMap<>();
        String message = "'" + appceleratorEvent + "' received from device.";
        Date dateGenerated = new Date();

        if (rawTimestamp != null) {
            try {
                dateGenerated = TIMESTAMP_PARSER.parse(rawTimestamp);
            } catch (ParseException e) {
                // Should never happen
                return createError(Response.Status.INTERNAL_SERVER_ERROR,
                        "Unable to parse the event timestamp: " + e.getMessage());
            }
        }

        // Persist the full payload
        eventContext.put("message", message);
        eventContext.put("dateGenerated", dateGenerated);
        eventContext.put("payload", eventPayload);

        Event event = eventService.createEvent(EventId.ACTIVITY, inventoryItem, eventContext);

        // Send the message
        // messageService.sendGatewayMessage(event, inventoryItem, dateGenerated.getTime());

        // Simply return a 200 that everything was okay (A 201 would require a URI to the thing created but that
        // doesn't make sense here.)
        return Response.ok().build();
    }

    /**
     * Helper that wraps {@link #createError(javax.ws.rs.core.Response.Status, String)} for bad request errors.
     *
     * @param message the error message
     *
     * @return the error object as JSON
     */
    private Response createInvalidRequestError(String message) {
        return createError(Response.Status.BAD_REQUEST, INVALID_REQUEST_PREFIX + message);
    }

    /**
     * Creates a {@link Response} representing an error.
     *
     * @param status the status code to use
     * @param message the error message
     *
     * @return the error object as JSON
     */
    private Response createError(Response.Status status, String message) {
        ErrorMessage errorMessage = new ErrorMessage();

        errorMessage.setErrorMessage(message);

        return Response.status(status)
                       .entity(JSONObject.fromObject(errorMessage).toString())
                       .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        appceleratorAccount = accountDAO.findByName(APPC_GENERIC_ACCOUNT_NAME);
        appceleratorUser = userDAO.findUserInAccount(appceleratorAccount, APPC_GENERIC_USERNAME);

        if (appceleratorAccount == null) {
            appceleratorAccount = userService.createAccount(
                    new Account.Builder()
                            .name(APPC_GENERIC_ACCOUNT_NAME)
                            .build());
        }

        if (appceleratorUser == null) {
            User user = new User.Builder()
                    .account(appceleratorAccount)
                    .username(APPC_GENERIC_USERNAME)
                    .alias(APPC_GENERIC_ALIAS)
                    .fullname(APPC_GENERIC_FULLNAME)
                    .roles(userService.getUserRoles())
                    .userStatus(User.UserStatus.ACTIVATED)
                    .build();

            userService.createUser(user);
        }
    }

}
