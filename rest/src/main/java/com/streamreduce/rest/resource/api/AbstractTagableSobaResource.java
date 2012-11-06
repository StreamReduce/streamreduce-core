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
