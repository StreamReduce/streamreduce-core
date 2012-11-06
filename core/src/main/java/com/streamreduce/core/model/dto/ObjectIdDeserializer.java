package com.streamreduce.core.model.dto;

import org.bson.types.ObjectId;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
    @Override
    public ObjectId deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String id = jp.getText();
        return new ObjectId(id);
    }
}
