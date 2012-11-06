package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Entity;

@SuppressWarnings("rawtypes")
@Entity(value = "hashtagFilter", noClassnameStored = true)
public class HashtagFilter extends SobaObject {

    private static final long serialVersionUID = 9119678677377834014L;

    // inherits hashtags, name, user, etc from parent
}
