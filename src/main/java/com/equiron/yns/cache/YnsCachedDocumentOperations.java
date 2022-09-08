package com.equiron.yns.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.equiron.yns.YnsDocIdAndRev;
import com.equiron.yns.YnsDocumentOperations;
import com.equiron.yns.exception.YnsBulkDocumentException;
import com.equiron.yns.json.YnsBulkResponse;
import com.equiron.yns.json.YnsDocument;
import com.equiron.yns.profiler.OperationType;
import com.fasterxml.jackson.databind.JavaType;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;

import lombok.SneakyThrows;

public class YnsCachedDocumentOperations extends YnsDocumentOperations {
    private final Map<String, CacheRecord> documentCache;
    
    private final Map<String, CacheRecordRaw> documentCacheRaw;
    
    private final Striped<ReadWriteLock> readWriteRecordStripedLock = Striped.readWriteLock(4096);

    private final Striped<Lock> initRecordStripedLock = Striped.lock(4096);
    
    private final YnsDocumentOperations delegate;
    
    @SuppressWarnings("resource")
    public YnsCachedDocumentOperations(YnsDocumentOperations delegate, int maxDocs, int maxTimeoutSec) {
        super(delegate.getYnsDb());
        this.delegate = delegate;
        documentCache = CacheBuilder.newBuilder().expireAfterAccess(maxTimeoutSec, TimeUnit.SECONDS).maximumSize(maxDocs).<String, CacheRecord>build().asMap();
        documentCacheRaw = CacheBuilder.newBuilder().expireAfterAccess(maxTimeoutSec, TimeUnit.SECONDS).maximumSize(maxDocs).<String, CacheRecordRaw>build().asMap();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> List<T> get(List<YnsDocIdAndRev> docIds, JavaType responseType, boolean raw) {
        List<String> ids = docIds.stream().map(YnsDocIdAndRev::getDocId).sorted().toList();
          
        ids.forEach(id -> readWriteRecordStripedLock.get(id).readLock().lock());
        
        try {
            List<String> nonExistingDocs = docIds.stream().map(YnsDocIdAndRev::getDocId).filter(id -> !documentCache.containsKey(id)).toList();
            
            List<T> resultDocs = new ArrayList<>();

            if (!nonExistingDocs.isEmpty()) {
                nonExistingDocs.stream().sorted().forEach(id -> initRecordStripedLock.get(id).lock());
                
                try {
                    List<YnsDocIdAndRev> nonExistingDocs2 = docIds.stream().filter(docAndRev -> !documentCache.containsKey(docAndRev.getDocId())).toList();
                    
                    if (!nonExistingDocs2.isEmpty()) {
                        for (Object doc : delegate.get(nonExistingDocs2, responseType, raw)) {
                            if (!raw) {
                                String docId = ((YnsDocument)doc).getDocId();
                                documentCache.put(docId, new CacheRecord(docId, (YnsDocument)doc));
                            } else {
                                String docId = (String)((Map<String, Object>)doc).get("_id");
                                documentCacheRaw.put(docId, new CacheRecordRaw(docId, (Map<String, Object>)doc));
                            }
                        }
                        
                        nonExistingDocs2.stream().map(YnsDocIdAndRev::getDocId).filter(id -> !documentCache.containsKey(id)).forEach(id -> {
                            documentCache.put(id, new CacheRecord(id, null));
                            documentCacheRaw.put(id, new CacheRecordRaw(id, null));
                        });
                    }
                } finally {
                    nonExistingDocs.stream().sorted().forEach(id -> initRecordStripedLock.get(id).unlock());
                }
            }
                
            for (String id : docIds.stream().map(YnsDocIdAndRev::getDocId).toList()) {
                if (raw) {
                    if (documentCacheRaw.get(id).getDoc() != null) {
                        resultDocs.add((T) documentCacheRaw.get(id).getDoc());
                    }
                } else {
                    if (documentCache.get(id).getDoc() != null) {
                        resultDocs.add((T) documentCache.get(id).getDoc());
                    }
                }
            }
            
            return resultDocs;
        } finally {
            ids.forEach(id -> readWriteRecordStripedLock.get(id).readLock().unlock());
        }
    }
    
    @SuppressWarnings({ "unchecked" })
    @SneakyThrows
    @Override
    public <T> void saveOrUpdate(List<T> docs, OperationType opType, boolean raw) {
        List<String> ids;
        
        if (raw) {
            ids = docs.stream().map(d -> (String)((Map<String, Object>)d).get("_id")).filter(Objects::nonNull).sorted().toList();
        } else {
            ids = docs.stream().map(d -> ((YnsDocument)d).getDocId()).filter(Objects::nonNull).sorted().toList();
        }
        
        ids.forEach(id -> {
            readWriteRecordStripedLock.get(id).writeLock().lock();
        });
        
        try {
            try {
                delegate.saveOrUpdate(docs, opType, raw);
                
                docs.forEach(d -> processDoc(raw, d));
            } catch (YnsBulkDocumentException e) {
                for (int i = 0; i < e.getResponses().size(); i++) {
                    YnsBulkResponse resp = e.getResponses().get(i);
                    
                    if (resp.isOk()) {
                        processDoc(raw, docs.get(i));
                    }
                    
                    if (resp.isUnknownError()) {
                        documentCache.remove(resp.getDocId());
                        documentCacheRaw.remove(resp.getDocId());
                    }
                }
    
                throw e;
            } catch (Exception e) {
                ids.forEach(id -> documentCache.remove(id));
                ids.forEach(id -> documentCacheRaw.remove(id));
              
                throw e;
            }
        } finally {
            ids.forEach(id -> {
                readWriteRecordStripedLock.get(id).writeLock().unlock();
            });
        }
    }

    @SuppressWarnings({ "unchecked" })
    private <T> void processDoc(boolean raw, T doc) {
        String docId;
        
        if (raw) {
            docId = (String)((Map<String, Object>)doc).get("_id");
            Boolean deleted = (Boolean)((Map<String, Object>)doc).get("_deleted");
            
            if (deleted != null && deleted) {
                documentCache.put(docId, new CacheRecord(docId, null));
                documentCacheRaw.put(docId, new CacheRecordRaw(docId, null));
            } else {
                documentCacheRaw.put(docId, new CacheRecordRaw(docId, (Map<String, Object>)doc));
                documentCache.remove(docId);
            }
        } else {
            docId = ((YnsDocument)doc).getDocId();
            boolean deleted = ((YnsDocument)doc).isDeleted();

            if (deleted) {
                documentCache.put(docId, new CacheRecord(docId, null));
                documentCacheRaw.put(docId, new CacheRecordRaw(docId, null));
            } else {
                documentCache.put(docId, new CacheRecord(docId, (YnsDocument)doc));
                documentCacheRaw.remove(docId);
            }
        }
    }
}
