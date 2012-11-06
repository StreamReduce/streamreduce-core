package com.streamreduce.rest.resource.api;

import com.google.common.collect.ImmutableMap;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.ProjectHostingIssue;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.exception.ConnectionExistsException;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.rest.dto.response.ConnectionInventoryResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionProviderResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionProvidersResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;
import com.streamreduce.rest.dto.response.InventoryItemResponseDTO;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.util.AbstractProjectHostingClient;
import com.streamreduce.util.ConnectionUtils;
import com.streamreduce.util.JiraClient;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Connection is an abstraction that provides a top level description of how Nodeable will connect to external
 * resources on behalf of users.  A Connection includes: a globally unique identifier, a provider and type identifying the provider of an external
 * integration and the interaction model with that provider, an authentication type that determines how Nodeable will
 * authenticate to a connection on a user's behalf, an account unique alias, an optional description, hashtags associated
 * to the connection, and the visibility scope of all messages created from that account.
 */
@Component
@Path("api/connections")
public class ConnectionResource extends AbstractOwnableResource {

    @Autowired
    ConnectionProviderFactory connectionProviderFactory;
    @Autowired
    ConnectionService connectionService;

    /**
     * Returns an array of connection provider types.
     * A connector provider type is a top level type for distinguishing what the type of a connection (e.g. cloud, projecthosting).
     * Every connection has a type.
     *
     * @return the array of connection provider types
     * @resource.representation.200.doc returned when all connection provider types are successfully returned
     */
    @GET
    @Path("types")
    public Response getProviderTypes() {
        return Response.ok(ConnectionUtils.PROVIDER_MAP.keySet()).build();
    }

    /**
     * Returns a list of all connection providers.  Each individual connection  provider resource describes
     * an identifier for a provider, canonical names for each provider, the type (e.g. cloud, projecthosting) that the provider belongs to, and
     * optionally all possible methods of authentication for a given provider.  Every connection has a provider.
     *
     * @param showAuthTypes boolean value specifying whether supported authTypes for each provider are returned for each provider document.
     * @return the object representing the collection of connection providers or an error if something goes wrong
     * @resource.representation.200.doc returned when all providerTypes are successfully returned
     */
    @GET
    @Path("providers")
    public Response getProviderList(@QueryParam("showAuthTypes") boolean showAuthTypes) {
        ConnectionProvidersResponseDTO responseDTO = new ConnectionProvidersResponseDTO();
        List<ConnectionProviderResponseDTO> providers = new ArrayList<ConnectionProviderResponseDTO>();

        for (ConnectionProvider provider : ConnectionUtils.getAllProviders()) {
            providers.add(ConnectionProviderResponseDTO.toDTO(provider, showAuthTypes));
        }

        responseDTO.setProviders(providers);

        return Response.ok(responseDTO).build();
    }

    /**
     * Returns a list of connection providers for only the specified type.
     * Supported connection provider types that can be specified are: cloud, projecthosting, feed, gateway
     *
     * @param providerType  the provider type to filter all connection providers by
     * @param showAuthTypes boolean value specifying whether authType details should be included or not
     * @return the list of connection providers
     * @resource.representation.200.doc returned when all connection provider types are successfully returned
     * @response.representation.400.doc Returned when an invalid or non-existent providerType is supplied.
     */
    @GET
    @Path("providers/{providerType}")
    public Response getProviderList(@PathParam("providerType") String providerType,
                                    @QueryParam("showAuthTypes") boolean showAuthTypes) {
        // We want our caller to know they requested an invalid connection type
        if (!ConnectionUtils.PROVIDER_MAP.containsKey(providerType)) {
            return error("'" + providerType + "' is an invalid connection provider type.",
                    Response.status(Response.Status.BAD_REQUEST));
        }

        ConnectionProvidersResponseDTO responseDTO = new ConnectionProvidersResponseDTO();
        List<ConnectionProviderResponseDTO> providers = new ArrayList<ConnectionProviderResponseDTO>();

        for (ConnectionProvider provider : ConnectionUtils.getSupportedProviders(providerType)) {
            providers.add(ConnectionProviderResponseDTO.toDTO(provider, showAuthTypes));
        }

        responseDTO.setProviders(providers);

        return Response.ok(responseDTO).build();
    }

