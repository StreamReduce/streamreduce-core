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

package com.streamreduce.rest.dto.response;

import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.dto.ObjectIdSerializer;

import java.util.Set;
import java.util.TreeSet;

import org.bson.types.ObjectId;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class SobaObjectResponseDTO extends ObjectWithIdResponseDTO {

    private String alias;
    private String description;
    private ObjectId accountId;
    private ObjectId userId;
    private Set<String> hashtags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private SobaObject.Visibility visibility;

    public SobaObject.Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(SobaObject.Visibility visibility) {
        this.visibility = visibility;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonSerialize(using = ObjectIdSerializer.class)
    public ObjectId getAccountId() {
        return accountId;
    }

    public void setAccountId(ObjectId accountId) {
        this.accountId = accountId;
    }

    @JsonSerialize(using=ObjectIdSerializer.class)
    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public Set<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(Set<String> hashtags) {
        this.hashtags = hashtags;
    }

}
