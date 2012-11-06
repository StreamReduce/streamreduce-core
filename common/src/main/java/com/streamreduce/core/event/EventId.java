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

package com.streamreduce.core.event;

public enum EventId {

    /* CRUD Events */
    CREATE,
    READ,
    UPDATE,
    DELETE,

    /* Activity Events */
    ACTIVITY,

    /* System Events*/
    NODEABLE_SYSTEM_REBOOT,

    /* Hashtag event ids */
    HASHTAG_ADD,
    HASHTAG_DELETE,

    /* User event ids */
    CREATE_USER_REQUEST,
    CREATE_USER_INVITE_REQUEST,
    DELETE_USER_INVITE_REQUEST,
    USER_PASSWORD_RESET_REQUEST,
    USER_MESSAGE,

    /* CloudInventoryItem event Ids*/
    CLOUD_INVENTORY_ITEM_REBOOT,
    CLOUD_INVENTORY_ITEM_REBOOT_FAILURE,
    CLOUD_INVENTORY_ITEM_TERMINATE,
    CLOUD_INVENTORY_ITEM_TERMINATE_FAILURE,

    /* SobaMessage Event Ids */
    CREATE_GLOBAL_MESSAGE,
    MESSAGE_COMMENT,

    /* Nodebelly event Ids*/
    NODEBELLY_STATUS, // once per hour, aggregates across connections within a service TODO
    NODEBELLY_SUMMARY, // once per day, aggregates across connections within a service
    NODEBELLY_ANOMALY, // anything statistically notable

    /* Subscription Events*/
    SUBSCRIPTION_TRIAL_EXPIRING,
    SUBSCRIPTION_TRIAL_EXPIRED

}
