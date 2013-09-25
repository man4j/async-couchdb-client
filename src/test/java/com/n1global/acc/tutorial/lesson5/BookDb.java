package com.n1global.acc.tutorial.lesson5;

import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.annotation.JsView;
import com.n1global.acc.view.CouchDbMapReduceView;

public class BookDb extends CouchDb {
    @JsView(map = "emit(doc.publisherName, 1)", reduce = JsView.SUM)
    private CouchDbMapReduceView<String, Integer, String, Integer> publishersBooksView;

    public BookDb(CouchDbConfig config) {
        super(config);
    }

    public CouchDbMapReduceView<String, Integer, String, Integer> getPublishersBooksView() {
        return publishersBooksView;
    }
}
