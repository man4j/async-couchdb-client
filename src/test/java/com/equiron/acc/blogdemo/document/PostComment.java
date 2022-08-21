package com.equiron.acc.blogdemo.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostComment extends BlogContent {
    private String blogPostId;
    
    private String parentCommentId;

    public PostComment(String content, String ownerId, String blogPostId, String parentCommentId) {
        super(content, ownerId);
        this.blogPostId = blogPostId;
        this.parentCommentId = parentCommentId;
    }
}
