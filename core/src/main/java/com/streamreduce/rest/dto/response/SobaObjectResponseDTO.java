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
