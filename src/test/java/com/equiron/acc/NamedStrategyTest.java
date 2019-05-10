package com.equiron.acc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.util.NamedStrategy;

public class NamedStrategyTest {
    @Test
    public void shouldWork() {
        Assertions.assertEquals("my_cool_db", NamedStrategy.addUnderscores("MyCoolDb"));
    }
}
