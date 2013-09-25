package com.n1global.acc.view;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.query.CouchDbMapQuery;
import com.n1global.acc.query.CouchDbMapQueryWithDocs;
import com.n1global.acc.query.CouchDbReduceQuery;

public class CouchDbMapReduceView<MapK, MapV, ReduceK, ReduceV> {
    private CouchDbMapView<MapK, MapV> mapView;

    private CouchDbReduceView<ReduceK, ReduceV> reducedView;

    public CouchDbMapReduceView(CouchDb couchDb, String designName, String viewName, JavaType[] jts) {
        mapView = new CouchDbMapView<>(couchDb, designName, viewName, new JavaType[] {jts[0], jts[1]});

        reducedView = new CouchDbReduceView<>(couchDb, designName, viewName, new JavaType[] {jts[2], jts[3]});
    }

    public CouchDbMapQuery<MapK, MapV> createMapQuery() {
        return mapView.createQuery();
    }

    public CouchDbReduceQuery<ReduceK, ReduceV> createReduceQuery() {
        return reducedView.createQuery();
    }

    public <T extends CouchDbDocument> CouchDbMapQueryWithDocs<MapK, MapV, T> createDocQuery() {
        return mapView.createDocQuery();
    }

    public CouchDbMapQueryWithDocs<MapK, MapV, Map<String, Object>> createRawDocQuery() {
        return mapView.createRawDocQuery();
    }
}
