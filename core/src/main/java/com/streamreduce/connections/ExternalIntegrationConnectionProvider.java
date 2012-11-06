package com.streamreduce.connections;

import com.streamreduce.core.model.Connection;
import com.streamreduce.util.ExternalIntegrationClient;

public interface ExternalIntegrationConnectionProvider extends ConnectionProvider {
    /**
     * Returns an ExternalIntegrationClient configured based on the passed in Connection.
     * @return An ExternalIntegrationClient instance.
     * @throws IllegalArgumentException if the connection is null or the connection is configured
     * for a different provider.
     */
    public ExternalIntegrationClient getClient(Connection connection);
}
