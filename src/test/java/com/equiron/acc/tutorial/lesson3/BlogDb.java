package com.equiron.acc.tutorial.lesson3;

import com.equiron.acc.YnsDb;
import com.equiron.acc.YnsDbConfig;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.json.YnsNullObject;
import com.equiron.acc.view.YnsMapReduceView;
import com.equiron.acc.view.YnsMapView;

import lombok.Getter;

@Getter
public class BlogDb extends YnsDb {
    /**
     * @see http://architects.dzone.com/articles/presentation-entity
     */
    @YnsJsView("""
               if (doc.type == 'BlogPost') emit(doc._id, null);
    		   if (doc.type == 'BlogComment') emit(doc.blogPostId, null);
    		   if (doc.type == 'Author') for (var i in doc.blogPostsIds) emit(doc.blogPostsIds[i], null); 
    		   """)
    private YnsMapView<String, BlogDocument> joinedView;

    @YnsJsView("if (doc.type == 'BlogPost') emit(doc.createdAt, null);")
    private YnsMapView<Long, YnsNullObject> postsByDate;
    
    @YnsJsView(map = "if (doc.type == 'BlogComment') emit(doc.blogPostId, null);", reduce = YnsJsView.COUNT)
    private YnsMapReduceView<String, YnsNullObject, String, Long> commentsCountByPost;

    public BlogDb(YnsDbConfig config) {
        super(config);
    }
}
