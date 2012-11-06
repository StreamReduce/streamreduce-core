package com.streamreduce.connections;

import com.streamreduce.ConnectionTypeConstants;

/**
 * Abstract class to be extended by all {@link ConnectionProvider} objects of type "cloud".
 */
public abstract class AbstractCloudConnectionProvider extends AbstractConnectionProvider implements CloudProvider {

    AbstractCloudConnectionProvider() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return ConnectionTypeConstants.CLOUD_TYPE;
    }

}
