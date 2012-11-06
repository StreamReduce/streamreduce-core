package com.streamreduce.core.routes;

public class QueueMessageRouteBuilder extends NodeableRouteBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() throws Exception {
        super.configure();

        from(this.endpointUrl).to("bean:queueMessageProcessor");
    }

}
