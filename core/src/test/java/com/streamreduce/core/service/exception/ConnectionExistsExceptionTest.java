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

import com.streamreduce.core.model.Connection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/1/12 10:21 AM</p>
 */
public class ConnectionExistsExceptionTest {

    private Connection connection;

    @Before
    public void setUp() throws Exception {
        connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getProviderId()).thenReturn("ConnectionExistsExceptionTest");
        Mockito.when(connection.getAlias()).thenReturn("ConnectionExistsExceptionTest Alias");
    }

    @Test
    public void testDuplicateCredentials() throws Exception {
        Assert.assertEquals(ConnectionExistsException.Factory.duplicateCredentials(connection).getMessage(),
                "A connection for the ConnectionExistsExceptionTest provider already exists using the same credentials, URL, or both.");
    }

    @Test
    public void testDuplicateAlias() throws Exception {
        Assert.assertEquals(ConnectionExistsException.Factory.duplicateAlias(connection).getMessage(),
                "A connection for the ConnectionExistsExceptionTest provider already exists with the connection name 'ConnectionExistsExceptionTest Alias'.");
    }

}
