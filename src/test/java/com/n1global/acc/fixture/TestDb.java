package com.n1global.acc.fixture;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.annotation.JsView;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;
import com.equiron.acc.view.CouchDbMapView;
import com.equiron.acc.view.CouchDbReduceView;

@Security(admins = @SecurityPattern(names = "admin"))
public class TestDb extends CouchDb {
    @JsView(map = "emit(doc._id, doc.name)")
    private CouchDbMapView<String, String> testView;

    @JsView(map = "emit(doc._id, 1)", reduce = "return sum(values)")
    private CouchDbReduceView<String, Integer> reducedTestView;

    public TestDb(CouchDbConfig config) {
        super(config);
    }

    public CouchDbMapView<String, String> getTestView() {
        return testView;
    }

    public CouchDbReduceView<String, Integer> getReducedTestView() {
        return reducedTestView;
    }
}
