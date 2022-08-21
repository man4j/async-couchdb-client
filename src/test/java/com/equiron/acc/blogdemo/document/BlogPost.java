package com.equiron.acc.blogdemo.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogPost extends BlogContent {
    private String title;

    public BlogPost(String title, String content, String ownerId) {
        super(content, ownerId);
        this.title = title;
    }
}
