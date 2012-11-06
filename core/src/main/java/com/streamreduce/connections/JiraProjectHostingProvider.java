/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
