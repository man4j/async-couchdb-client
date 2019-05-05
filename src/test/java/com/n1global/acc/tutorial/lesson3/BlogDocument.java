package com.n1global.acc.tutorial.lesson3;

import com.equiron.acc.json.CouchDbDocument;

public class BlogDocument extends CouchDbDocument implements Comparable<BlogDocument> {
    private long createdAt = System.currentTimeMillis();

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public int compareTo(BlogDocument o) {
        return Long.compare(createdAt, o.getCreatedAt());
    }
}
