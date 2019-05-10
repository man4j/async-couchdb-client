package com.equiron.acc.tutorial.lesson3;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.annotation.JsView;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.view.CouchDbMapView;

public class BlogDb extends CouchDb {
    /**
     * @see http://architects.dzone.com/articles/presentation-entity
     */
    @JsView(map = "if (doc['@class'] == 'com.n1global.acc.tutorial.lesson3.BlogPost')    emit(doc._id, null);" +
    		      "if (doc['@class'] == 'com.n1global.acc.tutorial.lesson3.BlogComment') emit(doc.blogPostId, null);" +
    		      "if (doc['@class'] == 'com.n1global.acc.tutorial.lesson3.Author')  for (var i in doc.blogPostsIds) emit(doc.blogPostsIds[i], null);")
    private CouchDbMapView<String, CouchDbDocument> joinedView;

    public BlogDb(CouchDbConfig config) {
        super(config);
    }

    public CouchDbMapView<String, CouchDbDocument> getJoinedView() {
        return joinedView;
    }
}
