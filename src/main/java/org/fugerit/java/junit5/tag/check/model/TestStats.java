package org.fugerit.java.junit5.tag.check.model;

public class TestStats {

    private int total = 0;
    private int failed = 0;
    private int errors = 0;
    private int skipped = 0;

    public void increaseTotal() {
        this.total++;
    }

    public void increaseFailed() {
        this.failed++;
    }

    public void increaseErrors() {
        this.errors++;
    }

    public void increaseSkipped() {
        this.skipped++;
    }

    public int getErrors() {
        return errors;
    }

    public int getFailed() {
        return failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getTotal() {
        return total;
    }
}