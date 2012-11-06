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
