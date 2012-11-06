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
