package com.n1global.acc.tutorial.lesson3;

import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.annotation.JsView;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.view.CouchDbMapView;

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