    /**
     * Returns a list of all existing and active connections the current user has access to.
     *
     * @return an array of connections or an error if something goes wrong
     * @response.representation.200.doc Returned when a valid users requests this resource
     */
    @GET
    public Response listAllConnections() {
        List<ConnectionResponseDTO> connectionsDTO = new ArrayList<ConnectionResponseDTO>();
        User user = securityService.getCurrentUser();

        List<Connection> connections = connectionService.getConnections(null, user);

        for (Connection connection : connections) {
            // do not include blacklisted connections, these have been "deleted".
            if (!ConnectionUtils.isBlacklisted(user.getAccount(), connection.getId())) {
                connectionsDTO.add(toFullDTO(connection));
            }
        }

        return Response.ok(connectionsDTO).build();
    }

    /**
     * Returns a list of connection providers of a particular type.
     * Supported connection provider types: cloud, projecthosting, feed, and gateway.
     *
     * @param providerType the connection provider type
     * @return the collection of connections for the given type or an error if something goes wrong
     * @response.representation.200.doc Returned when a list of connections for a type is successfully rendered
     */
    @GET
    @Path("types/{providerType}")
    public Response listConnectionsOfType(@PathParam("providerType") String providerType) {
        List<ConnectionResponseDTO> connectionsDTO = new ArrayList<ConnectionResponseDTO>();
        User user = securityService.getCurrentUser();
        List<Connection> connections = connectionService.getConnections(providerType, user);

        for (Connection connection : connections) {
            connectionsDTO.add(toFullDTO(connection));
        }

        return Response.ok(connectionsDTO).build();
    }

    /**
     * Returns the connection with the given id.
     *
     * @param id the connection id
     * @return the connection with the given id
     * @response.representation.200.doc Returned when a connection for the given id is successfully retrieved
     * @response.representation.400.doc Returned when the id is null when passed in
     * @response.representation.404.doc Returned when you request a connection whose id does not exist for the given user
     */
    @GET
    @Path("{id}")
    public Response getConnectionWithId(@PathParam("id") ObjectId id) {
        if (id == null) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }
        try {
            return Response
                    .ok(toFullDTO(connectionService.getConnection(id)))
                    .build();
        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
    }

