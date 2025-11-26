package org.fugerit.java.junit5.tag.check;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.fugerit.java.junit5.tag.check.facade.TagReportFacade;
import org.fugerit.java.junit5.tag.check.facade.TagScanFacade;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;
import org.fugerit.java.junit5.tag.check.model.TestStats;
import org.junit.jupiter.api.Tag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reports tags from actually executed tests by parsing Surefire reports
 */
@Mojo(
        name = "report-executed-tags",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.TEST
)
public class ExecutedTestTagReporterMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "surefire.reports.directory",
            defaultValue = "${project.build.directory}/surefire-reports")
    private File surefireReportsDirectory;

    @Parameter(property = "test.tag.reporter.outputFile",
            defaultValue = "${project.build.directory}/executed-test-tag-report.txt")
    private File outputFile;

    @Parameter(property = "test.tag.reporter.format", defaultValue = "text")
    private String format; // text, json, xml, html

    @Parameter(property = "test.tag.reporter.requiredTags")
    private List<String> requiredTags;

    @Parameter(property = "test.tag.reporter.failOnMissingTag", defaultValue = "false")
    private boolean failOnMissingTag;

    @Parameter(property = "test.tag.reporter.includeSkipped", defaultValue = "false")
    private boolean includeSkipped;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Executed Test Tag Reporter - Analyzing test execution...");

        if (!surefireReportsDirectory.exists()) {
            getLog().warn("Surefire reports directory not found: " +
                    surefireReportsDirectory.getAbsolutePath());
            return;
        }

        try {
            // Build classpath for test classes
            URLClassLoader classLoader = createTestClassLoader();

            // Parse Surefire reports to find executed tests
            List<ExecutedTest> executedTests = parseSurefireReports();

            getLog().info("Found " + executedTests.size() + " executed tests");

            // Extract tags from executed tests
            Map<ExecutedTest, Set<String>> testTagMap =
                    TagScanFacade.extractTagsFromExecutedTests(executedTests, classLoader);

            // Generate report
            TagReportFacade.generateReport( this.format, this.includeSkipped, this.outputFile, testTagMap);

            // Check for required tags
            if (requiredTags != null && !requiredTags.isEmpty()) {
                checkRequiredTags(testTagMap);
            }

            getLog().info("Executed Test Tag Report generated: " +
                    outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Error generating executed test tag report", e);
        }
    }

    private URLClassLoader createTestClassLoader() throws DependencyResolutionRequiredException {
        List<String> classpathElements = project.getTestClasspathElements();
        URL[] urls = classpathElements.stream()
                .map(element -> {
                    try {
                        return new File(element).toURI().toURL();
                    } catch (Exception e) {
                        getLog().warn("Could not convert to URL: " + element);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(URL[]::new);

        return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }

    private List<ExecutedTest> parseSurefireReports() {
        List<ExecutedTest> executedTests = new ArrayList<>();
        SAXReader reader = new SAXReader();

        // Find all XML report files
        File[] reportFiles = surefireReportsDirectory.listFiles(
                (dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));

        if (reportFiles == null || reportFiles.length == 0) {
            getLog().warn("No Surefire XML reports found in: " +
                    surefireReportsDirectory.getAbsolutePath());
            return executedTests;
        }

        for (File reportFile : reportFiles) {
            getLog().debug("Parsing report: " + reportFile.getName());

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
                            time
                    );

                    if (!skipped || includeSkipped) {
                        executedTests.add(test);
                    }
                }
            } catch (Exception e) {
                getLog().warn("Error parsing report file: " + reportFile.getName(), e);
            }
        }

        return executedTests;
    }



    private void checkRequiredTags(Map<ExecutedTest, Set<String>> testTagMap)
            throws MojoExecutionException {
        Set<String> foundTags = testTagMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        List<String> missingTags = new ArrayList<>();
        for (String requiredTag : requiredTags) {
            if (!foundTags.contains(requiredTag)) {
                missingTags.add(requiredTag);
            }
        }

        if (!missingTags.isEmpty()) {
            String message = "Missing required tags in executed tests: " +
                    String.join(", ", missingTags);
            if (failOnMissingTag) {
                throw new MojoExecutionException(message);
            } else {
                getLog().warn(message);
            }
        } else {
            getLog().info("All required tags found in executed tests: " +
                    String.join(", ", requiredTags));
        }
    }

}