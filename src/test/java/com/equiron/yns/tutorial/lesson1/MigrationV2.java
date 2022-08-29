package com.equiron.yns.tutorial.lesson1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.equiron.yns.json.YnsDocument;
import com.equiron.yns.migration.YnsMigration;

@Component
public class MigrationV2 implements YnsMigration {
    @Autowired
    private ExampleDb exampleDb;
    
    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public void migrate() {
        exampleDb.saveOrUpdate(new YnsDocument("secondDoc"));
    }
}
