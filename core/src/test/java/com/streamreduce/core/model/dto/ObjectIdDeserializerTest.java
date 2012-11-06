package com.streamreduce.core.model.dto;

import org.bson.types.ObjectId;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObjectIdDeserializerTest {

    static ObjectMapper mapper;

    @BeforeClass
    public static void createMapper() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("whatevs", new Version(0, 0, 0, null));
        module.addSerializer(new ObjectIdSerializer());
        module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        ObjectId expected = new ObjectId();
        String json = mapper.writeValueAsString(expected);
        ObjectId actual = mapper.readValue(json,ObjectId.class);
        assertEquals(expected,actual);
    }
}
