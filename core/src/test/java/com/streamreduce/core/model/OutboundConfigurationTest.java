package com.streamreduce.core.model;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutboundConfigurationTest {

    @Test(expected=IllegalStateException.class)
    public void testOutboundConfigurationDoesNotAllowNullCredentials() {
        new OutboundConfiguration.Builder()
                .credentials(null)
                .protocol("foo")
                .destination("bar")
                .namespace("baz")
                .build();
    }

    @Test(expected=IllegalStateException.class)
    public void testOutboundConfigurationDoesNotAllowNullIdentity() {
        new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials(null,""))
                .protocol("foo")
                .destination("bar")
                .namespace("baz")
                .build();
    }

    @Test(expected=IllegalStateException.class)
    public void testOutboundConfigurationDoesNotAllowEmptyIdentity() {
        new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("    ",""))
                .protocol("foo")
                .destination("bar")
                .namespace("baz")
                .build();
    }

    @Test(expected=IllegalStateException.class)
    public void testOutboundConfigurationDoesNotAllowNullProtocol() {
        new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol(null)
                .destination("bar")
                .namespace("baz")
                .build();
    }

    @Test(expected=IllegalStateException.class)
    public void testOutboundConfigurationDoesNotAllowBlankProtocol() {
        new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user","pass"))
                .protocol("   " )
                .destination("bar")
                .namespace("baz")
                .build();
    }

    @Test(expected=IllegalStateException.class)
    public void testOutboundConfigurationDoesNotAllowEmptyDataTypes() {
        new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user","pass"))
                .protocol("s3" )
                .destination("bar")
                .dataTypes()
                .namespace("baz")
                .build();
    }

    @Test
    public void testOutboundConfigurationGracefullyHandlesNoContainerOrNamespace() {
        OutboundConfiguration outboundConfiguration= new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        Assert.assertEquals("",outboundConfiguration.getDestination());
        Assert.assertEquals("",outboundConfiguration.getNamespace());
    }

    @Test
    public void testOutboundConfigurationBuilderAllowsNullOriginatingConnection() {
        OutboundConfiguration outboundConfiguration= new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .dataTypes(OutboundDataType.PROCESSED)
                .originatingConnection(null)
                .build();

        Assert.assertEquals("",outboundConfiguration.getDestination());
        Assert.assertEquals("",outboundConfiguration.getNamespace());
    }

    @Test
    public void testEquals() {

        OutboundConfiguration outboundConfigurationA = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        OutboundConfiguration outboundConfigurationB = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        Assert.assertTrue(outboundConfigurationA.equals(outboundConfigurationB));
    }

    @Test
    public void testNotEqualsBecauseOfDataTypes() {
        OutboundConfiguration outboundConfigurationA = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        OutboundConfiguration outboundConfigurationB = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED,OutboundDataType.RAW)
                .build();

        Assert.assertFalse(outboundConfigurationA.equals(outboundConfigurationB));
    }

    @Test
    public void testEqualHashCode() {
        OutboundConfiguration outboundConfigurationA = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        OutboundConfiguration outboundConfigurationB = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        Assert.assertEquals(outboundConfigurationA.hashCode(), outboundConfigurationB.hashCode());
    }

    @Test
    public void testUnequalHashCodeBecauseOfCredentials() {
        Connection connectionMockA = mock(Connection.class);
        when(connectionMockA.getId()).thenReturn(new ObjectId());
        Connection connectionMockB = mock(Connection.class);
        when(connectionMockB.getId()).thenReturn(new ObjectId());
        OutboundConfiguration outboundConfigurationA = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        OutboundConfiguration outboundConfigurationB = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("foo", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        Assert.assertFalse(outboundConfigurationA.hashCode() == outboundConfigurationB.hashCode());
    }
    @Test
    public void testUnequalHashCodeBecauseOfDataTypes() {
        OutboundConfiguration outboundConfigurationA = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        OutboundConfiguration outboundConfigurationB = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("user", "pass" ))
                .protocol("s3")
                .destination("bucket")
                .namespace("prefix")
                .dataTypes(OutboundDataType.PROCESSED, OutboundDataType.RAW)
                .build();

        Assert.assertFalse(outboundConfigurationA.hashCode() == outboundConfigurationB.hashCode());
    }

}
