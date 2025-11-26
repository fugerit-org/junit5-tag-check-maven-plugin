package org.fugerit.java.junit5.tag.check.facade;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TagSurefireFacade {

    private TagSurefireFacade() {}

    public static List<ExecutedTest> parseSurefireReports( File surefireReportsDirectory, boolean includeSkipped ) {
        List<ExecutedTest> executedTests = new ArrayList<>();
        SAXReader reader = new SAXReader();

        // Find all XML report files
        File[] reportFiles = surefireReportsDirectory.listFiles(
                (dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));

        if (reportFiles == null || reportFiles.length == 0) {
            log.warn("No Surefire XML reports found in: {}",
                    surefireReportsDirectory.getAbsolutePath());
            return executedTests;
        }

        for (File reportFile : reportFiles) {
            log.debug("Parsing report: {}", reportFile.getName());

            try {
                Document document = reader.read(reportFile);
                Element root = document.getRootElement();

                String className = root.attributeValue("name");

                @SuppressWarnings("unchecked")
                List<Element> testCases = root.elements("testcase");

                for (Element testCase : testCases) {
                    String methodName = testCase.attributeValue("name");
                    String testClassName = testCase.attributeValue("classname", className);
                    String time = testCase.attributeValue("time");

                    boolean skipped = testCase.element("skipped") != null;
                    boolean failed = testCase.element("failure") != null;
                    boolean error = testCase.element("error") != null;

                    ExecutedTest test = new ExecutedTest(
                            testClassName,
                            methodName,
                            skipped,
                            failed,
                            error,
                            new BigDecimal(time)
                    );

                    if (!skipped || includeSkipped) {
                        executedTests.add(test);
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing report file: {}", reportFile.getName(), e);
            }
        }

        return executedTests;
    }

}
