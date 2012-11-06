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


import com.streamreduce.ConnectionTypeConstants;

/**
 * The CloudProvider interface describes a cloud provider.
 */
public interface CloudProvider extends ConnectionProvider {

    public static final String TYPE = ConnectionTypeConstants.CLOUD_TYPE;

    /**
     * Returns a string that maps to the jclouds provider id for the
     * provider's compute service.
     *
     * @return the jclouds id for the provider's compute service
     */
    public String getComputeId();

}
