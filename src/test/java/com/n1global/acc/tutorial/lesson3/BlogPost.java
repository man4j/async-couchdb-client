package com.n1global.acc.tutorial.lesson3;

public class BlogPost extends BlogDocument {
    private String title;

    private String body;

    private String authorId;

    public BlogPost() {
        /* empty */
    }

    public BlogPost(String title, String body, String authorId) {
        this.title = title;
        this.body = body;
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAuthorId() {
        return authorId;
    }
}
