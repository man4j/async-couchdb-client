package com.equiron.acc;

import java.util.List;
import java.util.function.Function;

import com.equiron.acc.json.YnsDesignDocument;
import com.equiron.acc.json.YnsBooleanResponse;
import com.equiron.acc.json.YnsDbInfo;
import com.equiron.acc.json.YnsInstanceInfo;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.transformer.YnsBooleanResponseTransformer;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;

public class YnsAdminOperations {
    private YnsDb ynsDb;

    private HttpClientProvider httpClient;

    public YnsAdminOperations(YnsDb ynsDb) {
        this.ynsDb = ynsDb;
        this.httpClient = ynsDb.getHttpClientProvider();
    }
    
    private UrlBuilder createUrlBuilder() {
        return new UrlBuilder(ynsDb.getDbUrl());
    }
    
    public List<YnsDesignDocument> getDesignDocs() {
        return ynsDb.getBuiltInView()
                    .createQuery()
                    .startKey("_design/")
                    .endKey("_design0")
                    .asIds()
                    .stream()
                    .<YnsDesignDocument>map(ynsDb::get)
                    .filter(d -> d.getValidateDocUpdate() == null || d.getValidateDocUpdate().isBlank())
                    .toList();
    }
    
    public List<YnsDesignDocument> getDesignDocsWithValidators() {
        return ynsDb.getBuiltInView()
                    .createQuery()
                    .startKey("_design/")
                    .endKey("_design0")
                    .asIds()
                    .stream()
                    .<YnsDesignDocument>map(ynsDb::get)
                    .toList();
    }
    
    public List<String> getDatabases() {
        HttpClientProviderResponse response = httpClient.get(new UrlBuilder(ynsDb.getServerUrl()).addPathSegment("_all_dbs").build());
        
        return new YnsResponseHandler<>(response, new TypeReference<List<String>>() {/* empty */}, Function.identity(), ynsDb.mapper, null, null).transform();
    }

    public Boolean createDb() {
        HttpClientProviderResponse response = httpClient.put(createUrlBuilder().build(), "");

        return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, resp -> resp.isOk(), ynsDb.mapper, null, null).transform();
    }

    public Boolean deleteDb() {
        HttpClientProviderResponse response = httpClient.delete(createUrlBuilder().build());

        return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, new YnsBooleanResponseTransformer(), ynsDb.mapper, null, null).transform();
    }

    public YnsDbInfo getInfo() {
        HttpClientProviderResponse response = httpClient.get(createUrlBuilder().build());

        return new YnsResponseHandler<>(response, new TypeReference<YnsDbInfo>() {/* empty */}, Function.identity(), ynsDb.mapper, null, null).transform();
    }
    
    public YnsInstanceInfo getInstanceInfo() {
        HttpClientProviderResponse response = httpClient.get(ynsDb.getServerUrl());

        return new YnsResponseHandler<>(response, new TypeReference<YnsInstanceInfo>() {/* empty */}, Function.identity(), ynsDb.mapper, null, null).transform();
    }

    public Boolean cleanupViews() {
        HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_view_cleanup").build(), "");
        
        return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, new YnsBooleanResponseTransformer(), ynsDb.mapper, null, null).transform();
    }
}
