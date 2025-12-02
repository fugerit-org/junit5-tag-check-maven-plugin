package org.fugerit.java.junit5.tag.check;

import org.fugerit.java.junit5.tag.check.model.TestStats;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestStatsTest {

    @Test
    void testStats() {
        TestStats stats = new TestStats();
        stats.increaseErrors();
        stats.increaseFailed();
        stats.increaseSkipped();
        stats.increaseTotal();
        stats.increaseTotal();
        stats.increaseTotal();
        Assertions.assertEquals( 1, stats.getErrors() );
        Assertions.assertEquals( 1, stats.getFailed() );
        Assertions.assertEquals( 1, stats.getSkipped() );
        Assertions.assertEquals( 3, stats.getTotal() );
    }

}
