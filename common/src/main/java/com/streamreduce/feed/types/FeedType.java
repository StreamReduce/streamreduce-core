package com.streamreduce.feed.types;

public enum FeedType {
    RSS("rss");

    private String representation;

    FeedType(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return this.representation;
    }
}
