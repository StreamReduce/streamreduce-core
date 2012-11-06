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

package com.streamreduce.storm;

/**
 * All possible group names used for declaring specific groupings in Storm Topologies.
 *
 * These obviate the need to use Class.getSimpleName for classes in com.streamreduce.core.model to define groupings, although
 * the names defined match the simple names of these model classes.
 */
public final class GroupingNameConstants {
    private GroupingNameConstants() {}

    public static final String ACCOUNT_GROUPING_NAME = "Account";
    public static final String INVENTORY_ITEM_GROUPING_NAME = "InventoryItem";
    public static final String CONNECTION_GROUPING_NAME = "Connection";
    public static final String USER_GROUPING_NAME = "User";
    public static final String MESSAGE_GROUPING_NAME = "SobaMessage";

}
