package com.equiron.yns.tutorial.lesson2;

import com.equiron.yns.json.YnsDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class City extends YnsDocument {
    private String name;
}