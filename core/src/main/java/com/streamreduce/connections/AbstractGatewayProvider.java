package com.streamreduce.connections;

public abstract class AbstractGatewayProvider extends AbstractConnectionProvider implements GatewayProvider {

    AbstractGatewayProvider() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return GatewayProvider.TYPE;
    }
}
