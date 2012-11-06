package com.streamreduce.connections;

/**
 * Abstract class to be extended by all {@link com.streamreduce.connections.SocialProvider} objects of type
 * {@link com.streamreduce.ConnectionTypeConstants.SOCIAL_TYPE}.
 */
public abstract class AbstractSocialProvider extends AbstractConnectionProvider implements SocialProvider{

    AbstractSocialProvider() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return SocialProvider.TYPE;
    }

}
