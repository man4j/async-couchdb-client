package com.equiron.yns.changes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import lombok.SneakyThrows;

public class YnsFileSequenceStorage implements YnsSequenceStorage {
    private final String fileName;
    
    public YnsFileSequenceStorage(String fileName) {
        this.fileName = fileName;
    }
    
    @Override
    @SneakyThrows
    public String readSequence() {
        Path fullPath = Path.of(fileName);
        
        if (!Files.exists(fullPath)) {
            return null;
        }
        
        return Files.readString(fullPath);
    }

    @Override
    @SneakyThrows
    public void saveSequence(String sequenceValue) {
        Files.writeString(Path.of(fileName), sequenceValue, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
    }
}
