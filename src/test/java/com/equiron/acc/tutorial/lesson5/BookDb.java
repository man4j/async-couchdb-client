package com.equiron.acc.tutorial.lesson5;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.view.YnsMapReduceView;

public class BookDb extends CouchDb {
    @YnsJsView(map = "emit(doc.publisherName, 1)", reduce = YnsJsView.SUM)
    private YnsMapReduceView<String, Integer, String, Integer> publishersBooksView;

    public BookDb(CouchDbConfig config) {
        super(config);
    }

    public YnsMapReduceView<String, Integer, String, Integer> getPublishersBooksView() {
        return publishersBooksView;
    }
}
