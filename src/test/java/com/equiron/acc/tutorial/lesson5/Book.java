package com.equiron.acc.tutorial.lesson5;

import com.equiron.acc.json.YnsDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Book extends YnsDocument {
    private String title;

    private String publisherName;
}