    /**
     * Creates a new connection.  The passed in connection will be checked to make sure it can connect to the specified
     * provider with the optionally supplied url credentials.  A new connection must also have an account unique alias.
     *
     * @param json the json payload describing the connection
     * @return the created connection
     * @response.representation.200.doc Returned when a connection is successfully created
     * @response.representation.400.doc Returned when a duplicate connection (or alias) exists, if credentials are invalid, or if the json payload is missing necessary attributes.
     * @response.representation.500.doc Returned a host can't be found from a client supplied url or another unexpected network layer problem occurred connection to host.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createConnection(JSONObject json) {
        User currentUser = securityService.getCurrentUser();
        try {
            Connection connection = new Connection.Builder()
                    .mergeWithJSON(json)
                    .user(currentUser) //sets user and account
                    .provider(connectionProviderFactory.connectionProviderFromId(getJSON(json, "providerId"))) //sets providerId and type
                    .outboundConfigurations(extractOutboundConfigurationsFromJSON(json)) //set outboundConfigurations if available in the jsonObject
                    .build();

            connectionService.createConnection(connection);
            return Response.ok(toFullDTO(connection)).build();

        } catch (ConnectionExistsException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        } catch (ConnectionNotFoundException e) {
            return error("A Connection specified in an outboundConfiguration does not exist: " + e.getMessage(),
                    Response.status(Response.Status.BAD_REQUEST));
        } catch (InvalidCredentialsException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        } catch (UnknownHostException e) {
            return error(e.getMessage() + " is an unknown host. ", Response.status(Response.Status.INTERNAL_SERVER_ERROR));
        } catch (IOException e) {
            return error(e.getMessage(), Response.status(Response.Status.INTERNAL_SERVER_ERROR));
        } catch (IllegalArgumentException e) {
            // This happens with null URLs in project hosting validation (pre-DAO persist)
            ConstraintViolationExceptionResponseDTO dto = new ConstraintViolationExceptionResponseDTO();
            dto.setViolations(ImmutableMap.of("url", e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(dto).build();
        } catch (RuntimeException e) {
            //Catch all for runtime exceptions
            e.printStackTrace();
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }
    }

    /**
     * Creates a new external resource on a given connection if the connection provider supports two-way integration.
     * <p/>
     * Presently only creation of new issues on connections with a provider type of "jira" is supported.
     *
     * @param id   the id of the connection to create the external resource
     * @param json the json payload describing the resource to be created
     * @return the newly assigned resource id.
     * @response.representation.200.doc Returned when an external resource on the connection is successfully created
     * @resource.representation.400.doc Returned when the provider does not support creation of the resource
     * @resource.representation.404.doc Returned when the id is not found
     * @resource.representation.500.doc Returned when the resource creation request to external provider failed
     */
    @POST
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createExternalResource(@PathParam("id") ObjectId id, JSONObject json) {
        if (isNullOrEmpty(id)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        AbstractProjectHostingClient projectHostingClient = null;

        try {
            Connection connection = connectionService.getConnection(id);

            if (!isOwnerOrAdmin(connection.getUser(), connection.getAccount())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

            if (connection.getProviderId().equals(ProviderIdConstants.JIRA_PROVIDER_ID)) {
                projectHostingClient = new JiraClient(connection);

                ProjectHostingIssue issue = new ProjectHostingIssue();

                issue.setType(getJSON(json, "type"));
                issue.setProject("project");
                issue.setSummary("summary");
                issue.setDescription("description");

                try {
                    return Response.ok(((JiraClient) projectHostingClient).createIssue(issue)).build();
                } catch (SOAPException e) {
                    return error("Error creating Jira issue using SOAP API for connection [" + connection.getId() +
                            "]: " + e.getMessage(),
                            Response.status(Response.Status.INTERNAL_SERVER_ERROR));
                }
            } else {
                return error("The connection type for the id specified does not support creating external issues.",
                        Response.status(Response.Status.BAD_REQUEST));
            }

        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        } finally {
            if (projectHostingClient != null) {
                projectHostingClient.cleanUp();
            }
        }
    }

    /**
     * Updates the connection identified by the id in the path.
     *
     * @param id   the id of the connection to update
     * @param json the json payload describing the connection's changes
     * @return the updated connection
     * @response.representation.200.doc Returned when a connection is successfully updated
     * @response.representation.400.doc If a client attempts to update a connection not accessible to the client, or if new credentials for the connection do not work.
     * @response.representation.404.doc Returned when a connection id is not found
     * @response.representation.409.doc If a connection being updated will have no attributes that make it a duplicate of another connection
     * the connection fails validation
     * @resource.representation.500.doc Returned when the resource creation request to external provider failed
     */
    @PUT
    @Path("{id}")
    public Response updateConnection(@PathParam("id") ObjectId id, JSONObject json) {
        if (isNullOrEmpty(id)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            Connection connection = connectionService.getConnection(id);

            if (!isOwnerOrAdmin(connection.getUser(), connection.getAccount())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

            connection.mergeWithJSON(json);
            mergeOutboundConfigurations(connection, json);

            return Response.ok(toFullDTO(connectionService.updateConnection(connection))).build();
        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        } catch (ConnectionExistsException e) {
            return error(e.getMessage(), Response.status(Response.Status.CONFLICT));
        } catch (InvalidCredentialsException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        } catch (IOException e) {
            return error(e.getMessage(), Response.status(Response.Status.INTERNAL_SERVER_ERROR));
        } catch (IllegalArgumentException e) {
            // This happens with null URLs in project hosting validation (pre-DAO persist)
            ConstraintViolationExceptionResponseDTO dto = new ConstraintViolationExceptionResponseDTO();
            dto.setViolations(ImmutableMap.of("url", e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(dto).build();
        } catch (RuntimeException e) {
            //Catch all for runtime exceptions
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }
    }

    /**
     * Deletes the connection identified by the id in the path.
     *
     * @param id the id of the connection to delete
     * @return the response indicating the result of the deletion
     * @response.representation.200.doc Returned when a connection is successfully deleted
     * @response.representation.400.doc Returned when the specified connection does not exist or is inaccessible to the client.
     * @response.representation.404.doc Returned when the id is not found
     */
    @DELETE
    @Path("{id}")
    public Response deleteConnection(@PathParam("id") ObjectId id) {

        if (id == null) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            Connection connection = connectionService.getConnection(id);
            Account account = securityService.getCurrentUser().getAccount();

            // SOBA-1885 resource is PUBLIC and you are the admin.
            // AND you are not in the Nodeable account (because we really can delete those)
            if ((connection.getVisibility().equals(SobaObject.Visibility.PUBLIC)
                    // && securityService.hasRole(Roles.ADMIN_ROLE)  // SOBA-1937, allow any user to remove public
            )
                    && !applicationManager.getUserService().getSuperUser().getAccount().getId().equals(account.getId())) {
                // add to blacklist
                account.appendToPublicConnectionBlacklist(connection.getId());
                applicationManager.getUserService().updateAccount(account);

                // remove messages from inbox if we want
//                if(connection.getHashtags().contains("#sample")) {
//                    applicationManager.getMessageService().removeSampleMessages(account, connection.getId());
//                }

                return Response.ok().build();
            }


            // ACCOUNT or PRIVATE scope and you are not the owner, or admin
            if (!isOwnerOrAdmin(connection.getUser(), connection.getAccount())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

            // delete the connection, associated inventory will be removed in next refresh job
            connectionService.deleteConnection(connection);

            return Response.ok().build();
        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }
    }

    /**
     * Returns the inventory items for the connection identified by the id in the path.  Inventory items represent
     * assets created and managed by the external provider.  Supported connection provider types: cloud, projecthosting.
     *
     * @param id    the id of the connection whose inventory we're interested in
     * @param count set to true if only a count of the number of inventory items is needed
     * @return the connection's inventory
     * @response.representation.200.doc Returned when the inventory of a connection is successfully rendered
     * @response.representation.400.doc Returned when the client does not specify a connection id or specifies a connection id the client does not have access to
     * @response.representation.404.doc Returned when the connection id cannot be found
     */
    @GET
    @Path("{id}/inventory")
    public Response getInventory(@PathParam("id") ObjectId id, @DefaultValue("false") @QueryParam("count") boolean count) {
        if (id == null) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        User currentUser = securityService.getCurrentUser();

        Connection connection;

        try {
            connection = connectionService.getConnection(id);

            // if it's public it's ok
            if (!isInAccount(connection.getAccount()) && !connection.getVisibility().equals(SobaObject.Visibility.PUBLIC)) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        InventoryService inventoryService = applicationManager.getInventoryService();
        List<InventoryItem> inventoryItems = inventoryService.getInventoryItems(connection, currentUser);

        if (inventoryItems == null || inventoryItems.size() == 0) {
            return Response.noContent().build();
        } else if (count) {
            return Response.ok(inventoryItems.size()).build();
        } else {
            ConnectionInventoryResponseDTO dto = new ConnectionInventoryResponseDTO();
            List<InventoryItemResponseDTO> inventoryItemDTOs = new ArrayList<InventoryItemResponseDTO>();

            for (InventoryItem inventoryItem : inventoryItems) {
                inventoryItemDTOs.add(toFullDTO(inventoryItem));
            }

            dto.setInventoryItems(inventoryItemDTOs);

            return Response.ok(dto).build();
        }
    }

    /**
     * Adds the hashtag to a connection and also to all of its inventory items.
     *
     * @response.representation.200.doc Returned when a tag is successfully applied to a connection and all of its inventory items
     * @response.representation.400.doc Returned if no hashtag is specified, or if the client does not have access to the given connection
     * @response.representation.404.doc If the connection with the supplied id does not exist
     */
    @POST
    @Path("{id}/hashtag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public Response addTag(@PathParam("id") ObjectId id, JSONObject json) {

        String hashtag = getJSON(json, HASHTAG);

        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            Connection connection = connectionService.getConnection(id);
            User user = securityService.getCurrentUser();

            if (!isInAccount(connection.getAccount())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }
            connectionService.addHashtag(connection, user, hashtag);

            //  do this in the resources because only users are doing this right now.
            //  probably need to move to the service layer at some point
            List items = applicationManager.getInventoryService().getInventoryItems(connection, user);
            if (items != null) {
                for (Object abstractInventoryItem : items) {
                    applicationManager.getInventoryService().addHashtag((InventoryItem) abstractInventoryItem, user,
                            hashtag);
                }
            }

        } catch (ConnectionNotFoundException e) {
            return error("No connection with the provided id (" + id.toString() + ") could be found.",
                    Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .ok()
                .build();
    }

    /**
     * Retrieves a list of all hashtags for a given connection.
     *
     * @response.representation.200.doc Returned when a list of hashtags for a given connection is successfully rendered
     * @response.representation.400.doc Returned if a connection id isn't specified or the client does not have access to the connection.
     * @response.representation.404.doc If the connection with the supplied id does not exist
     */
    @GET
    @Path("{id}/hashtag")
    @Override
    public Response getTags(@PathParam("id") ObjectId id) {

        if (id == null) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }
        Set<String> tags;
        Connection connection;
        try {
            connection = connectionService.getConnection(id);

            if (!isInAccount(connection.getAccount())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

        } catch (ConnectionNotFoundException e) {
            return error("No connection with the provided id (" + id.toString() + ") could be found.",
                    Response.status(Response.Status.NOT_FOUND));
        }

        tags = connection.getHashtags();

        return Response
                .ok(tags)
                .build();

    }

    /**
     * Deletes a hashtag from a connection and all of its inventory items.
     *
     * @response.representation.200.doc Returned when a hashtag is successfully removed from a connection and all of its inventory
     * @response.representation.400.doc Returned if a connection id isn't specified or the client does not have access to the connection.
     * @response.representation.404.doc If the connection with the supplied id does not exist
     */
    @DELETE
    @Path("{id}/hashtag/{tagname}")
    @Override
    public Response removeTag(@PathParam("id") ObjectId id, @PathParam("tagname") String hashtag) {

        if (id == null) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            Connection connection = connectionService.getConnection(id);

            if (!isInAccount(connection.getAccount())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

            User user = securityService.getCurrentUser();
            connectionService.removeHashtag(connection, user, hashtag);

            //  do this in the resources because only users are doing this right now.
            //  probably need to move to the service layer at some point
            List items = applicationManager.getInventoryService().getInventoryItems(connection, user);
            if (items != null) {
                for (Object abstractInventoryItem : items) {
                    applicationManager.getInventoryService().removeHashtag((InventoryItem) abstractInventoryItem, user, hashtag);
                }
            }

        } catch (ConnectionNotFoundException e) {
            return error("No connection with the provided id (" + id.toString() + ") could be found.",
                    Response.status(Response.Status.NOT_FOUND));
        }

        return Response.ok().build();
    }


    /**
     * Immediately refreshes the inventory for the given connection
     *
     * @response.representation.200.doc Returned if the request to refresh inventory immediately was granted.
     * @response.representation.400.doc Returned if a connection id isn't specified or the client does not have access to the connection.
     * @response.representation.404.doc If the connection with the supplied id does not exist
     * @response.representation.500.doc If the refresh of the connection's inventory could not be scheduled
     */
    @POST
    @Path("{id}/inventory/refresh")
    public Response refreshInventory(@PathParam("id") ObjectId id) {

        if (id == null) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD, Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            Connection connection;
            try {
                connection = connectionService.getConnection(id);

            } catch (ConnectionNotFoundException e) {
                return error("No connection with the provided id (" + id.toString() + ") could be found.",
                        Response.status(Response.Status.NOT_FOUND));
            }

            // only the owner can do this...
            if (!isOwner(connection.getUser())) {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

            if (!connection.getType().equals(ConnectionTypeConstants.FEED_TYPE) || !connection.getType().equals(ConnectionTypeConstants.GATEWAY_TYPE)) {
                connectionService.fireOneTimeHighPriorityJobForConnection(connection);
            }
        } catch (ConnectionNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        } catch (InvalidCredentialsException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        } catch (IOException e) {
            return error(e.getMessage(), Response.status(Response.Status.INTERNAL_SERVER_ERROR));
        }

        return Response.ok().build();
    }

    /**
     * Expects a jsonObject that contains a child element of outboundConfigurations and converts it to an
     * OutboundConfiguration[] to be used as varargs to
     * {@link com.streamreduce.core.model.Connection.Builder#outboundConfigurations(com.streamreduce.core.model.OutboundConfiguration...)}
     *
     * @param jsonObject a JSONObject representing an OutboundConfiguration received from the REST API.
     * @return OutboundConfiguration[] containing all transformed jsonobjects into OutboundConfigurations
     */
    private OutboundConfiguration[] extractOutboundConfigurationsFromJSON(JSONObject jsonObject) throws ConnectionNotFoundException {
        //There's really no better place to put this unless the model objects start including services so they can
        //lazily load fields.

        if (jsonObject == null || !jsonObject.containsKey("outboundConfigurations")) {
            return new OutboundConfiguration[0];
        }

        List<OutboundConfiguration> outboundConfigurationList = new ArrayList<OutboundConfiguration>();
        for (Object o : jsonObject.getJSONArray("outboundConfigurations")) {
            OutboundConfiguration outboundConfiguration = extractOutboundConfigurationFromJSON((JSONObject) o);
            outboundConfigurationList.add(outboundConfiguration);
        }
        return outboundConfigurationList.toArray(new OutboundConfiguration[outboundConfigurationList.size()]);
    }

    private OutboundConfiguration extractOutboundConfigurationFromJSON(JSONObject outboundConfigurationAsJSONObject) {
        OutboundConfiguration.Builder outboundConfigurationBuilder = new OutboundConfiguration.Builder()
                .protocol(outboundConfigurationAsJSONObject.getString("protocol"));

        List<OutboundDataType> outboundDataTypes = new ArrayList<OutboundDataType>();
        for (Object dataTypeObj : outboundConfigurationAsJSONObject.getJSONArray("dataTypes")) {
            String dataType = ((String) dataTypeObj).toUpperCase();
            outboundDataTypes.add(OutboundDataType.valueOf(dataType));
        }
        outboundConfigurationBuilder.dataTypes(outboundDataTypes.toArray(new OutboundDataType[outboundDataTypes.size()]));

        if (outboundConfigurationAsJSONObject.containsKey("credentials")) {
            JSONObject credentialsJSONObject = outboundConfigurationAsJSONObject.getJSONObject("credentials");
            String username = credentialsJSONObject.containsKey("username") ? credentialsJSONObject.getString("username") : null;
            String password = credentialsJSONObject.containsKey("password") ? credentialsJSONObject.getString("password") : null;
            ConnectionCredentials credentials = new ConnectionCredentials(username, password);
            outboundConfigurationBuilder.credentials(credentials);
        }

        //Non-required fields
        if (outboundConfigurationAsJSONObject.containsKey("destination")) {
            outboundConfigurationBuilder.destination(outboundConfigurationAsJSONObject.getString("destination"));
        }
        if (outboundConfigurationAsJSONObject.containsKey("namespace")) {
            outboundConfigurationBuilder.namespace(outboundConfigurationAsJSONObject.getString("namespace"));
        }
        return outboundConfigurationBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private void mergeOutboundConfigurations(Connection connection, JSONObject json) throws ConnectionNotFoundException {
        if (!json.containsKey("outboundConfigurations")) {
            return;
        }

        // build a map of outbound configs keyed on the protocol + destination
        Map<String, OutboundConfiguration> currentConfigs = new HashMap<String, OutboundConfiguration>();
        if (!CollectionUtils.isEmpty(connection.getOutboundConfigurations())) {
            for (OutboundConfiguration outboundConfiguration : connection.getOutboundConfigurations()) {
                String key = outboundConfiguration.getProtocol();
                currentConfigs.put(key, outboundConfiguration);
            }
        }

        JSONArray outboundConfigurations = json.getJSONArray("outboundConfigurations");
        for (Iterator<JSONObject> iter = outboundConfigurations.iterator(); iter.hasNext(); ) {
            JSONObject outboundConfiguration = iter.next();
            if (outboundConfiguration.containsKey("protocol")) {
                // do we know about this config already?
                String key = outboundConfiguration.getString("protocol");
                OutboundConfiguration configuration = null;
                if (!currentConfigs.containsKey(key)) {
                    configuration = extractOutboundConfigurationFromJSON(outboundConfiguration);
                    connection.addOutboundConfiguration(configuration);
                } else {
                    configuration = currentConfigs.get(key);
                    configuration.mergeWithJSON(outboundConfiguration);
                }
            }
        }
    }
}
