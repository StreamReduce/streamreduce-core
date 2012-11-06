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

import java.util.Set;

public interface ConnectionProvider {

    /**
     * Returns a unique string identifying the provider.
     *
     * @return the provider's unique identifier
     */
    public String getId();

    /**
     * Returns the provider type as string.
     * 
     * @return the provider type
     */
    public String getType();

    /**
     * Returns a string to display to the user for the provider.
     *
     * @return the provider's display name
     */
    public String getDisplayName();

    /**
     * Returns a string that the cloud provider uses for the "username"
     * credential for authenticating to their service.
     *
     * @return the label displayed for the "username" credential
     */
    public String getUsernameLabel();

    /**
     * Returns a string that the cloud provider uses for the "password"
     * credential for authenticating to their service.
     *
     * @return the label displayed for the "password" credential
     */
    public String getPasswordLabel();

    /**
     * Returns a Set of all AuthTypes that are supported by a provider.
     * @return all supported AuthTypes
     */
    public Set<AuthType> getSupportedAuthTypes();


}
