package com.equiron.yns.fixture;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.annotation.YnsJsView;
import com.equiron.yns.annotation.YnsSecurity;
import com.equiron.yns.annotation.YnsSecurityPattern;
import com.equiron.yns.view.YnsMapView;

import lombok.Getter;

@YnsSecurity(admins = @YnsSecurityPattern(names = "admin"))
@Getter
@com.equiron.yns.annotation.YnsDbConfig(dbName = "my_test_db")
@Component
public class TestDb2 extends YnsDb {
    @YnsJsView("emit(doc._id, doc)")
    private YnsMapView<String, TestDoc> byIdView;    
}
