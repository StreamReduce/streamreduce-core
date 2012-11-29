package com.streamreduce.rest.resource.admin;

import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.service.ConnectionService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * These are just helper methods to get the API key that an appId or guid should be using to
 * make IMG requests. Ideally, these go away in the future.
 */
@Component
@Path("admin/connection")
public class AdminConnectionResource extends AbstractAdminResource {

    @Autowired
    private ConnectionService connectionService;

    /**
     * @param appId - find the API Key for the Connection (App) so we can
     *              make an IMG request
     * @return - the API Key stored on the Connection Object
     */
    @GET
    @Path("/appId/{appId}")
    public Response getConnectionAPIKey(@PathParam("appId") String appId) {

        String apiKey;
        try {
            Connection connection = connectionService.getConnectionByAPPID(appId);
            apiKey = connection.getCredentials().getApiKey();
        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
        return Response.ok(apiKey).build();
    }

    /**
     * @param guid - find the API Key for the Inventory Item (Device) so we can
     *             make an IMG request
     * @return - the API Key stored on the Connection Object
     */
    @GET
    @Path("/guid/{guid}")
    public Response getConnectionInventoryItemAPIKey(@PathParam("guid") String guid) {

        String apiKey;
        try {
            Connection connection = connectionService.getConnectionByGUID(guid);
            apiKey = connection.getCredentials().getApiKey();
        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
        return Response.ok(apiKey).build();
    }


}
