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
