package com.streamreduce.rest.resource.gateway;

import com.google.common.collect.ImmutableSet;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.OutboundStorageException;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.service.EventService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.MessageService;
import com.streamreduce.core.service.OutboundStorageService;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.core.service.exception.InventoryItemNotFoundException;
import com.streamreduce.rest.resource.AbstractResource;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Path("gateway") //Exposed on /gateway
public class GatewayResource extends AbstractResource {

    @Autowired
    MessageService messageService;
    @Autowired
    OutboundStorageService outboundService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    EventService eventService;


    final Set<String> validMetricTypes = ImmutableSet.of(
            "ABSOLUTE",
            "DELTA"
    );


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createGatewayMessage(JSONObject json) {

        //GateProviderType type = GateProviderType.valueOf(getJSON(json, "providerId"));
        // TODO: switch on type?
        return createCustomConnectionMessage(json);

    }

    /**
     * Processes an IMG message that can be a message and/or metrics for a connection or inventory item.  Your message
     * payload must contain either message and/or metrics attributes.  If your message has the inventoryItemId
     * attribute, the message and/or metrics will be associated with the inventory item.  If the inventory item for the
     * provided inventoryItemId does not exist, one will be created for you.
     *
     * @param json the JSON representing the payload
     * @resource.representation.201 Returned when the create is successful
     * @resource.representation.403 Returned when the account the connection is in does not allow IMG messages
     * @resource.representation.405 Returned whenever the payload is invalid
     *
     * @deprecated @see #createCustomConnectionMessage(JSONObject)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("generic")
    public Response createGenericConnectionMessage(JSONObject json) {
        return createCustomConnectionMessage(json);
    }

    /**
     * Processes an IMG message that can be a message and/or metrics for a connection or inventory item.  Your message
     * payload must contain either message and/or metrics attributes.  If your message has the inventoryItemId
     * attribute, the message and/or metrics will be associated with the inventory item.  If the inventory item for the
     * provided inventoryItemId does not exist, one will be created for you.
     *
     * @param json the JSON representing the payload
     * @resource.representation.201 Returned when the create is successful
     * @resource.representation.403 Returned when the account the connection is in does not allow IMG messages
     * @resource.representation.405 Returned whenever the payload is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("custom")
    public Response createCustomConnectionMessage(JSONObject json) {

        Connection connection = securityService.getCurrentGatewayConnection();

        // Make sure this account has IMG support enabled
        if (connection.getAccount().getConfigValue(Account.ConfigKey.DISABLE_INBOUND_API)) {
            return error("This account is not provisioned for inbound payloads, please contact support@nodeable.com.",
                    Response.status(Response.Status.FORBIDDEN));
        }

        JSONArray entries = new JSONArray();

        if (json.containsKey("data")) {
            Object raw = json.get("data");

            if (!(raw instanceof JSONArray)) {
                return error("'data' must be an object array.", Response.status(Response.Status.BAD_REQUEST));
            }

            JSONArray rawEntries = (JSONArray) raw;

            for (Object rawEntry : rawEntries) {
                if (!(rawEntry instanceof JSONObject)) {
                    return error("Every object in the 'data' array should be an object.",
                            Response.status(Response.Status.BAD_REQUEST));
                }

                entries.add(rawEntry);
            }
        } else {
            // The request is for a single "entry"
            entries.add(json);
        }

        for (Object rawEntry : entries) {
            if (!(rawEntry instanceof JSONObject)) {
                return error("'data' entries must be JSON objects.", Response.status(Response.Status.BAD_REQUEST));
            }

            JSONObject entry = (JSONObject) rawEntry;
            Date dateGenerated = new Date();

            // Make sure that at least a message is being created or metrics are being gathered or both
            if (getJSON(entry, "message") == null && getJSON(entry, "metrics") == null) {
                return error("You must supply at least a 'message' or 'metrics' attribute in the payload.",
                        Response.status(Response.Status.BAD_REQUEST));
            }

            // Make sure hashtags is an array if specified (Type validation occurs later)
            if (getJSON(entry, "hashtags") != null && !(entry.get("hashtags") instanceof JSONArray)) {
                return error("'hashtags' must be a string array.", Response.status(Response.Status.BAD_REQUEST));
            }

            // Make sure metrics is an array if specified (Type validation occurs later)
            if (getJSON(entry, "metrics") != null && !(entry.get("metrics") instanceof JSONArray)) {
                return error("'metrics' must be an object array.", Response.status(Response.Status.BAD_REQUEST));
            }

            // Use the "generatedDate" attribute if applicable
            if (entry.containsKey("dateGenerated")) {
                try {
                    dateGenerated = new Date(Long.valueOf(entry.get("dateGenerated").toString()));
                } catch (NumberFormatException nfe) {
                    return error("'dateGenerated' must be an number.", Response.status(Response.Status.BAD_REQUEST));
                }
            }

            // See if the user specified an inventory item to associate the message and/or metrics to
            String inventoryItemId = getJSON(entry, "inventoryItemId");
            String message = getJSON(entry, "message");
            InventoryItem inventoryItem = null;

            if (inventoryItemId != null) {
                try {
                    inventoryItem = inventoryService.getInventoryItemForExternalId(connection, inventoryItemId);
                } catch (InventoryItemNotFoundException e) {
                    // This is handled below
                }

                if (inventoryItem == null) {
                    try {
                        // Create the inventory item referenced by the inventoryItemId if one does not exist
                        inventoryItem = inventoryService.createInventoryItem(connection, entry);
                    } catch (ConnectionNotFoundException e) {
                        // Should never happen
                        return error("No connection could be found based on the API key used.",
                                Response.status(Response.Status.BAD_REQUEST));
                    } catch (InvalidCredentialsException e) {
                        // Should never happen
                        return error("The credentials for the connection are invalid.",
                                Response.status(Response.Status.BAD_REQUEST));
                    } catch (IOException e) {
                        // Should never happen
                        return error("Unexpected IO exception.", Response.status(Response.Status.BAD_REQUEST));
                    }
                }
            }

            // Add tags to the Connection/InventoryItem for message creation only, do not persist (Validation occurs)
            JSONArray hashtags = entry.has("hashtags") ? entry.getJSONArray("hashtags") : null;
            SobaObject target = inventoryItemId == null ? connection : inventoryItem;
            // TODO: add Custom TAG hack, remove this and do it right
            target.addHashtag("custom");

            if (hashtags != null) {
                // Pull in hashtags from the actual IMG message
                for (Object rawHashtag : hashtags) {
                    if (rawHashtag instanceof String) {
                        target.addHashtag((String) rawHashtag);
                    } else {
                        return error("All hashtags specified in the 'hashtags' attribute must be strings.",
                                Response.status(Response.Status.BAD_REQUEST));
                    }
                }
            }

            // Validate the metrics
            JSONArray metrics = entry.has("metrics") ? entry.getJSONArray("metrics") : null;

            if (metrics != null) {
                for (Object rawMetric : metrics) {
                    if (rawMetric instanceof JSONObject) {
                        JSONObject metric = (JSONObject) rawMetric;
                        String name = getJSON(metric, "name");
                        String type = getJSON(metric, "type");
                        Object rawValue = metric.get("value");
                        String errorMessage = null;

                        if (!StringUtils.hasText(name)) {
                            errorMessage = "'name' is required for each metric in the 'metrics' attribute.";
                        } else if (!StringUtils.hasText("type") || !validMetricTypes.contains(type)) {
                            errorMessage = "'type' is required for each metric in the 'metrics' attribute and must " +
                                    "be either 'ABSOLUTE' or 'DELTA'.";
                        } else if (rawValue instanceof JSONNull || rawValue == null) {
                            errorMessage = "'value' is required for each metric in the 'metrics' attribute and " +
                                    "must be a valid numerical value.";
                        } else {
                            try {
                                Double.valueOf(rawValue.toString());
                            } catch (NumberFormatException e) {
                                errorMessage = "'value' for each metric in the 'metrics' attribute must be a " +
                                        "numerical value.";
                            }
                        }

                        if (errorMessage != null) {
                            return error(errorMessage, Response.status(Response.Status.BAD_REQUEST));
                        }
                    } else {
                        return error("All metrics specified in the 'metrics' attribute must be objects.",
                                Response.status(Response.Status.BAD_REQUEST));
                    }
                }
            }

            // optional config for user
            try {
                outboundService.sendRawMessage(entry, connection);
            } catch (OutboundStorageException e) {
                logger.error("Unable to send RawMessage outbound to Connection.", e);
            }

            // Create the event stream entry
            Map<String, Object> eventContext = new HashMap<String, Object>();
            // Persist the full payload
            eventContext.put("message", message);
            eventContext.put("dateGenerated", dateGenerated);
            eventContext.put("payload", entry);
            Event event = eventService.createEvent(EventId.ACTIVITY, target, eventContext);

            if (message != null) {
                if (inventoryItem != null) {
                    messageService.sendGatewayMessage(event, inventoryItem, dateGenerated.getTime());
                } else {
                    messageService.sendGatewayMessage(event, connection, dateGenerated.getTime());
                }
            }
        }

        return Response
                .status(Response.Status.CREATED)
                .build();
    }
}
