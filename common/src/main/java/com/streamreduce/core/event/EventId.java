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
