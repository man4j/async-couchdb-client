package com.equiron.yns.tutorial.lesson7;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import com.equiron.yns.YnsDb;
import com.equiron.yns.changes.YnsEventListener;
import com.equiron.yns.changes.YnsSequenceStorage;
import com.equiron.yns.json.YnsEvent;

import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class Indexer extends YnsEventListener {
    private IndexWriter indexWriter;
    
    @SuppressWarnings("resource")
    @SneakyThrows
    public Indexer(YnsDb db, YnsSequenceStorage sequenceStorage) {
        super(db, sequenceStorage);
        indexWriter = new IndexWriter(FSDirectory.open(Files.createTempDirectory("tmp")), new IndexWriterConfig(new StandardAnalyzer()));
    }

    @Override
    public void onCancel() {
        try {
            indexWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEvent(YnsEvent e) throws Exception {
        if (e.isDeleted()) {
            indexWriter.deleteDocuments(new Term("uid", e.getDocId()));
        } else {
            @SuppressWarnings("resource")
            ForumContent doc = getDb().get(e.getDocId(), ForumContent.class);

            Document luceneDoc = new Document();

            luceneDoc.add(new StringField("uid", doc.getDocId(), Store.YES));

            if (doc instanceof Topic topic) {
                luceneDoc.add(new StringField("type", "topic", Store.YES));
                luceneDoc.add(new TextField("text", topic.getText(), Store.NO));
            } else if (doc instanceof Message message) {
                luceneDoc.add(new StringField("type", "message", Store.YES));
                luceneDoc.add(new TextField("text", message.getText(), Store.NO));
            }

            indexWriter.updateDocument(new Term("uid", doc.getDocId()), luceneDoc);
        }
        
        indexWriter.commit();
    }
}
