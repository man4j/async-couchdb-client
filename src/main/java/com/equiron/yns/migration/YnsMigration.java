package com.equiron.yns.migration;

public interface YnsMigration {
    int getVersion();
    
    void migrate();
}
