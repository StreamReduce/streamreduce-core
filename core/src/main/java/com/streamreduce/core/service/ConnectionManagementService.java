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
     * Returns broken connections as a formatted string, ideally JSON.
     *
     * @return formatted String
     */
    String getBrokenConnections(String type, boolean summary);

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
     * Flags a connection as broken.
     *
     * @param connectionObjectId
     */
    void setAsBroken(String connectionObjectId);

    /**
     * Flags a connection as unbroken.
     *
     * @param connectionObjectId
     */
    void setAsUnbroken(String connectionObjectId);

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
