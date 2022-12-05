package com.equiron.yns.tutorial.lesson4;

import java.util.concurrent.CountDownLatch;

import org.springframework.stereotype.Component;

import com.equiron.yns.changes.YnsEventListener;
import com.equiron.yns.changes.YnsSequenceStorage;
import com.equiron.yns.json.YnsEvent;

import lombok.Getter;

@Getter
@Component
public class ExampleListener extends YnsEventListener {
    public ExampleListener(ExampleDb db, YnsSequenceStorage sequenceStorage) {
		super(db, sequenceStorage);
	}

	private final CountDownLatch latch = new CountDownLatch(1);
    
    @Override
    public void onEvent(YnsEvent event) throws Exception {
        System.out.println("Is new: " + event.isNew());
        System.out.println("Is deleted: " + event.isDeleted());
        
        latch.countDown();
    }
}
