package com.streamreduce.rest.resource.api;

import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.streamreduce.Constants;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.core.CommandNotAllowedException;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.core.service.exception.InventoryItemNotFoundException;
import com.streamreduce.rest.resource.ErrorMessages;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

@Component
@Path("api/inventory")
public class InventoryResource extends AbstractOwnableResource {

    /**
     * Retrieve an inventory item given its id.
     *
     * @param itemId the inventory item's id
     * @return the inventory item
     *
     * @response.representation.200.doc Returned when the inventory item requested is successfully rendered
     * @response.representation.400.doc Returned when the client attempts to retrieve an inventory item that the resource doesn't support
     * @response.representation.404.doc Returned when the client attempts to retrieve an inventory item that does not exist
     */
    @GET
    @Path("{itemId}")
    public Response getInventoryItem(@PathParam("itemId") ObjectId itemId) {
        InventoryItem inventoryItem;

        try {
            inventoryItem = applicationManager.getInventoryService().getInventoryItem(itemId);
        } catch (InventoryItemNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response.ok(toFullDTO(inventoryItem)).build();
    }

    /**
     * Updates an inventory item based on the JSON payload submitted.
     *
     * @param itemId the inventory item to update
     * @param json   the updated object information
     * @return the updated inventory item
     *
     * @response.representation.200.doc Returned when the inventory item is successfully updated
     * @response.representation.400.doc Returned when the client attempts to update an inventory item it does not access to update
     * @response.representation.404.doc Returned when the client attempts to update an inventory item that does not exist
     */
    @PUT
    @Path("{itemId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateInventoryItem(@PathParam("itemId") ObjectId itemId, JSONObject json) {
        InventoryItem inventoryItem;

        try {
            inventoryItem = applicationManager.getInventoryService().getInventoryItem(itemId);
        } catch (InventoryItemNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        // only the owner can do this...
        if (!isOwner(inventoryItem.getConnection().getUser())) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
        }

        inventoryItem.mergeWithJSON(json);

        try {
            applicationManager.getInventoryService().updateInventoryItem(inventoryItem, json);
        } catch (ConnectionNotFoundException e) {
            return error("Unable to find the connection for the inventory item requested.",
                         Response.status(Response.Status.BAD_REQUEST));
        } catch (InvalidCredentialsException e) {
            return error("Connection credentials for the inventory item's connection are invalid.",
                         Response.status(Response.Status.BAD_REQUEST));
        } catch (IOException e) {
            return error("Unexpected IOException while updating the inventory item.",
                         Response.status(Response.Status.BAD_REQUEST));
        }

        return Response.ok(toFullDTO(inventoryItem)).build();
    }

    /**
     * Adds a hashtag specifically to a given inventory item.
     *
     * @response.representation.200.doc Returned when the hashtag is succesfully added to the inventory item
     * @response.representation.400.doc Returned when the client attempts to add a hashtag to an inventory item it does not have access to
     * @response.representation.404.doc Returned when the client attempts to add a hashtag to an inventory item that does not exist
     */
    @POST
    @Path("{itemId}/hashtag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public Response addTag(@PathParam("itemId") ObjectId itemId, JSONObject json) {

        String hashtag = getJSON(json, HASHTAG);

        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }

        InventoryItem inventoryItem;

        try {
            inventoryItem = applicationManager.getInventoryService().getInventoryItem(itemId);
        } catch (InventoryItemNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        // limit to same account
        if (!isInAccount(inventoryItem.getConnection().getAccount())) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
        }

        applicationManager.getInventoryService().addHashtag(inventoryItem,
                                                            applicationManager.getSecurityService().getCurrentUser(),
                                                            hashtag);

        return Response
                .ok()
                .build();
    }

