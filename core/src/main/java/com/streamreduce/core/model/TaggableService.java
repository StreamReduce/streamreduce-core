package com.streamreduce.core.model;

// we don't use a single collection for persistence
// so we have all these different tag impls for services,
// and REST resources... yet SobaObject is where the tags are stored. ugh!
public interface TaggableService<T extends Taggable> {

    void addHashtag(T target, SobaObject tagger, String tag);

    void removeHashtag(T target, SobaObject tagger, String tag);

}
