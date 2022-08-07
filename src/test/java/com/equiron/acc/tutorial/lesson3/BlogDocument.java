package com.equiron.acc.tutorial.lesson3;

import com.equiron.acc.json.YnsDocument;

public class BlogDocument extends YnsDocument implements Comparable<BlogDocument> {
    private long createdAt = System.currentTimeMillis();

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public int compareTo(BlogDocument o) {
        return Long.compare(createdAt, o.getCreatedAt());
    }
}
