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

package com.streamreduce.core.routes;

import java.util.concurrent.TimeUnit;

import com.streamreduce.util.SqsQueueNameFormatter;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public abstract class NodeableRouteBuilder extends SpringRouteBuilder implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(NodeableRouteBuilder.class);
    public static final long SQS_MESSAGE_RETENTION_PERIOD = TimeUnit.DAYS.toSeconds(14);

    protected String queueName;
    protected String brokerType;
    protected String environmentPrefix;
    protected String endpointUrl;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isBlank(queueName)) {
            throw new IllegalStateException("queueName property must be non-blank");
        }

        if ("sqs".equals(brokerType.trim())) {
            if (StringUtils.isBlank(environmentPrefix)) {
                throw new IllegalStateException("If brokerType is \"sqs\", then environmentPrefix " +
                                                        "property must be non-blank");
            }
            queueName = SqsQueueNameFormatter.formatSqsQueueName(queueName, environmentPrefix);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() throws Exception {
        if ("sqs".equals(brokerType)) {
            // AmazonSQSClient is looked up from registry
            endpointUrl = String.format("aws-sqs://%s?amazonSQSClient=#amazonSQSClient&messageRetentionPeriod=%d",
                                        queueName, SQS_MESSAGE_RETENTION_PERIOD);
        } else if ("file".equals(brokerType)) {
            endpointUrl = String.format("file://%s/.nodeable/%s/?delete=true&exclude=^tmp.*",
                                        System.getProperty("user.home"), queueName);
        } else if ("amq".equals(brokerType)) {
            endpointUrl = String.format("amq:queue:%s?concurrentConsumers=10&acknowledgementModeName=" +
                                                "CLIENT_ACKNOWLEDGE", queueName);
        } else {
            logger.warn("brokerType property was not one of sqs, file, or amq.  No Camel routes were started.");
        }
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public void setBrokerType(String brokerType) {
        this.brokerType = brokerType;
    }

    public void setEnvironmentPrefix(String environmentPrefix) {
        this.environmentPrefix = environmentPrefix;
    }

}
