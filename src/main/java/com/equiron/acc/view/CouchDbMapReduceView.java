package com.equiron.acc.view;

import java.util.Collections;
import java.util.Map;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.query.CouchDbMapQuery;
import com.equiron.acc.query.CouchDbMapQueryWithDocs;
import com.equiron.acc.query.CouchDbReduceQuery;
import com.fasterxml.jackson.databind.JavaType;

public class CouchDbMapReduceView<MapK, MapV, ReduceK, ReduceV> implements CouchDbView {
    private final CouchDbMapView<MapK, MapV> mapView;

    private final CouchDbReduceView<ReduceK, ReduceV> reducedView;

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

    @Override
    public void update() {
        createMapQuery().byKeys(Collections.emptyList()).asId();
    }

    @Override
    public String getDesignName() {
        return mapView.getDesignName();
    }

    @Override
    public String getViewName() {
        return mapView.getViewName();
    }
}
