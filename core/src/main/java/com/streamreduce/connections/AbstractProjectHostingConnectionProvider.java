package com.streamreduce.connections;

/**
 * Abstract class to be extended by all {@link ConnectionProvider} objects of type
 * {@link com.streamreduce.connections.ProjectHostingProvider.TYPE}.
 */
public abstract class AbstractProjectHostingConnectionProvider extends AbstractConnectionProvider
    implements ProjectHostingProvider {

    AbstractProjectHostingConnectionProvider() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return ProjectHostingProvider.TYPE;
    }

}
