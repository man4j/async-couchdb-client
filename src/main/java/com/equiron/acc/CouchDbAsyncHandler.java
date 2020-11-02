package com.equiron.acc;

import java.net.http.HttpResponse;
import java.util.function.Function;

import com.equiron.acc.exception.CouchDbResponseException;
import com.equiron.acc.exception.CouchDbTransformResultException;
import com.equiron.acc.exception.CouchDbUnmarshallException;
import com.equiron.acc.exception.http.CouchDbBadContentTypeException;
import com.equiron.acc.exception.http.CouchDbBadRequestException;
import com.equiron.acc.exception.http.CouchDbConflictException;
import com.equiron.acc.exception.http.CouchDbExpectationFailedException;
import com.equiron.acc.exception.http.CouchDbForbiddenException;
import com.equiron.acc.exception.http.CouchDbInternalServerErrorException;
import com.equiron.acc.exception.http.CouchDbNotAcceptableException;
import com.equiron.acc.exception.http.CouchDbNotAllowedException;
import com.equiron.acc.exception.http.CouchDbNotFoundException;
import com.equiron.acc.exception.http.CouchDbPreconditionFailedException;
import com.equiron.acc.exception.http.CouchDbRequestedRangeException;
import com.equiron.acc.exception.http.CouchDbUnauthorizedException;
import com.equiron.acc.json.resultset.CouchDbAbstractResultSet;
import com.equiron.acc.profiler.CouchDbOperationStats;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class CouchDbAsyncHandler<F, T> {
    private JavaType javaType;

    private Function<F, T> transformer;
    
    private ObjectMapper mapper;

    private HttpResponse<String> response;
    
    private OperationInfo opInfo;
    
    private CouchDbOperationStats couchDbOperationStats;

    public CouchDbAsyncHandler(HttpResponse<String> response, TypeReference<F> typeReference, Function<F, T> transformer, ObjectMapper mapper, OperationInfo opInfo, CouchDbOperationStats couchDbOperationStats) {
        this(response, TypeFactory.defaultInstance().constructType(typeReference), transformer, mapper, opInfo, couchDbOperationStats);
    }

    public CouchDbAsyncHandler(HttpResponse<String> response, JavaType javaType, Function<F, T> transformer, ObjectMapper mapper, OperationInfo opInfo, CouchDbOperationStats couchDbOperationStats) {
        this.response = response;
        this.javaType = javaType;
        this.transformer = transformer;
        this.mapper = mapper;
        this.opInfo = opInfo;
        this.couchDbOperationStats = couchDbOperationStats;
    }
    
    public T transform() {
        try {
            CouchDbHttpResponse couchDbHttpResponse;
    
            String statusText = response.statusCode() + "";
            int statusCode = response.statusCode();
            String body = response.body();
            
            if (opInfo != null) {
                opInfo.setStatus(statusCode);
            }
            
            couchDbHttpResponse = new CouchDbHttpResponse(statusCode, statusText, body, response.uri().toString());
            
            if (statusCode == 404 && !response.uri().getPath().contains("_view")) {//this is not query request.
                return transformResult(null, couchDbHttpResponse);
            }
    
            if (statusCode >= 400) {
                throw responseCode2Exception(couchDbHttpResponse);
            }
    
            F couchDbResult = parseHttpResponse(couchDbHttpResponse, javaType);
            
            if (opInfo != null && opInfo.getOperationType() == OperationType.QUERY) {
                opInfo.setSize(body.length());
            
                if (couchDbResult instanceof CouchDbAbstractResultSet) {
                    CouchDbAbstractResultSet<?,?,?> rs = (CouchDbAbstractResultSet<?,?,?>) couchDbResult;
                    
                    opInfo.setDocsCount(rs.getRows().size());
                }
            }
            
            try {
                return transformResult(couchDbResult, couchDbHttpResponse);
            } catch (CouchDbConflictException e) {
                if (opInfo != null) {
                    opInfo.setStatus(409);
                }
                throw e;
            } catch (CouchDbForbiddenException e) {
                if (opInfo != null) {
                    opInfo.setStatus(403);
                }
                throw e;
            }
            
        } finally {
            if (opInfo != null) {
                couchDbOperationStats.addOperation(opInfo);
            }
        }
    }

    private <R> R parseHttpResponse(CouchDbHttpResponse couchDbHttpResponse, JavaType javaType) {
        try {
            return mapper.readValue(couchDbHttpResponse.getResponseBody(), javaType);
        } catch (Exception e) {
            throw new CouchDbUnmarshallException(couchDbHttpResponse, e);
        }
    }

    private T transformResult(F couchDbResult, CouchDbHttpResponse couchDbHttpResponse) {
        try {
            return transformer.apply(couchDbResult);
        } catch (CouchDbResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchDbTransformResultException(couchDbHttpResponse, e);
        }
    }

    public static CouchDbResponseException responseCode2Exception(CouchDbHttpResponse response) {
        switch (response.getStatusCode()) {
            case 400: return new CouchDbBadRequestException(response);
            case 401: return new CouchDbUnauthorizedException(response);
            case 403: return new CouchDbForbiddenException(response);
            case 404: return new CouchDbNotFoundException(response);
            case 405: return new CouchDbNotAllowedException(response);
            case 406: return new CouchDbNotAcceptableException(response);
            case 409: return new CouchDbConflictException(response);
            case 412: return new CouchDbPreconditionFailedException(response);
            case 415: return new CouchDbBadContentTypeException(response);
            case 416: return new CouchDbRequestedRangeException(response);
            case 417: return new CouchDbExpectationFailedException(response);
            case 500: return new CouchDbInternalServerErrorException(response);
             default: return new CouchDbResponseException(response);
        }
    }
}
