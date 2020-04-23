package com.equiron.acc.tutorial.lesson7;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message extends ForumContent {
    private String text;

    private String topicId;

    private long createdAt = System.currentTimeMillis();

    @JsonCreator
    public Message(@JsonProperty("text") String text, @JsonProperty("topicId") String topicId) {
        this.text = text;
        this.topicId = topicId;
    }

    public String getText() {
        return text;
    }

    public String getTopicId() {
        return topicId;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
