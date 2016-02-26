package com.n1global.acc;

import java.io.IOException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.n1global.acc.exception.CouchDbResponseException;
import com.n1global.acc.exception.CouchDbTransformResultException;
import com.n1global.acc.exception.CouchDbUnmarshallException;
import com.n1global.acc.exception.http.CouchDbBadContentTypeException;
import com.n1global.acc.exception.http.CouchDbBadRequestException;
import com.n1global.acc.exception.http.CouchDbConflictException;
import com.n1global.acc.exception.http.CouchDbExpectationFailedException;
import com.n1global.acc.exception.http.CouchDbForbiddenException;
import com.n1global.acc.exception.http.CouchDbInternalServerErrorException;
import com.n1global.acc.exception.http.CouchDbNotAcceptableException;
import com.n1global.acc.exception.http.CouchDbNotAllowedException;
import com.n1global.acc.exception.http.CouchDbNotFoundException;
import com.n1global.acc.exception.http.CouchDbPreconditionFailedException;
import com.n1global.acc.exception.http.CouchDbRequestedRangeException;
import com.n1global.acc.exception.http.CouchDbUnauthorizedException;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class CouchDbAsyncHandler<F, T> extends AsyncCompletionHandler<T> {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    private JavaType javaType;

    private Function<F, T> transformer;

    private ObjectMapper mapper;

    private long startTime;

    public CouchDbAsyncHandler(TypeReference<F> typeReference, Function<F, T> transformer, ObjectMapper mapper) {
        this(TypeFactory.defaultInstance().constructType(typeReference), transformer, mapper);
    }

    public CouchDbAsyncHandler(JavaType javaType, Function<F, T> transformer, ObjectMapper mapper) {
        this.javaType = javaType;
        this.transformer = transformer;
        this.mapper = mapper;

        startTime = System.currentTimeMillis();
    }
    
    @Override
    public T onCompleted(Response response) {
        CouchDbHttpResponse couchDbHttpResponse = null;

        String path = response.getUri().getPath();
        String statusText = response.getStatusText();
        int statusCode = response.getStatusCode();
        String uri = response.getUri().toString();

        try {
            String body = response.getResponseBody("UTF-8");
            
            couchDbHttpResponse = new CouchDbHttpResponse(statusCode, statusText, body, uri);
            
            logger.debug((System.currentTimeMillis() - startTime) + " ms, " + couchDbHttpResponse.toString() + "\n");
        } catch (IOException e) {
            throw new CouchDbResponseException(new CouchDbHttpResponse(statusCode, statusText, "Unable to get response: " + e.getMessage(), uri));
        }
 
        if (response.getStatusCode() == 404 && !path.contains("_view")) {//this is not query request.
            return transformResult(null, couchDbHttpResponse);
        }

        if (response.getStatusCode() >= 400) {
            throw responseCode2Exception(couchDbHttpResponse);
        }

        F couchDbResult = parseHttpResponse(couchDbHttpResponse, javaType);

        return transformResult(couchDbResult, couchDbHttpResponse);
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
        } catch (Exception e) {
            throw new CouchDbTransformResultException(couchDbHttpResponse, e);
        }
    }

    private static CouchDbResponseException responseCode2Exception(CouchDbHttpResponse response) {
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
