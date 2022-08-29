package com.equiron.yns.migration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class YnsMigrationService extends YnsAbstractMigrationService {
    private YnsMigrationDb migrationDb;
    
    public YnsMigrationService(YnsMigrationDb migrationDb) {
        this.migrationDb = migrationDb;
    }
    
    @Override
    protected Integer getVersion() {
        Map<String, Object> versionDoc = migrationDb.getRaw("migration");

        int dbVersion = versionDoc == null ? 0 : (int)versionDoc.get("version");
        
        return dbVersion;
    }

    @Override
    protected void saveVersion(int version) {
        Map<String, Object> versionDoc = migrationDb.getRaw("migration");

        if (versionDoc == null) {
            versionDoc = new HashMap<>();
            versionDoc.put("_id", "migration");
        }

        versionDoc.put("version", version);
        
        migrationDb.saveOrUpdateRaw(versionDoc);
    }
}
