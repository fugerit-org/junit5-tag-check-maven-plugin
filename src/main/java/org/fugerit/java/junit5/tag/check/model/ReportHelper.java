package org.fugerit.java.junit5.tag.check.model;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ReportHelper {

    private Map<ExecutedTest, Set<String>> testTagMap;

    private ReportModel reportModel;

    public ReportHelper(Map<ExecutedTest, Set<String>> testTagMap) {
        this.testTagMap = testTagMap;
        this.reportModel = new ReportModel();
        for (Map.Entry<ExecutedTest, Set<String>> entry : this.getTestTagMap().entrySet()) {
            ExecutedTest current = entry.getKey();
            current.getTags().addAll(entry.getValue());
            reportModel.getExecutedTests().add( current );
        }
    }

    public Map<ExecutedTest, Set<String>> getTestTagMap() {
        return this.testTagMap;
    }

    public ReportModel getReportModel() {
        return this.reportModel;
    }

    public long getSummaryPass() {
        return this.getTestTagMap().keySet().stream().filter(t -> !t.isFailed() && !t.isError()).count();
    }

    public long getSummaryFail() {
        return this.getTestTagMap().keySet().stream().filter(ExecutedTest::isFailed).count();
    }

    public long getSummaryError() {
        return this.getTestTagMap().keySet().stream().filter(ExecutedTest::isError).count();
    }

    public Map<String, List<ExecutedTest>> getTagsSummary() {
        Map<String, List<ExecutedTest>> tagToTests = new LinkedHashMap<>();
        for (Map.Entry<ExecutedTest, Set<String>> entry : this.getTestTagMap().entrySet()) {
            for (String tag : entry.getValue()) {
                tagToTests.computeIfAbsent(tag, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
        log.debug( "getTagsSummary() : {}", tagToTests );
        return tagToTests;
    }

}
