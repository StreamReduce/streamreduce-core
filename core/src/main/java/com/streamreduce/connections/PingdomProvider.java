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
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.util.PingdomClient;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Provider class for Pingdom (http://www.pingdom.com). Note that Pingdom
 * requires both username/password as well as an API key to access the
 * REST API.
 *
 * <p>Documentation on Pingdom's REST API can be found here: http://www.pingdom.com/services/api-documentation-rest/</p>
 *
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/25/12 11:14</p>
 */
@Component
public class PingdomProvider extends AbstractConnectionProvider
        implements MonitoringProvider, ExternalIntegrationConnectionProvider {

    static final private Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.USERNAME_PASSWORD_WITH_API_KEY);



    @Override
    public String getType() {
        return ConnectionTypeConstants.MONITORING_TYPE;
    }

    @Override
    public String getId() {
        return ProviderIdConstants.PINGDOM_PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return "Pingdom";
    }

    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

    @Override
    public PingdomClient getClient(Connection connection) {
        return new PingdomClient(connection);
    }
}
