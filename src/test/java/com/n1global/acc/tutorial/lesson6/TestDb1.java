package com.n1global.acc.tutorial.lesson6;

import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.annotation.JsView;
import com.n1global.acc.view.CouchDbMapView;

public class TestDb1 extends CouchDb {
    @JsView(map = "emit(doc._id, null)")
    private CouchDbMapView<String, String> mapView;

    public TestDb1(CouchDbConfig config) {
        super(config);
    }

    public CouchDbMapView<String, String> getMapView() {
        return mapView;
    }
}
