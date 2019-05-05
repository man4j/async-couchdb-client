package com.equiron.acc.view;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbAsyncHandler;
import com.equiron.acc.CouchDbFieldAccessor;
import com.equiron.acc.json.CouchDbDesignInfo;
import com.equiron.acc.util.ExceptionHandler;
import com.equiron.acc.util.FutureUtils;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

public abstract class CouchDbAbstractView {
    private CouchDbViewAsyncOperations asyncOps = new CouchDbViewAsyncOperations();

    String designUrl;

    String viewUrl;

    CouchDb couchDb;

    JavaType keyType;

    JavaType valueType;

    public CouchDbAbstractView(CouchDb couchDb, String designName, String viewName, JavaType[] jts) {
        this.couchDb = couchDb;

        this.designUrl = new UrlBuilder(couchDb.getDbUrl()).addPathSegment("_design")
                                                           .addPathSegment(designName)
                                                           .build();

        this.viewUrl =  new UrlBuilder(designUrl).addPathSegment("_view")
                                                 .addPathSegment(viewName)
                                                 .build();

        keyType = jts[0];
        valueType = jts[1];
    }

    public class CouchDbViewAsyncOperations {
        public CompletableFuture<CouchDbDesignInfo> getInfo() {
            try {
                CouchDbFieldAccessor couchDbFieldAccessor = new CouchDbFieldAccessor(couchDb);

                return FutureUtils.toCompletable(couchDb.getConfig().getHttpClient().prepareRequest(couchDbFieldAccessor.getPrototype())
                                                        .setMethod("GET")
                                                        .setUrl(designUrl + "/_info")
                                                        .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbDesignInfo>() {/* empty */}, Function.identity(), couchDbFieldAccessor.getMapper())));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public CouchDbViewAsyncOperations async() {
        return asyncOps;
    }

    public CouchDbDesignInfo getInfo() {
        return ExceptionHandler.handleFutureResult(async().getInfo());
    }
}
