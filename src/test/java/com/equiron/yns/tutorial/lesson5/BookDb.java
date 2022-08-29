package com.equiron.yns.tutorial.lesson5;

import com.equiron.yns.YnsDb;
import com.equiron.yns.YnsDbConfig;
import com.equiron.yns.annotation.YnsJsView;
import com.equiron.yns.json.YnsNullObject;
import com.equiron.yns.view.YnsMapReduceView;

import lombok.Getter;

@Getter
public class BookDb extends YnsDb {
    @YnsJsView(map = "emit(doc.publisherName, null)", reduce = YnsJsView.COUNT)
    private YnsMapReduceView<String, YnsNullObject, String, Integer> publishersBooksView;

    public BookDb(YnsDbConfig config) {
        super(config);
    }
}
