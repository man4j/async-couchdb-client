package com.equiron.acc.tutorial.lesson7;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class Message extends ForumContent {
    private String topicId;

    private long createdAt = System.currentTimeMillis();

    public Message(String text, String topicId) {
        super(text);
        this.topicId = topicId;
    }
}
