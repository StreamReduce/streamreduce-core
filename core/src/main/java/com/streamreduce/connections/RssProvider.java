package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.core.model.Connection;
import com.streamreduce.util.FeedClient;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.streamreduce.feed.types.FeedType.RSS;

@Component
public class RssProvider extends AbstractFeedProvider implements ExternalIntegrationConnectionProvider {

    static final private Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.NONE);


    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return RSS.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "RSS Feed";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

    @Override
    public FeedClient getClient(Connection connection) {
        return new FeedClient(connection);
    }
}
