package com.equiron.yns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.util.NamedStrategy;

public class NamedStrategyTest {
    @Test
    public void shouldWork() {
        Assertions.assertEquals("my_cool_db", NamedStrategy.addUnderscores("MyCoolDb"));
    }
}
