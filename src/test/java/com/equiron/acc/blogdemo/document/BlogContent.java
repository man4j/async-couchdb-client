package com.equiron.acc.blogdemo.document;

import com.equiron.acc.json.YnsDocument;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class BlogContent extends YnsDocument {
    @NonNull
    private String content;
    
    private long createdAt = System.currentTimeMillis();
    
    @NonNull
    private String ownerId;
}
