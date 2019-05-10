package com.equiron.acc.tutorial.lesson2;

import java.util.List;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.CouchDbConstants;

public class SimpleCityDb extends CouchDb {
    public SimpleCityDb(CouchDbConfig config) {
        super(config);
    }

    public List<City> suggest(String q) {
        return getBuiltInView().<City>createDocQuery().startKey(q)
                                                      .endKey(q + CouchDbConstants.LAST_CHAR)
                                                      .asDocs();
    }
}
