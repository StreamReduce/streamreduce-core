package com.streamreduce.connections;

public abstract class AbstractFeedProvider extends AbstractConnectionProvider implements FeedProvider {

    AbstractFeedProvider() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return TYPE;
    }
}
