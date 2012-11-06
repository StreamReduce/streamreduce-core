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
