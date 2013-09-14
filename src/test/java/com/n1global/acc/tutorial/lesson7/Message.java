package com.n1global.acc.tutorial.lesson7;

public class Message extends ForumContent implements Comparable<Message> {
    private String text;

    private String topicId;

    private long createdAt = System.currentTimeMillis();

    public Message() {
        /* empty */
    }

    public Message(String text, String topicId) {
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

    @Override
    public int compareTo(Message o) {
        return Long.valueOf(createdAt).compareTo(o.getCreatedAt());
    }
}
