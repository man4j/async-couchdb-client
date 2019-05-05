package com.n1global.acc;

import org.junit.Assert;
import org.junit.Test;

import com.equiron.acc.util.NamedStrategy;

public class NamedStrategyTest {
    @Test
    public void shouldWork() {
        Assert.assertEquals("my_cool_db", NamedStrategy.addUnderscores("MyCoolDb"));
    }
}
