package com.equiron.yns.view;

import java.util.function.Function;

import com.equiron.yns.YnsDb;
import com.equiron.yns.YnsResponseHandler;
import com.equiron.yns.json.YnsDesignInfo;
import com.equiron.yns.provider.HttpClientProviderResponse;
import com.equiron.yns.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import lombok.Getter;

public abstract class YnsAbstractView implements YnsView {
    private final String designUrl;

    final String viewUrl;

    final YnsDb ynsDb;

    final JavaType keyType;

    final JavaType valueType;
    
    @Getter
    final String viewName;
    
    @Getter
    final String designName;

    public YnsAbstractView(YnsDb ynsDb, String designName, String viewName, JavaType[] jts) {
        this.viewName = viewName;
        this.designName = designName;
        this.ynsDb = ynsDb;

        this.designUrl = new UrlBuilder(ynsDb.getDbUrl()).addPathSegment("_design")
                                                         .addPathSegment(designName)
                                                         .build();

        this.viewUrl =  new UrlBuilder(designUrl).addPathSegment("_view")
                                                 .addPathSegment(viewName)
                                                 .build();

        this.keyType = jts[0];
        this.valueType = jts[1];
    }

    public YnsDesignInfo getInfo() {
        HttpClientProviderResponse response = ynsDb.getHttpClientProvider().get(designUrl + "/_info");

        return new YnsResponseHandler<>(response, new TypeReference<YnsDesignInfo>() {/* empty */}, Function.identity(), ynsDb.getMapper(), null, null).transform();
    }
}
