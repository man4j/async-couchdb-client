package com.equiron.acc.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.equiron.acc.YnsDocumentOperations;
import com.equiron.acc.json.YnsBulkGetResponse;
import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.core.type.TypeReference;
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
    public <T> List<T> get(List<String> docIds, boolean attachments, TypeReference<YnsBulkGetResponse<T>> listOfDocs, boolean raw) {
        docIds.stream().sorted().forEach(id -> readWriteRecordStripedLock.get(id).readLock().lock());
        
        try {
            List<String> nonExistingDocs = docIds.stream().filter(id -> !documentCache.containsKey(id)).toList();
            
            List<T> resultDocs = new ArrayList<>();

            if (!nonExistingDocs.isEmpty()) {
                nonExistingDocs.stream().sorted().forEach(id -> initRecordStripedLock.get(id).lock());
                
                try {
                    List<String> nonExistingDocs2 = docIds.stream().filter(id -> !documentCache.containsKey(id)).toList();
                    
                    if (!nonExistingDocs2.isEmpty()) {
                        for (T doc : delegate.get(nonExistingDocs2, attachments, listOfDocs, raw)) {
                            if (!raw) {
                                String docId = ((YnsDocument)doc).getDocId();
                                documentCache.put(docId, new CacheRecord(docId, (YnsDocument)doc));
                            } else {
                                String docId = (String)((Map<String, Object>)doc).get("_id");
                                documentCacheRaw.put(docId, new CacheRecordRaw(docId, (Map<String, Object>)doc));
                            }
                        }
                        
                        nonExistingDocs2.stream().filter(id -> !documentCache.containsKey(id)).forEach(id -> {
                            documentCache.put(id, new CacheRecord(id, null));
                            documentCacheRaw.put(id, new CacheRecordRaw(id, null));
                        });
                    }
                } finally {
                    nonExistingDocs.stream().sorted().forEach(id -> initRecordStripedLock.get(id).unlock());
                }
            }
                
            for (String id : docIds) {
                if (raw) {
                    if (documentCacheRaw.get(id).getDoc() != null) {
                        resultDocs.add((T) documentCacheRaw.get(id).getDoc());
                    }
                } else {
                    if (documentCache.get(id).getDoc() != null) {
                        resultDocs.add((T) documentCache.get(id));
                    }
                }
            }
            
            return resultDocs;
        } finally {
            docIds.stream().sorted().forEach(id -> readWriteRecordStripedLock.get(id).readLock().unlock());
        }
    }

//    @Override
//    public <T extends YnsDocument> void saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) throws YnsBulkException {
//        T[] allDocs = ArrayUtils.insert(0, docs, doc);
//        
//        saveOrUpdate(Arrays.asList(allDocs));
//    }
//
//    @Override
//    public <T extends YnsDocument> void saveOrUpdate(List<T> docs) throws YnsBulkException {
//        docs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().lock());
//
//        try {
//            try {
//                delegate.saveOrUpdate(docs);
//
//                docs.forEach(d -> documentCache.put(d.getDocId(), new CacheRecord(d.getDocId(), d)));
//            } catch (YnsBulkException e) {
//                docs.forEach(d -> {
//                    if (d.isOk()) {
//                        documentCache.put(d.getDocId(), new CacheRecord(d.getDocId(), d));
//                    }
//                    
//                    if (d.isUnknownError()) {
//                        documentCache.remove(d.getDocId());
//                    }
//                });
//
//                throw e;
//            } catch (Exception e) {
//                docs.forEach(d -> documentCache.remove(d.getDocId()));
//                
//                throw e;
//            }
//        } finally {
//            docs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().unlock());
//        }
//    }
//    
//    //==========================================================
//
//    @Override
//    public List<YnsBulkResponse> saveOrUpdateRaw(Map<String, Object> doc, @SuppressWarnings("unchecked") Map<String, Object>... docs) throws YnsBulkException {
//        Map<String, Object>[] allDocs = ArrayUtils.insert(0, docs, doc);
//        
//        return saveOrUpdateRaw(Arrays.asList(allDocs));
//    }
//
//    @Override
//    public List<YnsBulkResponse> saveOrUpdateRaw(List<Map<String, Object>> docs) throws YnsBulkException {
//        docs.forEach(d -> readWriteRecordStripedLock.get(d.get("_id")).writeLock().lock());
//
//        try {
//            try {
//                List<YnsBulkResponse> resp = delegate.saveOrUpdateRaw(docs);
//                
//                docs.forEach(d -> documentCache.remove(d.get("_id")));
//                
//                return resp;
//            } catch (YnsBulkException e) {
//                e.getResponses().forEach(r -> {
//                    if (r.isOk() || r.isUnknownError()) {
//                        documentCache.remove(r.getDocId());
//                    }
//                });
//
//                throw e;
//            } catch (Exception e) {
//                docs.forEach(d -> documentCache.remove(d.get("_id")));
//                
//                throw e;
//            }
//        } finally {
//            docs.forEach(d -> readWriteRecordStripedLock.get(d.get("_id")).writeLock().unlock());
//        }
//    }
//
//    //==========================================================
//    
//    @Override
//    public List<YnsBulkResponse> delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) throws YnsBulkException {
//        YnsDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
//        
//        return delete(Arrays.asList(allDocs));
//    }
//    
//    @Override
//    public List<YnsBulkResponse> delete(List<YnsDocIdAndRev> docRevs) throws YnsBulkException {
//        docRevs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().lock());
//
//        try {
//            try {
//                List<YnsBulkResponse> resp = delegate.delete(docRevs);
//
//                docRevs.forEach(d -> documentCache.put(d.getDocId(), new CacheRecord(d.getDocId(), null)));
//                
//                return resp;
//            } catch (YnsBulkException e) {
//                e.getResponses().forEach(r -> {
//                    if (r.isOk()) {
//                        documentCache.put(r.getDocId(), new CacheRecord(r.getDocId(), null));
//                    }
//                    
//                    if (r.isUnknownError()) {
//                        documentCache.remove(r.getDocId());
//                    }
//                });
//
//                throw e;
//            } catch (Exception e) {
//                docRevs.forEach(d -> documentCache.remove(d.getDocId()));
//                
//                throw e;
//            }
//        } finally {
//            docRevs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().unlock());
//        }
//    }
//
//    @Override
//    public YnsBulkResponse attach(YnsDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public StreamResponse getAttachmentAsStream(String docId, String name) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public StreamResponse getAttachmentAsStream(String docId, String name, Map<String, String> headers) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Boolean deleteAttachment(YnsDocIdAndRev docIdAndRev, String name) {
//        // TODO Auto-generated method stub
//        return null;
//    }
}
