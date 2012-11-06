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

package com.streamreduce.client.outbound;

import com.streamreduce.core.model.OutboundConfiguration;
import net.sf.json.JSONObject;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class S3OutboundClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void testPutRawMessageWithNullPayload() throws Exception {
        new S3OutboundClient(mock(OutboundConfiguration.class)).putRawMessage(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutRawMessageWithBlankPayload() throws Exception {
        new S3OutboundClient(mock(OutboundConfiguration.class)).putRawMessage(new JSONObject());
    }
}
