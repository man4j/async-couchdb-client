package com.n1global.acc.tutorial.lesson7;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.n1global.acc.notification.document.CouchDbDocumentUpdateHandler;

public class Indexer implements CouchDbDocumentUpdateHandler<ForumContent>, Closeable {
    private IndexWriter indexWriter;

    @SuppressWarnings("resource")
    public Indexer() throws Exception {
        indexWriter = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(Version.LUCENE_43)));
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
    }

    @Override
    public void onUpdate(ForumContent doc) throws Exception {
        Document luceneDoc = new Document();

        luceneDoc.add(new StringField("uid", doc.getDocId(), Store.YES));

        if (doc instanceof Topic) {
            Topic topic = (Topic) doc;

            luceneDoc.add(new StringField("type", "topic", Store.YES));
            luceneDoc.add(new TextField("text", topic.getText(), Store.NO));
        } else if (doc instanceof Message) {
            Message message = (Message) doc;

            luceneDoc.add(new StringField("type", "message", Store.YES));
            luceneDoc.add(new TextField("text", message.getText(), Store.NO));
        }

        indexWriter.updateDocument(new Term("uid", doc.getDocId()), luceneDoc);
    }

    @Override
    public void onDelete(String docId) throws Exception {
        indexWriter.deleteDocuments(new Term("uid", docId));
    }

    public List<String> search(String query) throws Exception {
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(indexWriter, true));

        BooleanQuery booleanQuery = new BooleanQuery();

        try (StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_43); TokenStream stream = analyzer.tokenStream("text", new StringReader(query))) {
            stream.reset();

            CharTermAttribute attribute = stream.addAttribute(CharTermAttribute.class);

            while (stream.incrementToken()) {
                booleanQuery.add(new FuzzyQuery(new Term("text", attribute.toString()), 2, 1), Occur.SHOULD);
            }

            stream.end();
        }

        TopDocs topDocs = indexSearcher.search(booleanQuery, 15);

        List<String> results = new ArrayList<>();

        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            results.add(indexSearcher.doc(topDocs.scoreDocs[i].doc).get("uid"));
        }

        return results;
    }
}
