package com.streamreduce.util;

import java.io.IOException;

import com.streamreduce.connections.AuthType;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class all external integration clients will extend.
 */
public abstract class ExternalIntegrationClient {

    public final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private ConnectionCredentials connectionCredentials;
    private ObjectId connectionId;
    private AuthType authType;

    /**
     * Constructor.
     *
     * @param connection the {@link Connection} to get the client information from.
     */
    public ExternalIntegrationClient(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null.");
        }

        connectionCredentials = connection.getCredentials();
        connectionId = connection.getId();
        authType = connection.getAuthType();
    }

    /**
     * Constructor.
     *
     * @param outboundConfiguration the {@link Connection} to get the client information from.
     */
    protected ExternalIntegrationClient(OutboundConfiguration outboundConfiguration) {
        if (outboundConfiguration == null) {
            throw new IllegalArgumentException("outboundConfiguration cannot be null.");
        }

        connectionCredentials = outboundConfiguration.getCredentials();
        authType = AuthType.USERNAME_PASSWORD;
    }

    /**
     * Returns the {@link ObjectId} representing the identifier of the connection.
     *
     * @return the connection id
     */
    public ObjectId getConnectionId() {
        return connectionId;
    }

    /**
     * Returns the connection credentials for this client.
     *
     * @return the connection credentials
     */
    public ConnectionCredentials getConnectionCredentials() {
        return connectionCredentials;
    }

    /**
     * Returns the Connection.AuthType to be used for this client.
     * @return
     */
    public AuthType getAuthType() {
        return authType;
    }

    /**
     * Perform any cleanup necessary
     */
    public void cleanUp() {
        // Do nothing but be here for those that want to override without requiring it
    }

    /**
     * Simple helper to simplify some logging code.
     *
     * @param logger the logger to log to if necessary
     * @param message the message to log
     */
    protected void debugLog(Logger logger, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug("[" + connectionId + "] " + message);
        }
    }

    /**
     * Validates the client by using the information in the connection to make a request that
     * will ensure the client is valid.
     *
     * @throws com.streamreduce.core.service.exception.InvalidCredentialsException if the connection's credentials are invalid
     * @throws java.io.IOException if anything else goes wrong
     */
    public abstract void validateConnection() throws InvalidCredentialsException, IOException;

}
