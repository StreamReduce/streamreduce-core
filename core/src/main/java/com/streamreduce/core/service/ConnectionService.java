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

import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.TaggableService;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.ConnectionExistsException;
import com.streamreduce.core.service.exception.InvalidCredentialsException;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.bson.types.ObjectId;

public interface ConnectionService extends TaggableService<Connection> {

    /**
     * Creates a connection.
     *
     * @param connection the connection to create
     *
     * @return the created connection
     *
     * @throws InvalidCredentialsException if the cloud credentials are invalid
     * @throws IOException if there is connectivity issue
     * @throws ConnectionExistsException if there is already a cloud object for provider/user/credentials combination
     */
    public Connection createConnection(Connection connection) throws ConnectionExistsException,
            InvalidCredentialsException, IOException;

    /**
     * Returns all connections of a particular type or all connections if type is null.
     *
     * @param type the type to filter all connections by, or null to get all connections
     *
     * @return the connections
     */
    public List<Connection> getConnections(@Nullable String type);

    /**
     * Returns all connections of a particular type that are public
     *
     * @param type the type to filter all connections by, or null to get all connections
     *
     * @return the connections
     */
    public List<Connection> getPublicConnections(@Nullable String type);

    /**
     * Returns all connections of a particular type for the given user.
     *
     * @param type the type to filter all connections by
     * @param user the user to filter all connections by
     *
     * @return the connections
     */
    public List<Connection> getConnections(String type, User user);

    public List<Connection> getAccountConnections(Account account);

    /**
     * Returns the connection with the given id.
     *
     * @param id the id of the connection
     *
     * @return the connection
     */
    public Connection getConnection(ObjectId id) throws ConnectionNotFoundException;

    /**
     * Updates the connection.
     *
     * @param connection the connection to update
     *
     * @return the updated connection
     *
     * @throws InvalidCredentialsException if the cloud credentials are invalid
     * @throws IOException if there is connectivity issue
     * @throws ConnectionExistsException if there is already a cloud object for provider/user/credentials combination
     */
    public Connection updateConnection(Connection connection) throws ConnectionExistsException,
            InvalidCredentialsException, IOException;

    /**
     * Updates the connection but suppresses the object event.
     *
     * @param connection the connection to update
     * @param silentUpdate indicate whether or not suppress the connection event (events and validation are skipped if silent update)
     *
     * @return the updated connection
     *
     * @throws InvalidCredentialsException if the cloud credentials are invalid
     * @throws IOException if there is connectivity issue
     * @throws ConnectionExistsException if there is already a cloud object for provider/user/credentials combination
     */
    public Connection updateConnection(Connection connection, boolean silentUpdate) throws ConnectionExistsException,
            InvalidCredentialsException, IOException;


    /**
     * Deletes the connection.
     *
     * @param connection the connection to delete
     */
    public void deleteConnection(Connection connection);

    /**
     * Deletes the associated inventory of the connection.
     *
     * @param connectionId the id of the connection object
     */
    void deleteConnectionInventory(ObjectId connectionId);

    /**
     * Fires a one time high priority inventory refresh job for a single connection.
     *
     * @param connection the connection whose job to run
     *
     * @throws InvalidCredentialsException if the cloud credentials are invalid
     * @throws IOException if there is connectivity issue
     * @throws ConnectionExistsException if there is already a cloud object for provider/user/credentials combination
     */
    public void fireOneTimeHighPriorityJobForConnection(Connection connection) throws ConnectionNotFoundException,
            InvalidCredentialsException, IOException;

    /**
     * Flags a connection as broken. This method is used by the inventory polling jobs when it finds invalid credentials
     * or successive exceptions in the refresh process. In the invalid credentials case the broken flag can only be reset
     * in manual connection update.
     *
     * @param connection the connection that will be flagged as broken
     */
    void flagConnectionAsBroken(Connection connection, String lastErrorMessage);

    /**
     * Clears the connection broken flag. This method is used by the inventory polling jobs to automatically reactivate
     * the refresh of broken connections after the configured elapsed time.
     *
     * @param connection the connection that will have the broken status cleared
     */
    void clearBrokenFlag(Connection connection);

    /**
     * The guid should be a meta attr on the inventory item.
     * @param guid
     * @return
     */
    Connection getConnectionByGUID(String guid) throws ConnectionNotFoundException;

}
