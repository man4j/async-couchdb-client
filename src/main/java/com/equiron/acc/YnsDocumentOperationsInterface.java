package com.equiron.acc;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.equiron.acc.exception.YnsBulkException;
import com.equiron.acc.exception.YnsBulkGetException;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.util.StreamResponse;

public interface YnsDocumentOperationsInterface {
    //------------------ Fetch API -------------------------
    <T extends YnsDocument> List<T> get(String docId, String... docIds) throws YnsBulkGetException;
    
    <T extends YnsDocument> List<T> get(List<String> docIds) throws YnsBulkGetException;

    <T extends YnsDocument> List<T> get(List<String> docIds, boolean attachments) throws YnsBulkGetException;
    
    //------------------ Fetch RAW API -------------------------

    List<Map<String, Object>> getRaw(String docId, String... docIds) throws YnsBulkGetException;
    
    List<Map<String, Object>> getRaw(List<String> docIds) throws YnsBulkGetException;

    List<Map<String, Object>> getRaw(List<String> docIds, boolean attachments) throws YnsBulkGetException;

    //------------------ Save or update API -------------------------

    <T extends YnsDocument> void saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) throws YnsBulkException;

    <T extends YnsDocument> void saveOrUpdate(List<T> docs) throws YnsBulkException;
    
    //------------------ Save or update RAW API -------------------------

    void saveOrUpdateRaw(Map<String, Object> doc, @SuppressWarnings("unchecked") Map<String, Object>... docs) throws YnsBulkException;

    void saveOrUpdateRaw(List<Map<String, Object>> docs) throws YnsBulkException;

    //------------------ Delete API -------------------------
    
    void delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) throws YnsBulkException;

    void delete(List<YnsDocIdAndRev> docRevs) throws YnsBulkException;

    //------------------ Attach API -------------------------

    YnsBulkResponse attach(YnsDocIdAndRev docIdAndRev, InputStream in, String name, String contentType);
        
    StreamResponse getAttachmentAsStream(String docId, String name);
    
    StreamResponse getAttachmentAsStream(String docId, String name, Map<String, String> headers);

    Boolean deleteAttachment(YnsDocIdAndRev docIdAndRev, String name);
}