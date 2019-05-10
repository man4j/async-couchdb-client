package com.equiron.acc.tutorial.lesson7;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
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
import org.apache.lucene.store.MMapDirectory;

import com.equiron.acc.changes.CouchDbEventHandler;
import com.equiron.acc.json.CouchDbEvent;

public class Indexer implements CouchDbEventHandler<ForumContent> {
    private IndexWriter indexWriter;

    @SuppressWarnings("resource")
    public Indexer() throws Exception {
        indexWriter = new IndexWriter(new MMapDirectory(Files.createTempDirectory("tmp")), new IndexWriterConfig(new StandardAnalyzer()));
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
    public void onEvent(CouchDbEvent<ForumContent> e) throws Exception {
        if (e.isDeleted()) {
            indexWriter.deleteDocuments(new Term("uid", e.getDocId()));
        } else {
            ForumContent doc = e.getDoc();

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
    }

    public List<String> search(String query) throws Exception {
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(indexWriter));

        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

        try (StandardAnalyzer analyzer = new StandardAnalyzer(); TokenStream stream = analyzer.tokenStream("text", new StringReader(query))) {
            stream.reset();

            CharTermAttribute attribute = stream.addAttribute(CharTermAttribute.class);

            while (stream.incrementToken()) {
                booleanQueryBuilder.add(new FuzzyQuery(new Term("text", attribute.toString()), 2, 1), Occur.SHOULD);
            }

            stream.end();
        }

        TopDocs topDocs = indexSearcher.search(booleanQueryBuilder.build(), 15);

        List<String> results = new ArrayList<>();

        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            results.add(indexSearcher.doc(topDocs.scoreDocs[i].doc).get("uid"));
        }

        return results;
    }

    @Override
    public void onStart() throws Exception {
        //empty
    }

    @Override
    public void onError(Throwable e) throws Exception {
        e.printStackTrace();
    }
}
