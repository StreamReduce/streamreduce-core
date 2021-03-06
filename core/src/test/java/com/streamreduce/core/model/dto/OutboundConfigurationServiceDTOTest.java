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

package com.streamreduce.core.model.dto;

import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class OutboundConfigurationServiceDTOTest {

    @Test
    public void testSerializeAndDeserialize_EqualsAndHashCode() throws Exception {
        OutboundConfiguration outboundConfiguration = new OutboundConfiguration.Builder()
                .protocol("s3")
                .destination("us-east")
                .namespace("my.foo.bucket")
                .credentials(new ConnectionCredentials("foo", "bar"))
                .dataTypes(OutboundDataType.PROCESSED).build();
        OutboundConfigurationServiceDTO dtoBefore = new OutboundConfigurationServiceDTO(outboundConfiguration);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(dtoBefore);
        OutboundConfigurationServiceDTO dtoAfter = mapper.reader(OutboundConfigurationServiceDTO.class).readValue(json);
        Assert.assertEquals(dtoBefore, dtoAfter);
        Assert.assertEquals(dtoBefore.hashCode(), dtoAfter.hashCode());
    }
}
