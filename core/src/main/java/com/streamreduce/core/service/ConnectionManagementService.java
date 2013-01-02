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

package com.streamreduce.core.service;

/**
 * Interface used to provide JMX management functionality for Connection objects.
 *
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/30/12 16:03</p>
 */
public interface ConnectionManagementService {

    /**
     * Returns all connections as a formatted string, ideally JSON.
     *
     * @return formatted String
     */
    String getAllConnections(String type, boolean summary);

    /**
     * Returns disabled connections as a formatted string, ideally JSON.
     *
     * @return formatted String
     */
    String getDisabledConnections(String type, boolean summary);

    /**
     * Returns a specified connection as a formatted string, ideally JSON.
     *
     * @return formatted String
     */
    String getConnection(String connectionObjectId);


    /**
     * Disables a connection.
     *
     * @param connectionObjectId
     */
    void disableConnection(String connectionObjectId);

    /**
     * Enables a connection.
     *
     * @param connectionObjectId
     */
    void enableConnection(String connectionObjectId);

}
