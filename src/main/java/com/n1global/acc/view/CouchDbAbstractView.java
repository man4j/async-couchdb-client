package com.n1global.acc.view;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbAsyncHandler;
import com.n1global.acc.CouchDbFieldAccessor;
import com.n1global.acc.json.CouchDbDesignInfo;
import com.n1global.acc.util.ExceptionHandler;
import com.n1global.acc.util.FutureUtils;
import com.n1global.acc.util.NoopFunction;
import com.n1global.acc.util.UrlBuilder;

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
                                                        .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbDesignInfo>() {/* empty */}, new NoopFunction<CouchDbDesignInfo>(), couchDbFieldAccessor.getMapper())));
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
