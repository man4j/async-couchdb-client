package com.equiron.acc.changes;

import com.equiron.acc.YnsDb;

import lombok.SneakyThrows;

public class YnsLastDbSequenceStorage implements YnsSequenceStorage {
    private YnsDb ynsDb;
    
    public YnsLastDbSequenceStorage(YnsDb ynsDb) {
        this.ynsDb = ynsDb;
    }
    
    @Override
    @SneakyThrows
    public String readSequence() {
        return ynsDb.getInfo().getUpdateSeq();
    }

    @Override
    public void saveSequence(String sequenceValue) {
        //empty
    }
}
