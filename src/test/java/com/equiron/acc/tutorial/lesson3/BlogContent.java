package com.equiron.acc.tutorial.lesson3;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
abstract public class BlogContent extends BlogDocument {
    @NonNull
    private String content;
    
    private long createdAt = System.currentTimeMillis();
    
    @NonNull
    private String ownerId;
}