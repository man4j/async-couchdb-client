package com.equiron.acc;

import java.util.function.Function;

import com.equiron.acc.exception.YnsBulkGetRuntimeException;
import com.equiron.acc.exception.YnsBulkRuntimeException;
import com.equiron.acc.exception.YnsResponseException;
import com.equiron.acc.exception.YnsTransformResultException;
import com.equiron.acc.exception.YnsUnmarshallException;
import com.equiron.acc.exception.http.YnsBadContentTypeException;
import com.equiron.acc.exception.http.YnsBadRequestException;
import com.equiron.acc.exception.http.YnsConflictException;
import com.equiron.acc.exception.http.YnsExpectationFailedException;
import com.equiron.acc.exception.http.YnsForbiddenException;
import com.equiron.acc.exception.http.YnsInternalServerErrorException;
import com.equiron.acc.exception.http.YnsNotAcceptableException;
import com.equiron.acc.exception.http.YnsNotAllowedException;
import com.equiron.acc.exception.http.YnsNotFoundException;
import com.equiron.acc.exception.http.YnsPreconditionFailedException;
import com.equiron.acc.exception.http.YnsRequestedRangeException;
import com.equiron.acc.exception.http.YnsUnauthorizedException;
import com.equiron.acc.json.resultset.YnsAbstractResultSet;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.profiler.YnsOperationStats;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class YnsResponseHandler<F, T> {
    private JavaType javaType;

    private Function<F, T> transformer;
    
    private ObjectMapper mapper;

    private HttpClientProviderResponse response;
    
    private OperationInfo opInfo;
    
    private YnsOperationStats ynsOperationStats;

    public YnsResponseHandler(HttpClientProviderResponse response, TypeReference<F> typeReference, Function<F, T> transformer, ObjectMapper mapper, OperationInfo opInfo, YnsOperationStats ynsOperationStats) {
        this(response, TypeFactory.defaultInstance().constructType(typeReference), transformer, mapper, opInfo, ynsOperationStats);
    }

    public YnsResponseHandler(HttpClientProviderResponse response, JavaType javaType, Function<F, T> transformer, ObjectMapper mapper, OperationInfo opInfo, YnsOperationStats ynsOperationStats) {
        this.response = response;
        this.javaType = javaType;
        this.transformer = transformer;
        this.mapper = mapper;
        this.opInfo = opInfo;
        this.ynsOperationStats = ynsOperationStats;
    }
    
    public T transform() {
        try {
            YnsHttpResponse ynsHttpResponse;
    
            int statusCode = response.getStatus();
            String body = response.getBody();
            
            if (opInfo != null) {
                opInfo.setStatus(statusCode);
            }
            
            ynsHttpResponse = new YnsHttpResponse(statusCode, "", body, response.getUri().toString());
            
            if (statusCode == 404 && !response.getUri().contains("_view")) {//this is not query request.
                return transformResult(null, ynsHttpResponse);
            }
    
            if (statusCode >= 400) {
                throw responseCode2Exception(ynsHttpResponse);
            }
    
            F couchDbResult = parseHttpResponse(ynsHttpResponse);
            
            if (opInfo != null && opInfo.getOperationType() == OperationType.QUERY) {
                opInfo.setSize(body.length());
            
                if (couchDbResult instanceof YnsAbstractResultSet) {
                    YnsAbstractResultSet<?,?,?> rs = (YnsAbstractResultSet<?,?,?>) couchDbResult;
                    
                    opInfo.setDocsCount(rs.getRows().size());
                }
            }
            
            if (opInfo != null && (opInfo.getOperationType() == OperationType.GET || opInfo.getOperationType() == OperationType.GET_WITH_ATTACHMENT)) {
                opInfo.setSize(body.length());
            
                if (couchDbResult instanceof YnsAbstractResultSet) {
                    YnsAbstractResultSet<?,?,?> rs = (YnsAbstractResultSet<?,?,?>) couchDbResult;
                    
                    opInfo.setDocsCount(rs.getRows().size());
                }
            }
            
            try {
                return transformResult(couchDbResult, ynsHttpResponse);
            } catch (YnsResponseException e) {
                opInfo.setStatus(e.getStatus());
                throw e;
            }
        } finally {
            if (opInfo != null) {
                ynsOperationStats.addOperation(opInfo);
            }
        }
    }

    private <R> R parseHttpResponse(YnsHttpResponse couchDbHttpResponse) {
        try {
            return mapper.readValue(couchDbHttpResponse.getResponseBody(), javaType);
        } catch (Exception e) {
            throw new YnsUnmarshallException(couchDbHttpResponse, e);
        }
    }

    private T transformResult(F couchDbResult, YnsHttpResponse couchDbHttpResponse) {
        try {
            return transformer.apply(couchDbResult);
        } catch (YnsResponseException e) {
            throw e;
        } catch (YnsBulkRuntimeException e) {
            throw e;
        } catch (YnsBulkGetRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new YnsTransformResultException(couchDbHttpResponse, e);
        }
    }

    public static YnsResponseException responseCode2Exception(YnsHttpResponse response) {
        switch (response.getStatusCode()) {
            case 400: return new YnsBadRequestException(response);
            case 401: return new YnsUnauthorizedException(response);
            case 403: return new YnsForbiddenException(response);
            case 404: return new YnsNotFoundException(response);
            case 405: return new YnsNotAllowedException(response);
            case 406: return new YnsNotAcceptableException(response);
            case 409: return new YnsConflictException(response);
            case 412: return new YnsPreconditionFailedException(response);
            case 415: return new YnsBadContentTypeException(response);
            case 416: return new YnsRequestedRangeException(response);
            case 417: return new YnsExpectationFailedException(response);
            case 500: return new YnsInternalServerErrorException(response);
             default: return new YnsResponseException(response, response.getStatusCode());
        }
    }
}
