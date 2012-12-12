package com.streamreduce.rest.resource.appcelerator;

import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for consuming all events from Appcelerator.  The idea is that event structures, documented
 * <a href="https://wiki.appcelerator.org/display/cls/Example+Analytics+JSON+payloads">here</a>, will be parsed and
 * mapped to internal StreamReduce objects (Accounts, Users, Connections, etc.) on an as-needed basis.
 */
@Component
@Path("appcelerator")
public class AppceleratorEventsResource {

    private final String INVALID_REQUEST_PREFIX = "Invalid event structure: ";

    /**
     * Consumes raw JSON events from Appcelerator and creates Nodeable events as a result.
     *
     * @param event the JSON representing the event payload
     *
     * @resource.representation.200 Returned when the event is processed successfully
     * @resource.representation.405 Returned whenever the event payload is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleEvent(JSONObject event) {
        // The process for consuming Appcelerator events works like this:
        //
        //   1) Parse out the necessary pieces available in the payload:
        //     * aguid: Application id
        //     * mid  : Machine/Device ID
        //   2) Lookup Nodeable objects based on the Appcelerator ids
        //   3) Create necessary objects to correspond with the Appcelerator ids (if necessary)
        //   4) Create event
        //   5) Return response

        // Quick return if we know the request is guaranteed to be invalid.
        if (event == null || event.size() == 0) {
            return createError("Empty event structure");
        }

        // Based on the wiki referenced in the class' javadocs, 'aguid' is guaranteed to always be in the payload and is
        // used to map to a Connection object.
        if (!event.containsKey("aguid")) {
            return createError("'aguid' is a required event property");
        }

        // Based on the wiki referenced in the class' javadocs, 'mid' is guaranteed to always be in the payload and is
        // used to map to an InventoryItem object.
        if (!event.containsKey("mid")) {
            return createError("'mid' is a required event property");
        }

        String aguid = event.getString("aguid");
        String mid = event.getString("mid");

        // Return a 500 until this thing actually works
        return Response.serverError().build();
    }

    /**
     * Creates a {@link Response} representing an error.
     *
     * @param message the error message
     *
     * @return the error object as JSON
     */
    private Response createError(String message) {
        JSONObject json = new JSONObject();
        JSONObject error = new JSONObject();

        error.put("message", INVALID_REQUEST_PREFIX + message);

        json.put("error", error);

        return Response.status(Response.Status.BAD_REQUEST).entity(json).build();
    }

}
