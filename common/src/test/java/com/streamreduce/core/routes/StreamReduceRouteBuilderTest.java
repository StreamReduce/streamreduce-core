package com.streamreduce.core.routes;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


public class StreamReduceRouteBuilderTest {

    StreamReduceRouteBuilder routeBuilderUnderTest;

    @Before
    public void setUp() throws Exception {
        routeBuilderUnderTest = spy(new StreamReduceRouteBuilder() {
            @Override
            protected void configureRoutes() throws Exception {
            }
        });
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testAfterPropertiesSet_sqsBrokerTypeModifiesQueueName() throws Exception {
        routeBuilderUnderTest.setBrokerType("sqs");
        routeBuilderUnderTest.setQueueName("testQueue");
        routeBuilderUnderTest.setEnvironmentPrefix("env");
        routeBuilderUnderTest.afterPropertiesSet();

        assertTrue(routeBuilderUnderTest.queueName.contains("env") &&
                routeBuilderUnderTest.queueName.contains("testQueue"));
    }

    @Test(expected = IllegalStateException.class)
    public void testAfterPropertiesSet_brokerTypeOfSqsWithBlankEnvPrefix() throws Exception {
        routeBuilderUnderTest.setBrokerType("sqs");
        routeBuilderUnderTest.setEnvironmentPrefix(" ");
        routeBuilderUnderTest.afterPropertiesSet();
    }

    @Test(expected = IllegalStateException.class)
    public void testAfterPropertiesSet_brokerTypeOfSqsWithNullEnvPrefix() throws Exception {
        routeBuilderUnderTest.setBrokerType("sqs");
        routeBuilderUnderTest.setEnvironmentPrefix(null);
        routeBuilderUnderTest.afterPropertiesSet();
    }

    @Test(expected = IllegalStateException.class)
    public void testAfterPropertiesSet_nullQueueName() throws Exception {
        routeBuilderUnderTest.setQueueName(null);
        routeBuilderUnderTest.afterPropertiesSet();
    }

    @Test(expected = IllegalStateException.class)
    public void testAfterPropertiesSet_blankQueueName() throws Exception {
        routeBuilderUnderTest.setQueueName("");
        routeBuilderUnderTest.afterPropertiesSet();
    }

    @Test(expected = IllegalStateException.class)
    public void testAfterPropertiesSet_sqsBrokerTypeWithNullEnvironmentPrefix() throws Exception {
        routeBuilderUnderTest.setBrokerType("sqs");
        routeBuilderUnderTest.setQueueName("testQueue");
        routeBuilderUnderTest.setEnvironmentPrefix(null);
        routeBuilderUnderTest.afterPropertiesSet();
    }

    @Test(expected = IllegalStateException.class)
    public void testAfterPropertiesSet_sqsBrokerTypeWithBlankEnvironmentPrefix() throws Exception {
        routeBuilderUnderTest.setBrokerType("sqs");
        routeBuilderUnderTest.setQueueName("testQueue");
        routeBuilderUnderTest.setEnvironmentPrefix("  ");
        routeBuilderUnderTest.afterPropertiesSet();
    }

    @Test
    public void testConfigure_sqsBrokerType() throws Exception {
        routeBuilderUnderTest.setBrokerType("sqs");
        routeBuilderUnderTest.setQueueName("testQueue");
        routeBuilderUnderTest.setEnvironmentPrefix("prod");
        routeBuilderUnderTest.afterPropertiesSet();
        routeBuilderUnderTest.configure();

        assertTrue(routeBuilderUnderTest.endpointUrl.startsWith("aws-sqs"));
    }

    @Test
    public void testConfigure_fileBrokerType() throws Exception {
        routeBuilderUnderTest.setBrokerType("file");
        routeBuilderUnderTest.setQueueName("testQueue");
        routeBuilderUnderTest.afterPropertiesSet();
        routeBuilderUnderTest.configure();

        assertTrue(routeBuilderUnderTest.endpointUrl.startsWith("file"));
    }

    @Test
    public void testConfigure_amqBrokerType() throws Exception {
        routeBuilderUnderTest.setBrokerType("amq");
        routeBuilderUnderTest.setQueueName("testQueue");
        routeBuilderUnderTest.afterPropertiesSet();
        routeBuilderUnderTest.configure();

        assertTrue(routeBuilderUnderTest.endpointUrl.startsWith("amq"));
    }

    @Test
    public void testConfigure_unknownBrokerType() throws Exception {
        routeBuilderUnderTest.setBrokerType("blahblahblah");
        routeBuilderUnderTest.setQueueName("testQueue");
        routeBuilderUnderTest.afterPropertiesSet();
        routeBuilderUnderTest.configure();

        verify(routeBuilderUnderTest, never()).configureRoutes();
    }

    @Test
    public void testSetQueueName() throws Exception {
        routeBuilderUnderTest.setQueueName("allTheMessages");
        assertEquals("allTheMessages", routeBuilderUnderTest.queueName);
    }

    @Test
    public void testSetBrokerType() throws Exception {
        routeBuilderUnderTest.setBrokerType("sqs");
        Assert.assertEquals("sqs", routeBuilderUnderTest.brokerType);
    }

    @Test
    public void testSetEnvironmentPrefix() throws Exception {
        routeBuilderUnderTest.setBrokerType("dev");
        Assert.assertEquals("dev", routeBuilderUnderTest.brokerType);
    }
}
