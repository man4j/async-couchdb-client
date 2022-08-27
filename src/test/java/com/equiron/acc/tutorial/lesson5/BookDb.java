package com.equiron.acc.tutorial.lesson5;

import com.equiron.acc.YnsDb;
import com.equiron.acc.YnsDbConfig;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.json.YnsNullObject;
import com.equiron.acc.view.YnsMapReduceView;

import lombok.Getter;

@Getter
public class BookDb extends YnsDb {
    @YnsJsView(map = "emit(doc.publisherName, null)", reduce = YnsJsView.COUNT)
    private YnsMapReduceView<String, YnsNullObject, String, Integer> publishersBooksView;

    public BookDb(YnsDbConfig config) {
        super(config);
    }
}