    /**
     * Retrieves a list of all hashtags currently applied to a given inventory item
     *
     * @response.representation.200.doc Returned when a list of hashtags for the given inventory item is successfully rendered
     * @response.representation.400.doc Returned when the client attempts to retrieve hashtags for an inventory item it does not have access to
     * @response.representation.404.doc Returned when the client attempts to retrieve hashtags for an inventory item that does not exist
     */
    @GET
    @Path("{itemId}/hashtag")
    @Override
    public Response getTags(@PathParam("itemId") ObjectId itemId) {
        InventoryItem inventoryItem;

        try {
            inventoryItem = applicationManager.getInventoryService().getInventoryItem(itemId);
        } catch (InventoryItemNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        // limit to same account
        if (!isInAccount(inventoryItem.getConnection().getAccount())) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
        }

        return Response
                .ok(inventoryItem.getHashtags())
                .build();

    }

    /**
     * Removes a hashtag from a given inventory item.
     *
     * @response.representation.200.doc Returned when the specified hashtag has been successfully removed from the inventory item
     * @response.representation.400.doc Returned when the client attempts to delete a hashtag for an inventory item it does not have access to
     * @response.representation.404.doc Returned when the client attempts to delete a hashtag for an inventory item that does not exist
     */
    @DELETE
    @Path("{itemId}/hashtag/{tagname}")
    @Override
    public Response removeTag(@PathParam("itemId") ObjectId itemId, @PathParam("tagname") String hashtag) {
        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }

        InventoryItem inventoryItem;

        try {
            inventoryItem = applicationManager.getInventoryService().getInventoryItem(itemId);
        } catch (InventoryItemNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        // limit to same account
        if (!isInAccount(inventoryItem.getConnection().getAccount())) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
        }

        applicationManager.getInventoryService().removeHashtag(inventoryItem, applicationManager.getSecurityService().getCurrentUser(), hashtag);
        return Response.ok().build();
    }

    /**
     * Reboots an inventory item.  Presently, this is only supported for cloud inventory items that belong to a cloud connection.
     *
     * @param itemId inventory item id corresponding with a cloud inventory item
     * @return the result of the reboot
     *
     * @response.representation.200.doc Returned when you attempt to reboot a non-cloud inventory item
     * @response.representation.400.doc Returned when the client attempts to reboot a non-cloud inventory item or an inventory item it does not own.
     */
    @PUT
    @Path("{itemId}/reboot")
    public Response reboot(@PathParam("itemId") ObjectId itemId) {
        InventoryService inventoryService = applicationManager.getInventoryService();
        InventoryItem inventoryItem;

        try {
            inventoryItem = inventoryService.getInventoryItem(itemId);
        } catch (InventoryItemNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        if (!(inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE))) {
            return error("You can only reboot cloud inventory items.", Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            // only the owner can do this...
            if (!isOwner(inventoryItem.getConnection().getUser())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

            inventoryService.rebootComputeInstance(inventoryItem);
        } catch (CommandNotAllowedException cnae) {
            return error(cnae.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        } catch (InvalidCredentialsException icce) {
            return error(icce.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }

        return Response.ok().build();
    }

    /**
     * Deletes an inventory item from the external provider.  For cloud providers this may mean something like terminating an instance.
     * For projecthosting providers, this may mean removing a project.
     *
     * @param itemId the id of the inventory item to delete
     * @return the result of the deletion
     *
     * @response.representation.200.doc Returned when the inventory item was successfully deleted
     * @response.representation.400.doc Returned when the client attempts to delete an inventory item it does not own
     * @response.representation.404.doc Returned when the client attempts to delete an inventory item that does not exist
     */
    @DELETE
    @Path("{itemId}")
    public Response deleteInventoryItem(@PathParam("itemId") ObjectId itemId) {
        InventoryService inventoryService = applicationManager.getInventoryService();
        InventoryItem inventoryItem;

        try {
            inventoryItem = inventoryService.getInventoryItem(itemId);
        } catch (InventoryItemNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        // only the owner can do this...
        if (!isOwner(inventoryItem.getConnection().getUser())) {
            return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
        }

        inventoryService.deleteInventoryItem(inventoryItem);

        return Response.ok().build();
    }

}
