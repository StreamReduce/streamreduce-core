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
