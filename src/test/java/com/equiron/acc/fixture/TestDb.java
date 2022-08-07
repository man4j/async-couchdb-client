package com.equiron.acc.fixture;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.YnsValidator;
import com.equiron.acc.annotation.YnsErlangView;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.annotation.YnsSecurity;
import com.equiron.acc.annotation.YnsSecurityPattern;
import com.equiron.acc.annotation.YnsValidateDocUpdate;
import com.equiron.acc.view.YnsMapView;
import com.equiron.acc.view.YnsReduceView;

@YnsSecurity(admins = @YnsSecurityPattern(names = "admin"))
public class TestDb extends CouchDb {
    @YnsJsView(map = "if (doc.name) emit(doc._id, doc.name)")
    private YnsMapView<String, String> testView;
    
    @YnsErlangView(map = "Name = proplists:get_value(<<\"name\">>, Doc, null), Id = proplists:get_value(<<\"_id\">>, Doc, null), if Name /= null -> Emit(Id, Name); true -> ok end")
    private YnsMapView<String, String> testErlangView;

    @YnsJsView(map = "emit(doc._id, 1)", reduce = "return sum(values)")
    private YnsReduceView<String, Integer> reducedTestView;
    
    @YnsValidateDocUpdate("if (newDoc.name === 'bomb') throw({forbidden: 'Only admins may plant the bombs.'});")
    private YnsValidator validator;

    public TestDb(CouchDbConfig config) {
        super(config);
    }

    public YnsMapView<String, String> getTestView() {
        return testView;
    }
    
    public YnsMapView<String, String> getTestErlangView() {
        return testErlangView;
    }

    public YnsReduceView<String, Integer> getReducedTestView() {
        return reducedTestView;
    }
}
