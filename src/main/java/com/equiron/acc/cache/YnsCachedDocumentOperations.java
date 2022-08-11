package com.equiron.acc.cache;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.lang3.ArrayUtils;

import com.equiron.acc.YnsDocIdAndRev;
import com.equiron.acc.YnsDocumentOperationsInterface;
import com.equiron.acc.exception.YnsBulkException;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.util.StreamResponse;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;

public class YnsCachedDocumentOperations implements YnsDocumentOperationsInterface {
    private final Map<String, CacheRecord> documentCache;
    
    private final Striped<ReadWriteLock> readWriteRecordStripedLock = Striped.readWriteLock(4096);

    private final Striped<Lock> initRecordStripedLock = Striped.lock(4096);
    
    private final YnsDocumentOperationsInterface delegate;
    
    public YnsCachedDocumentOperations(YnsDocumentOperationsInterface delegate, int maxDocs, int maxTimeoutSec) {
        this.delegate = delegate;
        documentCache = CacheBuilder.newBuilder().expireAfterAccess(maxTimeoutSec, TimeUnit.SECONDS).maximumSize(maxDocs).<String, CacheRecord>build().asMap();
    }

    @Override
    public <T extends YnsDocument> T get(String docId) {
        return get(docId, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends YnsDocument> T get(String docId, boolean attachments) {
        readWriteRecordStripedLock.get(docId).readLock().lock();
        
        try {
            if (!documentCache.containsKey(docId)) {//требуется подкачка из базы
                initRecordStripedLock.get(docId).lock();
                
                try {
                    if (!documentCache.containsKey(docId)) {
                        documentCache.put(docId, new CacheRecord(docId, delegate.get(docId)));
                    }
                } finally {
                    initRecordStripedLock.get(docId).unlock();
                }
            }
            
            return (T) documentCache.get(docId).getDoc();
        } finally {
            readWriteRecordStripedLock.get(docId).readLock().unlock();
        }
    }

    @Override
    public Map<String, Object> getRaw(String docId) {
        return getRaw(docId, false);
    }

    @Override
    public Map<String, Object> getRaw(String docId, boolean attachments) {
        readWriteRecordStripedLock.get(docId).readLock().lock();
        
        try {
            return delegate.getRaw(docId);
        } finally {
            readWriteRecordStripedLock.get(docId).readLock().unlock();
        }
    }

    @Override
    public <T extends YnsDocument> void saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) throws YnsBulkException {
        T[] allDocs = ArrayUtils.insert(0, docs, doc);
        
        saveOrUpdate(Arrays.asList(allDocs));
    }

    @Override
    public <T extends YnsDocument> void saveOrUpdate(List<T> docs) throws YnsBulkException {
        docs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().lock());

        try {
            try {
                delegate.saveOrUpdate(docs);

                docs.forEach(d -> documentCache.put(d.getDocId(), new CacheRecord(d.getDocId(), d)));
            } catch (YnsBulkException e) {
                docs.forEach(d -> {
                    if (d.isOk()) {
                        documentCache.put(d.getDocId(), new CacheRecord(d.getDocId(), d));
                    }
                    
                    if (d.isUnknownError()) {
                        documentCache.remove(d.getDocId());
                    }
                });

                throw e;
            } catch (Exception e) {
                docs.forEach(d -> documentCache.remove(d.getDocId()));
                
                throw e;
            }
        } finally {
            docs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().unlock());
        }
    }
    
    //==========================================================

    @Override
    public List<YnsBulkResponse> saveOrUpdateRaw(Map<String, Object> doc, @SuppressWarnings("unchecked") Map<String, Object>... docs) throws YnsBulkException {
        Map<String, Object>[] allDocs = ArrayUtils.insert(0, docs, doc);
        
        return saveOrUpdateRaw(Arrays.asList(allDocs));
    }

    @Override
    public List<YnsBulkResponse> saveOrUpdateRaw(List<Map<String, Object>> docs) throws YnsBulkException {
        docs.forEach(d -> readWriteRecordStripedLock.get(d.get("_id")).writeLock().lock());

        try {
            try {
                List<YnsBulkResponse> resp = delegate.saveOrUpdateRaw(docs);
                
                docs.forEach(d -> documentCache.remove(d.get("_id")));
                
                return resp;
            } catch (YnsBulkException e) {
                e.getResponses().forEach(r -> {
                    if (r.isOk() || r.isUnknownError()) {
                        documentCache.remove(r.getDocId());
                    }
                });

                throw e;
            } catch (Exception e) {
                docs.forEach(d -> documentCache.remove(d.get("_id")));
                
                throw e;
            }
        } finally {
            docs.forEach(d -> readWriteRecordStripedLock.get(d.get("_id")).writeLock().unlock());
        }
    }

    //==========================================================
    
    @Override
    public List<YnsBulkResponse> delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) throws YnsBulkException {
        YnsDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return delete(Arrays.asList(allDocs));
    }
    
    @Override
    public List<YnsBulkResponse> delete(List<YnsDocIdAndRev> docRevs) throws YnsBulkException {
        docRevs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().lock());

        try {
            try {
                List<YnsBulkResponse> resp = delegate.delete(docRevs);

                docRevs.forEach(d -> documentCache.put(d.getDocId(), new CacheRecord(d.getDocId(), null)));
                
                return resp;
            } catch (YnsBulkException e) {
                e.getResponses().forEach(r -> {
                    if (r.isOk()) {
                        documentCache.put(r.getDocId(), new CacheRecord(r.getDocId(), null));
                    }
                    
                    if (r.isUnknownError()) {
                        documentCache.remove(r.getDocId());
                    }
                });

                throw e;
            } catch (Exception e) {
                docRevs.forEach(d -> documentCache.remove(d.getDocId()));
                
                throw e;
            }
        } finally {
            docRevs.forEach(d -> readWriteRecordStripedLock.get(d.getDocId()).writeLock().unlock());
        }
    }

    @Override
    public YnsBulkResponse attach(YnsDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StreamResponse getAttachmentAsStream(String docId, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StreamResponse getAttachmentAsStream(String docId, String name, Map<String, String> headers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean deleteAttachment(YnsDocIdAndRev docIdAndRev, String name) {
        // TODO Auto-generated method stub
        return null;
    }
}
