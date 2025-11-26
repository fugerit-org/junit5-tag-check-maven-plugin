package org.fugerit.java.junit5.tag.check.model;

import java.util.ArrayList;
import java.util.List;

public class ReportModel {

    public ReportModel() {
        this.executedTests = new ArrayList<>();
    }

    private List<ExecutedTest> executedTests;

    public List<ExecutedTest> getExecutedTests() {
        return this.executedTests;
    }

}
