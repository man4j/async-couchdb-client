package com.equiron.acc.changes;

public interface YnsSequenceStorage {

    String readSequence();
    
    void saveSequence(String sequenceValue);
}
