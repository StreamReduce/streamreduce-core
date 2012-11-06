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
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/3/12 8:54 AM</p>
 */
@Component
public class WebHDFSProvider extends AbstractGatewayProvider {

    private static final Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.USERNAME_PASSWORD);

    WebHDFSProvider() {}

    @Override
    public String getId() {
        return ProviderIdConstants.WEBHDFS_PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return "WebHDFS";
    }

    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

}
