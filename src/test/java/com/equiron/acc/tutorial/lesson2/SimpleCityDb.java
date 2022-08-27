package com.equiron.acc.tutorial.lesson2;

import java.util.List;

import com.equiron.acc.YnsConstants;
import com.equiron.acc.YnsDb;
import com.equiron.acc.YnsDbConfig;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.view.YnsMapView;

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
