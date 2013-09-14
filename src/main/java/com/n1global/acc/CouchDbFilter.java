package com.n1global.acc;

public class CouchDbFilter {
    private String designName;

    private String filterName;

    public CouchDbFilter(String designName, String filterName) {
        this.designName = designName;
        this.filterName = filterName;
    }

    public String getDesignName() {
        return designName;
    }

    public String getFilterName() {
        return filterName;
    }
}
