package com.equiron.acc.tutorial.lesson3;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.view.YnsMapView;

public class BlogDb extends CouchDb {
    /**
     * @see http://architects.dzone.com/articles/presentation-entity
     */
    @YnsJsView(map = "if (doc['@class'] == 'com.equiron.acc.tutorial.lesson3.BlogPost')    emit(doc._id, null);" +
    		      "if (doc['@class'] == 'com.equiron.acc.tutorial.lesson3.BlogComment') emit(doc.blogPostId, null);" +
    		      "if (doc['@class'] == 'com.equiron.acc.tutorial.lesson3.Author')  for (var i in doc.blogPostsIds) emit(doc.blogPostsIds[i], null);")
    private YnsMapView<String, YnsDocument> joinedView;

    public BlogDb(CouchDbConfig config) {
        super(config);
    }

    public YnsMapView<String, YnsDocument> getJoinedView() {
        return joinedView;
    }
}
