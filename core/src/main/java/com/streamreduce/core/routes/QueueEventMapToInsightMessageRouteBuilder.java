package com.streamreduce.core.routes;

import com.streamreduce.core.component.EventMapToInsightMessageConsumer;
import org.springframework.beans.factory.annotation.Autowired;

public class QueueEventMapToInsightMessageRouteBuilder extends NodeableRouteBuilder {

    @Autowired
    EventMapToInsightMessageConsumer eventMapToInsightMessageConsumerProcessor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() throws Exception {
        super.configure();

        //From Storm/Analytics to Nodeable
        from(endpointUrl).process(eventMapToInsightMessageConsumerProcessor);
    }

}
