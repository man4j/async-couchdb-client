package com.n1global.acc.tutorial.lesson8;

import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.CouchDbDirectUpdater;
import com.n1global.acc.annotation.UpdateHandler;

public class UserDb extends CouchDb {
    /**
     * @see http://wiki.apache.org/couchdb/Document_Update_Handlers
     */
    @UpdateHandler(func = "var name = doc.name.split(' ')[0];" +
    		              "var lastName = doc.name.split(' ')[1];" +
    		              "doc.name = name; " +
    		              "doc.lastName = lastName; " +
    		              "return [doc, '']")
    private CouchDbDirectUpdater testUpdater;

    public UserDb(CouchDbConfig config) {
        super(config);
    }

    public CouchDbDirectUpdater getTestUpdater() {
        return testUpdater;
    }
}
