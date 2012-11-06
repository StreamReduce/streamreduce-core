package com.streamreduce.core.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.streamreduce.core.dao.ConnectionDAO;
import com.streamreduce.core.model.Connection;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/30/12 16:12</p>
 */
@Component
@ManagedResource(objectName="com.streamreduce.core.service:type=ConnectionManagementService,name=connection-management-srvc")
public class ConnectionManagementServiceImpl implements ConnectionManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManagementServiceImpl.class);
    
    @Resource
    private ConnectionDAO connectionDAO;

    private ObjectMapper objectMapper;

    public ConnectionManagementServiceImpl() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        objectMapper.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    @Override
    @ManagedOperation(description = "Returns all connections")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "type", description = "If not null, returns connections of a specific type."),
            @ManagedOperationParameter(name = "summary", description = "If true, only connection summaries are returned.")})
    public String getAllConnections(String type, boolean summary) {
        if ("String".equals(type)) {
            type = null;
        }
        List<Connection> connections = connectionDAO.allConnectionsOfType(type);
        return toJSON(connections, summary);
    }

    @Override
    @ManagedOperation(description = "Returns all broken connections")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "type", description = "If not null, returns connections of a specific type."),
            @ManagedOperationParameter(name = "summary", description = "If true, only connection summaries are returned.")})
    public String getBrokenConnections(String type, boolean summary) {
        if ("String".equals(type)) {
            type = null;
        }
        List<Connection> connections = connectionDAO.allBrokenConnectionsOfType(type);
        return toJSON(connections, summary);
    }

    @Override
    @ManagedOperation(description = "Returns all disabled connections")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "type", description = "If not null, returns connections of a specific type."),
            @ManagedOperationParameter(name = "summary", description = "If true, only connection summaries are returned.")})
    public String getDisabledConnections(String type, boolean summary) {
        if ("String".equals(type)) {
            type = null;
        }
        List<Connection> connections = connectionDAO.allDisabledConnectionsOfType(type);
        return toJSON(connections, summary);
    }

    @Override
    @ManagedOperation(description = "Returns a specific connection")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "connectionObjectId", description = "Connection ObjectID.")})
    public String getConnection(String connectionObjectId) {
        return toJSON(connectionDAO.get(new ObjectId(connectionObjectId)), false);
    }

    @Override
    @ManagedOperation(description = "Sets a connection as broken.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "connectionObjectId", description = "Connection ObjectID.")})
    public void setAsBroken(String connectionObjectId) {
        Connection connection = connectionDAO.get(new ObjectId(connectionObjectId));
        connection.setAsBroke("Set as broken by JMX user.");
        connectionDAO.save(connection);
    }

    @Override
    @ManagedOperation(description = "Sets a connection as unbroken.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "connectionObjectId", description = "Connection ObjectID.")})
    public void setAsUnbroken(String connectionObjectId) {
        Connection connection = connectionDAO.get(new ObjectId(connectionObjectId));
        connection.setAsUnbroke();
        connectionDAO.save(connection);
    }

    @Override
    @ManagedOperation(description = "Disables a connection.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "connectionObjectId", description = "Connection ObjectID.")})
    public void disableConnection(String connectionObjectId) {
        Connection connection = connectionDAO.get(new ObjectId(connectionObjectId));
        connection.setDisabled(true);
        connectionDAO.save(connection);
    }

    @Override
    @ManagedOperation(description = "Enables a connection.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "connectionObjectId", description = "Connection ObjectID.")})
    public void enableConnection(String connectionObjectId) {
        Connection connection = connectionDAO.get(new ObjectId(connectionObjectId));
        connection.setDisabled(false);
        connectionDAO.save(connection);
    }

    private String toJSON(List<Connection> connections, boolean summary) {
        if (CollectionUtils.isEmpty(connections)) {
            return "{'error' : 'No connections found.'}";
        }

        try {
            if (summary) {
                List<Map<String,String>> summaries = Lists.newArrayList();
                for (Connection connection : connections) {
                    summaries.add(toMap(connection));
                }
                return objectMapper.writeValueAsString(summaries);
            }
            else {
                return objectMapper.writeValueAsString(connections);
            }
        }
        catch (IOException e) {
            LOGGER.error("Encountered exception", e);
            return String.format("{'error' : 'Encountered exception serializing connections: %s'}", e.getMessage());
        }
    }

    private String toJSON(Connection connection, boolean summary) {
        if (connection == null) {
            return "{'error' : 'Connection is null.'}";
        }

        try {
            if (summary) {
                return objectMapper.writeValueAsString(toMap(connection));
            }
            else {
                return objectMapper.writeValueAsString(connection);
            }
        }
        catch (IOException e) {
            LOGGER.error("Encountered exception", e);
            return String.format("{'error' : 'Encountered exception serializing connections: %s'}", e.getMessage());
        }
    }

    private Map<String, String> toMap(Connection connection) {
        Map<String,String> summaryConnection = Maps.newHashMap();
        try {
            summaryConnection.put("_id", connection.getId().toString());
            summaryConnection.put("providerId", connection.getProviderId());
            summaryConnection.put("type", connection.getType());
            summaryConnection.put("alias", connection.getAlias());
            summaryConnection.put("created", connection.getCreated().toString());
            summaryConnection.put("lastActivityPollDate", connection.getLastActivityPollDate().toString());
            summaryConnection.put("pollingFailedCount", Long.toString(connection.getPollingFailedCount()));
            summaryConnection.put("broken", Boolean.toString(connection.isBroken()));
            summaryConnection.put("disabled", Boolean.toString(connection.isDisabled()));
        }
        catch (NullPointerException npe) {
            summaryConnection.put("ERROR", "Encountered NPE while creating summary connection object.");
        }
        return summaryConnection;
    }
}
