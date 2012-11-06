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

package com.streamreduce.core.service.exception;

import com.streamreduce.DuplicateItemException;
import com.streamreduce.core.model.Connection;

public class ConnectionExistsException extends DuplicateItemException {

    private static final long serialVersionUID = -3989583176254745073L;

    public ConnectionExistsException(String message) {
        super(message);
    }

    public ConnectionExistsException(Connection connection) {
        super("There is already a connection for the " + connection.getProviderId() +
                " provider that uses the same credentials or URL.");
    }

    public static final class Factory {

        private static final String DUPLICATE_CREDS = "A connection for the %s provider already exists using the same credentials, URL, or both.";
        private static final String DUPLICATE_ALIAS = "A connection for the %s provider already exists with the connection name '%s'.";

        public static ConnectionExistsException duplicateCredentials(Connection connection) {
            return new ConnectionExistsException(String.format(DUPLICATE_CREDS, connection.getProviderId()));
        }

        public static ConnectionExistsException duplicateAlias(Connection connection) {
            return new ConnectionExistsException(String.format(DUPLICATE_ALIAS, connection.getProviderId(), connection.getAlias()));
        }
    }

}
