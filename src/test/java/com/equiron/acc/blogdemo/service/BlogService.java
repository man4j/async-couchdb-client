package com.equiron.acc.blogdemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.equiron.acc.blogdemo.db.BlogDb;
import com.equiron.acc.blogdemo.document.BlogPost;
import com.equiron.acc.blogdemo.document.PostComment;

@Service
public class BlogService {
    @Autowired
    private BlogDb blogDb;
    
    public String createNewPost(String title, String content, String currentUserId) {
        return blogDb.saveOrUpdate(new BlogPost(title, content, currentUserId));
    }
    
    public String createNewComment(String content, String ownerId, String blogPostId, String parentCommentId) {
        return blogDb.saveOrUpdate(new PostComment(content, ownerId, blogPostId, parentCommentId));
    }
}
