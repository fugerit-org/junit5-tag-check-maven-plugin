package org.fugerit.java.junit5.tag.check.model;

import lombok.extern.slf4j.Slf4j;
import org.fugerit.java.junit5.tag.check.facade.TagCheckFacade;

import java.util.*;

@Slf4j
public class ReportHelper {

    private Map<ExecutedTest, Set<String>> testTagMap;

    private ReportModel reportModel;

    private Map<String, List<ExecutedTest>> tagToTests;

    private Map<String, TestStats> tagStats;

    public ReportHelper(Map<ExecutedTest, Set<String>> testTagMap) {
        this.testTagMap = testTagMap;
        // report model
        this.reportModel = new ReportModel();
        for (Map.Entry<ExecutedTest, Set<String>> entry : this.getTestTagMap().entrySet()) {
            ExecutedTest current = entry.getKey();
            current.getTags().addAll(entry.getValue());
            reportModel.getExecutedTests().add( current );
        }
        // tag to test
        this.tagToTests = new HashMap<>();
        this.tagStats = new HashMap<>();

        for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
            ExecutedTest test = entry.getKey();
            for (String tag : entry.getValue()) {
                tagToTests.computeIfAbsent(tag, k -> new ArrayList<>()).add(test);

                TestStats stats = tagStats.computeIfAbsent(tag, k -> new TestStats());
                stats.increaseTotal();
                if (test.isFailed()) stats.increaseFailed();
                if (test.isError()) stats.increaseErrors();
                if (test.isSkipped()) stats.increaseSkipped();
            }
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
        Map<String, List<ExecutedTest>> tagsSummary = new LinkedHashMap<>();
        for (Map.Entry<ExecutedTest, Set<String>> entry : this.getTestTagMap().entrySet()) {
            for (String tag : entry.getValue()) {
                tagsSummary.computeIfAbsent(tag, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
        log.debug( "getTagsSummary() : {}", tagsSummary );
        return tagsSummary;
    }

    public Map<String, List<ExecutedTest>> getTagsToTests() {
        return this.tagToTests;
    }

    public Map<String, TestStats> getTagsStats() {
        return this.tagStats;
    }

    public TagCheckResult getTagCheckResult() {
        return TagCheckFacade.checkHelper( this.tagToTests.keySet(), testTagMap );
    }

}
