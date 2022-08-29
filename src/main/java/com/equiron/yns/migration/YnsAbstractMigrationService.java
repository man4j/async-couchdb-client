package com.equiron.yns.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract public class YnsAbstractMigrationService {
    @Autowired(required = false)
    private List<YnsMigration> migrations = new ArrayList<>();

    @PostConstruct
    public void migrate() {
        log.info("Found migrations: {}", migrations.size());
        
        int dbVersion = getVersion() == null ? 0 : getVersion();
            
        for (YnsMigration m : migrations.stream().sorted(Comparator.comparing(YnsMigration::getVersion)).filter(m -> m.getVersion() > dbVersion).collect(Collectors.toList())) {
            log.info("CouchDB migration started. Version: {}", m.getVersion());
            
            m.migrate();

            saveVersion(m.getVersion());
            
            log.info("CouchDB migration finished. Version: {}", m.getVersion());
        }
    }
    
    abstract protected Integer getVersion();
    
    abstract protected void saveVersion(int version);
}
