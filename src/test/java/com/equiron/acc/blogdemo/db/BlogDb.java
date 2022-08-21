package com.equiron.acc.blogdemo.db;

import java.util.List;

import org.springframework.stereotype.Component;

import com.equiron.acc.YnsDb;
import com.equiron.acc.YnsDbConfig;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.blogdemo.document.PostComment;
import com.equiron.acc.json.YnsNullObject;
import com.equiron.acc.view.YnsMapReduceView;
import com.equiron.acc.view.YnsMapView;

@Component
public class BlogDb extends YnsDb {
    @YnsJsView("if (doc.blogPostId) emit(doc.blogPostId, null)")
    private YnsMapView<String, YnsNullObject> commentsByPost;

    @YnsJsView(map = "if (doc.blogPostId) emit(doc.blogPostId, null)", reduce = YnsJsView.COUNT)
    private YnsMapReduceView<String, YnsNullObject, String, Long> commentsCountByPost;
    
    @YnsJsView("if (doc['@class'] == 'com.equiron.acc.blogdemo.document.BlogPost') emit(doc.createdAt, null)")
    private YnsMapView<String, YnsNullObject> postsByCreatedTime;
    
    public BlogDb(YnsDbConfig config) {
        super(config);
    }
    
    public List<PostComment> findCommentsByPost(String postId) {
        return get(commentsByPost.createQuery().byKey(postId).asIds());
    }
}
