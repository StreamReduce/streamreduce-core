package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.util.ExternalIntegrationClient;
import com.streamreduce.util.JiraClient;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The JiraProjectHostingProvider class is an implementation of the
 * {@link ProjectHostingProvider} that provides support
 * for the Jira project hosting services.
 */
@Component
public class JiraProjectHostingProvider extends AbstractProjectHostingConnectionProvider
    implements ExternalIntegrationConnectionProvider {

    static final private Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.USERNAME_PASSWORD);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsernameLabel() {
        return super.getUsernameLabel() + " (Jira Studio)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ProviderIdConstants.JIRA_PROVIDER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "Jira";
    }

    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

    @Override
    public ExternalIntegrationClient getClient(Connection connection) {
        return new JiraClient(connection);
    }
}
