package com.streamreduce.core.routes;


import com.streamreduce.core.component.OutboundMessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class QueueOutbountMessageRouteBuilder extends NodeableRouteBuilder {

    @Autowired
    OutboundMessageProcessor outboundMessageProcessor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() throws Exception {
        super.configure();

        // Async Handling of OutboundMessages
        from("direct:outbound-messages").to(endpointUrl);
        from(endpointUrl).process(outboundMessageProcessor);
    }

}
