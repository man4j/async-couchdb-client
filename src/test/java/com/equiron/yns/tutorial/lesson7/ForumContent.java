package com.equiron.yns.tutorial.lesson7;

import com.equiron.yns.json.YnsDocument;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = Message.class,   name = "Message"),
               @JsonSubTypes.Type(value = Topic.class,     name = "Topic")})
@Getter
@AllArgsConstructor
@NoArgsConstructor
abstract public class ForumContent extends YnsDocument {
    private String text;
}
