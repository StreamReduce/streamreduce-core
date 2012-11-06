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

package com.streamreduce.core.component;

import com.streamreduce.core.dao.MetricDAO;
import com.streamreduce.core.model.Metric;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import net.sf.json.JSONObject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/11/12 15:29</p>
 */
@Component
@ManagedResource(objectName = "com.streamreduce.core.component:type=QueueMessageProcessor,name=queue-msg-processor")
public class QueueMessageProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueMessageProcessor.class);
    private static final Counter consumedCounter = Metrics.newCounter(QueueMessageProcessor.class, "queue-msg-processor-consumed-count");
    private static final Counter processedCounter = Metrics.newCounter(QueueMessageProcessor.class, "queue-msg-processor-processed-count");
    private static final Counter errorCounter = Metrics.newCounter(QueueMessageProcessor.class, "queue-msg-processor-error-count");

    @Resource
    private MetricDAO metricDAO;

    @Override
    public void process(Exchange exchange) throws Exception {
        String payload = exchange.getIn().getBody(String.class);
        JSONObject json = JSONObject.fromObject(payload);
        consumedCounter.inc();
        Metric metric = processPayload(json);
        exchange.getOut().setBody(metric);
    }

    private Metric processPayload(Map<String, Object> payload) throws Exception {
        String account = null;
        if (payload.containsKey("account") && payload.get("account") != null) {
            account = (String) payload.get("account");
        }

        LOGGER.info("Received event {} from queue for account {}", consumedCounter.count(), account);

        Metric metric = null;
        try {
            metric = new Metric(payload);
            metricDAO.save(metric);
            processedCounter.inc();
        }
        catch (Exception e) {
            errorCounter.inc();
        }

        return metric;
    }
}
