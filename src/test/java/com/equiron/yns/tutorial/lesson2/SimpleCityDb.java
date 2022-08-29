package com.equiron.yns.tutorial.lesson2;

import java.util.List;

import com.equiron.yns.YnsConstants;
import com.equiron.yns.YnsDb;
import com.equiron.yns.YnsDbConfig;
import com.equiron.yns.annotation.YnsJsView;
import com.equiron.yns.view.YnsMapView;

public class SimpleCityDb extends YnsDb {
    @YnsJsView("emit(doc.name, doc)")
    private YnsMapView<String, City> byNameView;

    public SimpleCityDb(YnsDbConfig config) {
        super(config);
    }

    public List<City> suggest(String q) {
        return byNameView.createQuery().startKey(q)
                                       .endKey(q + YnsConstants.LAST_CHAR)
                                       .asValues();
    }
}
