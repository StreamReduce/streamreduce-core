package com.streamreduce.core.model;

import java.util.Set;

public interface Taggable {

    void addHashtags(Set<String> hashtags);

    void addHashtag(String tag);

    void removeHashtag(String tag);

}
