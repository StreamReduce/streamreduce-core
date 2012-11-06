package com.streamreduce.core.model.dto;

import org.bson.types.ObjectId;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Implementation of {@link JsonSerializer} to help serialize {@link ObjectId} objects.
 */
public final class ObjectIdSerializer extends JsonSerializer<ObjectId> {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Class<ObjectId> handledType() {
        return ObjectId.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(ObjectId objectId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {

        jsonGenerator.writeString(objectId.toString());
    }

}
