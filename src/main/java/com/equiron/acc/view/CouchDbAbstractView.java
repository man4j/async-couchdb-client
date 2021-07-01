package com.equiron.acc.view;

import java.util.function.Function;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbResponseHandler;
import com.equiron.acc.json.CouchDbDesignInfo;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

public abstract class CouchDbAbstractView implements CouchDbView {
    private final String designUrl;

    final String viewUrl;

    final CouchDb couchDb;

    final JavaType keyType;

    final JavaType valueType;
    
    final String viewName;
    
    final String designName;

    public CouchDbAbstractView(CouchDb couchDb, String designName, String viewName, JavaType[] jts) {
        this.viewName = viewName;
        this.designName = designName;
        this.couchDb = couchDb;

        this.designUrl = new UrlBuilder(couchDb.getDbUrl()).addPathSegment("_design")
                                                           .addPathSegment(designName)
                                                           .build();

        this.viewUrl =  new UrlBuilder(designUrl).addPathSegment("_view")
                                                 .addPathSegment(viewName)
                                                 .build();

        this.keyType = jts[0];
        this.valueType = jts[1];
    }

    public CouchDbDesignInfo getInfo() {
        HttpClientProviderResponse response = couchDb.getHttpClientProvider().get(designUrl + "/_info");

        return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbDesignInfo>() {/* empty */}, Function.identity(), couchDb.getMapper(), null, null).transform();
    }

    @Override
    public String getDesignName() {
        return designName;
    }
    
    @Override
    public String getViewName() {
        return viewName;
    }
}
