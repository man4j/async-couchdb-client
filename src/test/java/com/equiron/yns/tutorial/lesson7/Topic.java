package com.equiron.yns.tutorial.lesson7;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Topic extends ForumContent {
    public Topic(String text) {
        super(text);
    }
}
