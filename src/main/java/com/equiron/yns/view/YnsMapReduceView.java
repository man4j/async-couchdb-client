package com.equiron.yns.view;

import java.util.Collections;

import com.equiron.yns.YnsDb;
import com.equiron.yns.query.YnsMapQuery;
import com.equiron.yns.query.YnsReduceQuery;
import com.fasterxml.jackson.databind.JavaType;

public class YnsMapReduceView<MapK, MapV, ReduceK, ReduceV> implements YnsView {
    private final YnsMapView<MapK, MapV> mapView;

    private final YnsReduceView<ReduceK, ReduceV> reducedView;

    public YnsMapReduceView(YnsDb ynsDb, String designName, String viewName, JavaType[] jts) {
        mapView = new YnsMapView<>(ynsDb, designName, viewName, new JavaType[] {jts[0], jts[1]});
        reducedView = new YnsReduceView<>(ynsDb, designName, viewName, new JavaType[] {jts[2], jts[3]});
    }

    public YnsMapQuery<MapK, MapV> createMapQuery() {
        return mapView.createQuery();
    }

    public YnsReduceQuery<ReduceK, ReduceV> createReduceQuery() {
        return reducedView.createQuery();
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
