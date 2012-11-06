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

package com.streamreduce.core.transformer.message;

import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

public interface MessageTransformer {

    /**
     * Creates a message based on the event.
     *
     * @param event the event to create the message from
     *
     * @return the message
     */
    public String doTransform(Event event);


    public SobaMessageDetails getDetails();

}
