package com.equiron.yns.fixture;

import java.math.BigDecimal;

import com.equiron.yns.YnsDb;
import com.equiron.yns.YnsDbConfig;
import com.equiron.yns.YnsValidator;
import com.equiron.yns.annotation.YnsErlangView;
import com.equiron.yns.annotation.YnsJsView;
import com.equiron.yns.annotation.YnsSecurity;
import com.equiron.yns.annotation.YnsSecurityPattern;
import com.equiron.yns.annotation.YnsValidateDocUpdate;
import com.equiron.yns.view.YnsMapView;
import com.equiron.yns.view.YnsReduceView;

import lombok.Getter;

@YnsSecurity(admins = @YnsSecurityPattern(names = "admin"))
@Getter
@com.equiron.yns.annotation.YnsDbConfig(dbName = "my_test_db")
public class TestDb extends YnsDb {
    @YnsJsView("emit(doc._id, doc)")
    private YnsMapView<String, TestDoc> byIdView;
    
    @YnsErlangView("Emit(proplists:get_value(<<\"_id\">>, Doc, null), {Doc})")
    private YnsMapView<String, TestDoc> byIdErlangView;

    @YnsJsView("emit(doc._id, doc)")
    private YnsMapView<String, GenericTestDoc<BigDecimal>> byIdGenericView;

    @YnsJsView("if (doc.name) emit(doc._id, doc.name)")
    private YnsMapView<String, String> testView;
    
    @YnsJsView(map = "emit(doc._id, 1)", reduce = "return sum(values)")
    private YnsReduceView<String, Integer> reducedTestView;
    
    @YnsValidateDocUpdate("if (newDoc.name === 'bomb') throw({forbidden: 'Only admins may plant the bombs.'});")
    private YnsValidator validator;

    public TestDb(YnsDbConfig config) {
        super(config);
    }
}
