package com.equiron.acc.tutorial.lesson7;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

public class Searcher {
    private IndexWriter indexWriter;
    
    public Searcher(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    public List<String> search(String query) throws Exception {
        try (DirectoryReader directoryReader = DirectoryReader.open(indexWriter)) {
            IndexSearcher indexSearcher = new IndexSearcher(directoryReader);

            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

            try (StandardAnalyzer analyzer = new StandardAnalyzer(); TokenStream stream = analyzer.tokenStream(null, query)) {
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
    }
}
