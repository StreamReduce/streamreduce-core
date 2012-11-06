package com.streamreduce.rest.resource.api;

import com.streamreduce.rest.resource.AbstractResource;

import javax.ws.rs.core.Response;

import net.sf.json.JSONObject;
import org.bson.types.ObjectId;

public abstract class AbstractTagableSobaResource extends AbstractResource {

    public static String HASHTAG = "hashtag";

    protected abstract Response addTag(ObjectId id, JSONObject hashtag);

    protected abstract Response getTags(ObjectId id);

    protected abstract Response removeTag(ObjectId id, String hashtag);


}
