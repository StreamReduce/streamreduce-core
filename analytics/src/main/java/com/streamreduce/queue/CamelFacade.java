package com.streamreduce.queue;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.streamreduce.util.JSONUtils;
import com.streamreduce.util.PropertiesOverrideLoader;
import com.streamreduce.util.SqsQueueNameFormatter;
import net.sf.json.JSONObject;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class CamelFacade {
    private CamelFacade() {
    }

    static private final Logger LOGGER = Logger.getLogger(CamelFacade.class);
    static private ProducerTemplate producerTemplate;
    static private String brokerType;


    private static void startCamelContextAndSetProducerTemplate() {
        try {
            Properties messageBrokerProperties = PropertiesOverrideLoader.loadProperties("messagebroker.properties");
            final String eventsToInsightsQueueName = messageBrokerProperties.getProperty("eventMapsToInsightsQueueName");
            final String environment = messageBrokerProperties.getProperty("broker.environment.prefix");
            brokerType = messageBrokerProperties.getProperty("broker.type");  //sendInsightMessage uses brokerType

            //work around for https://issues.apache.org/jira/browse/CAMEL-5453
            //Register a SQSClient in the context with credentials already supplied.
            final SQSClientAndEndPointPair sqsClientAndEndPointPair =
                    setupSqsEndpointAndClient(eventsToInsightsQueueName,environment);
            JndiContext jndiContext = new JndiContext();
            jndiContext.bind("amazonSQSClient", sqsClientAndEndPointPair.amazonSQSClient);

            CamelContext camelContext = new DefaultCamelContext(jndiContext);

            final String amqEndpoint = setupAmqComponentAndEndpoint(camelContext, messageBrokerProperties,
                    eventsToInsightsQueueName);
            final String fileEndpoint = setupFileComponent(eventsToInsightsQueueName); //For local dev

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    if ("amq" .equals(brokerType)) {
                        from("direct:sendEventToAmq").to(amqEndpoint);
                    }
                    if ("sqs" .equals(brokerType)) {
                        from("direct:sendEventToSqs").to(sqsClientAndEndPointPair.endpoint);
                    }
                    if ("file" .equals(brokerType)) {
                        from("direct:sendEventToTempDir").to(fileEndpoint);
                    }
                }
            });

            camelContext.start();
            producerTemplate = camelContext.createProducerTemplate();
            LOGGER.info("Camel (Insight Message Producer) started successfully");
        } catch (Exception e) {
            LOGGER.error("Error starting the Camel context: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static String setupFileComponent(String eventsToInsightsQueueName) {
        String tmpDirPath =  System.getProperty( "user.home" ) + "/.nodeable/" + eventsToInsightsQueueName;
        File tmpDir = new File(tmpDirPath);
        tmpDir.mkdir();
        return "file://" + tmpDirPath + "?tempPrefix=tmp.";
    }

    private static SQSClientAndEndPointPair setupSqsEndpointAndClient(String queueName, String environment) throws UnsupportedEncodingException {
        Properties cloudProperties = PropertiesOverrideLoader.loadProperties("cloud.properties");
        String accessKeyId = cloudProperties.getProperty("nodeable.aws.accessKeyId");
        String secretKey = cloudProperties.getProperty("nodeable.aws.secretKey");
        long messageRetentionPeriod = TimeUnit.DAYS.toSeconds(14);
        String actualQueueName = SqsQueueNameFormatter.formatSqsQueueName(queueName,environment);

        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId,secretKey);
        AmazonSQSClient sqsClient = new AmazonSQSClient(awsCredentials);
        String endpoint =  String.format("aws-sqs://" + actualQueueName + "?amazonSQSClient=#amazonSQSClient&" +
                "messageRetentionPeriod=%d",messageRetentionPeriod);
        return new SQSClientAndEndPointPair(sqsClient,endpoint);
    }

    private static String setupAmqComponentAndEndpoint(CamelContext camelContext, Properties messageBrokerProperties, String eventsToInsightsQueueName) {
        String brokerUrl = messageBrokerProperties.getProperty("activemq.broker.url");
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);

        pooledConnectionFactory.setMaxConnections(8);
        pooledConnectionFactory.setMaximumActive(500);
        pooledConnectionFactory.setIdleTimeout(0); //No timeout for connections in the pool

        ActiveMQComponent activeMQComponent = ActiveMQComponent.activeMQComponent();
        activeMQComponent.setUsePooledConnection(true);
        activeMQComponent.setConnectionFactory(pooledConnectionFactory);
        camelContext.addComponent("amq", activeMQComponent);

        return "amq:queue:" + eventsToInsightsQueueName + "?jmsMessageType=Text";
    }

    public static void sendInsightMessage(Map<String, Object> eventMap) {
        if (producerTemplate == null) {
            startCamelContextAndSetProducerTemplate();
        }

        JSONObject jsonObject = JSONObject.fromObject(JSONUtils.sanitizeMapForJson(eventMap));

        if ("sqs".equals(brokerType)) {
            producerTemplate.sendBody("direct:sendEventToSqs",jsonObject.toString());
            LOGGER.debug("Insight message sent to SQS: " + jsonObject);
        } else if ("amq".equals(brokerType)) {
            producerTemplate.sendBody("direct:sendEventToAmq", jsonObject.toString());
            LOGGER.debug("Insight message sent to AMQ: " + jsonObject);
        } else if ("file".equals(brokerType))  {
            producerTemplate.sendBody("direct:sendEventToTempDir", jsonObject.toString());
            LOGGER.debug("Insight message sent to Temp Directory: " + eventMap);
        } else {
            throw new RuntimeException("Unable to write event to queue.  brokerType was   \"" + brokerType +
                    "\" was not one of \"sqs\", \"amq\", or \"file\"");
        }
    }

    private static class SQSClientAndEndPointPair {
        final AmazonSQSClient amazonSQSClient;
        final String endpoint;
        SQSClientAndEndPointPair(AmazonSQSClient amazonSQSClient, String endpoint) {
            this.amazonSQSClient = amazonSQSClient;
            this.endpoint = endpoint;
        }
    }

    public static void main(String[] args) {
        sendInsightMessage(new HashMap<String,Object>());
    }
}
