package com.equiron.acc.tutorial.lesson3;

import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = Author.class,       name = "Author"),
               @JsonSubTypes.Type(value = BlogPost.class,     name = "BlogPost"),
               @JsonSubTypes.Type(value = BlogComment.class,  name = "BlogComment")})
public class BlogDocument extends YnsDocument {
    private long createdAt = System.currentTimeMillis();
}
