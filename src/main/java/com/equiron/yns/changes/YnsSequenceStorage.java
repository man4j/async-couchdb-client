package com.equiron.yns.changes;

public interface YnsSequenceStorage {

    String readSequence();
    
    void saveSequence(String sequenceValue);
}
