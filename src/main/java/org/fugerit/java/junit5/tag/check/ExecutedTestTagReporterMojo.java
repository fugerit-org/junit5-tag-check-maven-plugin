package org.fugerit.java.junit5.tag.check;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
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
                    extractTagsFromExecutedTests(executedTests, classLoader);

            // Generate report
            generateReport(testTagMap);

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

    private List<ExecutedTest> parseSurefireReports() throws Exception {
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

    private Map<ExecutedTest, Set<String>> extractTagsFromExecutedTests(
            List<ExecutedTest> executedTests,
            URLClassLoader classLoader) {

        Map<ExecutedTest, Set<String>> testTagMap = new LinkedHashMap<>();

        for (ExecutedTest test : executedTests) {
            try {
                Class<?> testClass = classLoader.loadClass(test.getClassName());
                Method testMethod = findTestMethod(testClass, test.getMethodName());

                if (testMethod != null) {
                    Set<String> tags = new HashSet<>();

                    // Get tags from method
                    Tag[] methodTags = testMethod.getAnnotationsByType(Tag.class);
                    for (Tag tag : methodTags) {
                        tags.add(tag.value());
                    }

                    // Get tags from class
                    Tag[] classTags = testClass.getAnnotationsByType(Tag.class);
                    for (Tag tag : classTags) {
                        tags.add(tag.value());
                    }

                    testTagMap.put(test, tags);
                } else {
                    getLog().warn("Could not find method: " + test.getClassName() +
                            "#" + test.getMethodName());
                    testTagMap.put(test, Collections.emptySet());
                }
            } catch (ClassNotFoundException e) {
                getLog().warn("Could not load test class: " + test.getClassName());
                testTagMap.put(test, Collections.emptySet());
            }
        }

        return testTagMap;
    }

    private Method findTestMethod(Class<?> testClass, String methodName) {
        // Try exact match first
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        // JUnit 5 display names might cause mismatches - try parent classes
        Class<?> currentClass = testClass.getSuperclass();
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

    private void generateReport(Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        outputFile.getParentFile().mkdirs();

        switch (format.toLowerCase()) {
            case "json":
                generateJsonReport(testTagMap);
                break;
            case "xml":
                generateXmlReport(testTagMap);
                break;
            case "html":
                generateHtmlReport(testTagMap);
                break;
            default:
                generateTextReport(testTagMap);
        }
    }

    private void generateTextReport(Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        // Helper method for String.repeat(80)
        String separator = repeatString("=", 80);
        String line = repeatString("-", 80);

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(separator + "\n");
            writer.write("EXECUTED TEST TAG REPORT\n");
            writer.write(separator + "\n\n");

            int totalTests = testTagMap.size();

            // Calcolo delle statistiche con i filter di Java 8 Stream
            int passedTests = (int) testTagMap.keySet().stream()
                    .filter(t -> !t.isFailed() && !t.isError()).count();
            int failedTests = (int) testTagMap.keySet().stream()
                    .filter(ExecutedTest::isFailed).count();
            int errorTests = (int) testTagMap.keySet().stream()
                    .filter(ExecutedTest::isError).count();
            int skippedTests = (int) testTagMap.keySet().stream()
                    .filter(ExecutedTest::isSkipped).count();

            writer.write("EXECUTION SUMMARY:\n");
            writer.write(line + "\n");
            writer.write(String.format("  Total Tests:    %d%n", totalTests));
            writer.write(String.format("  Passed:         %d%n", passedTests));
            writer.write(String.format("  Failed:         %d%n", failedTests));
            writer.write(String.format("  Errors:         %d%n", errorTests));
            if (includeSkipped) {
                writer.write(String.format("  Skipped:        %d%n", skippedTests));
            }
            writer.write("\n");

            // Summary by tag (Java 8 compatible map operations: computeIfAbsent is fine)
            Map<String, List<ExecutedTest>> tagToTests = new HashMap<>();
            Map<String, TestStats> tagStats = new HashMap<>();

            for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                ExecutedTest test = entry.getKey();
                for (String tag : entry.getValue()) {
                    tagToTests.computeIfAbsent(tag, k -> new ArrayList<>()).add(test);

                    TestStats stats = tagStats.computeIfAbsent(tag, k -> new TestStats());
                    stats.total++;
                    if (test.isFailed()) stats.failed++;
                    if (test.isError()) stats.errors++;
                    if (test.isSkipped()) stats.skipped++;
                }
            }

            writer.write("SUMMARY BY TAG:\n");
            writer.write(line + "\n");
            writer.write(String.format("%-20s | %5s | %5s | %5s | %5s%n",
                    "Tag", "Total", "Pass", "Fail", "Error"));
            writer.write(line + "\n");

            for (Map.Entry<String, TestStats> entry : tagStats.entrySet()) {
                TestStats stats = entry.getValue();
                int passed = stats.total - stats.failed - stats.errors - stats.skipped;
                writer.write(String.format("%-20s | %5d | %5d | %5d | %5d%n",
                        entry.getKey(), stats.total, passed, stats.failed, stats.errors));
            }

            // Tests without tags
            long testsWithoutTags = testTagMap.values().stream()
                    .filter(Set::isEmpty)
                    .count();

            if (testsWithoutTags > 0) {
                writer.write(String.format("%-20s | %5d | %5s | %5s | %5s%n",
                        "<NO TAG>", testsWithoutTags, "?", "?", "?"));
            }

            writer.write("\n" + separator + "\n");
            writer.write("EXECUTED TESTS BY TAG:\n");
            writer.write(separator + "\n\n");

            for (Map.Entry<String, List<ExecutedTest>> entry : tagToTests.entrySet()) {
                writer.write(String.format("Tag: %s (%d tests)%n",
                        entry.getKey(), entry.getValue().size()));
                writer.write(line + "\n");
                for (ExecutedTest test : entry.getValue()) {
                    String status = getStatusIcon(test);
                    writer.write(String.format("  %s %s#%s (%.3fs)%n",
                            status,
                            test.getClassName(),
                            test.getMethodName(),
                            Double.parseDouble(test.getTime())));
                }
                writer.write("\n");
            }

            writer.write("\n" + separator + "\n");
            writer.write("ALL EXECUTED TESTS WITH TAGS:\n");
            writer.write(separator + "\n\n");

            for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                ExecutedTest test = entry.getKey();
                String status = getStatusIcon(test);

                // Sostituzione di String.join (che è OK per Java 8, ma spesso da errore di compilazione
                // se la versione JDK non è impostata correttamente nel POM) con un Joiner da Stream
                String tags = entry.getValue().isEmpty() ?
                        "<NO TAGS>" : "[" + entry.getValue().stream().collect(Collectors.joining(", ")) + "]";

                writer.write(String.format("%s %-50s : %s%n",
                        status,
                        test.getClassName() + "#" + test.getMethodName(),
                        tags));
            }

            if (testsWithoutTags > 0) {
                writer.write("%n" + separator + "\n");
                writer.write("⚠️  WARNING: " + testsWithoutTags +
                        " executed tests without tags\n");
                writer.write(separator + "\n");
            }
        }
    }

    /**
     * Helper method to replace String.repeat(int) which is Java 11+.
     */
    private String repeatString(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private void generateHtmlReport(Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("<!DOCTYPE html>%n<html>%n<head>%n");
            writer.write("<meta charset=\"UTF-8\">%n");
            writer.write("<title>Executed Test Tag Report</title>%n");
            writer.write("<style>%n");
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; }%n");
            writer.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; }%n");
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }%n");
            writer.write("th { background-color: #4CAF50; color: white; }%n");
            writer.write("tr:nth-child(even) { background-color: #f2f2f2; }%n");
            writer.write(".pass { color: green; }%n");
            writer.write(".fail { color: red; }%n");
            writer.write(".error { color: orange; }%n");
            writer.write(".skip { color: gray; }%n");
            writer.write(".tag { background-color: #e7f3ff; padding: 2px 8px; ");
            writer.write("border-radius: 3px; margin: 2px; display: inline-block; }%n");
            writer.write("</style>%n");
            writer.write("</head>%n<body>%n");
            writer.write("<h1>Executed Test Tag Report</h1>%n");

            // Summary
            writer.write("<h2>Summary</h2>%n");
            writer.write("<table>%n");
            writer.write("<tr><th>Metric</th><th>Count</th></tr>%n");
            writer.write("<tr><td>Total Tests</td><td>" + testTagMap.size() + "</td></tr>%n");

            long passed = testTagMap.keySet().stream()
                    .filter(t -> !t.isFailed() && !t.isError()).count();
            long failed = testTagMap.keySet().stream().filter(ExecutedTest::isFailed).count();
            long errors = testTagMap.keySet().stream().filter(ExecutedTest::isError).count();

            writer.write("<tr><td>Passed</td><td class='pass'>" + passed + "</td></tr>%n");
            writer.write("<tr><td>Failed</td><td class='fail'>" + failed + "</td></tr>%n");
            writer.write("<tr><td>Errors</td><td class='error'>" + errors + "</td></tr>%n");
            writer.write("</table>%n");

            // Tags summary
            Map<String, List<ExecutedTest>> tagToTests = new HashMap<>();
            for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                for (String tag : entry.getValue()) {
                    tagToTests.computeIfAbsent(tag, k -> new ArrayList<>()).add(entry.getKey());
                }
            }

            writer.write("<h2>Tags Summary</h2>%n");
            writer.write("<table>%n");
            writer.write("<tr><th>Tag</th><th>Tests</th></tr>%n");
            for (Map.Entry<String, List<ExecutedTest>> entry : tagToTests.entrySet()) {
                writer.write("<tr><td>" + entry.getKey() + "</td><td>" +
                        entry.getValue().size() + "</td></tr>%n");
            }
            writer.write("</table>%n");

            // All tests
            writer.write("<h2>All Executed Tests</h2>%n");
            writer.write("<table>%n");
            writer.write("<tr><th>Status</th><th>Test</th><th>Tags</th><th>Time</th></tr>%n");
            for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                ExecutedTest test = entry.getKey();
                String statusClass = test.isFailed() ? "fail" :
                        test.isError() ? "error" : "pass";
                String status = getStatusIcon(test);

                writer.write("<tr>");
                writer.write("<td class='" + statusClass + "'>" + status + "</td>");
                writer.write("<td>" + test.getClassName() + "#" + test.getMethodName() + "</td>");
                writer.write("<td>");
                for (String tag : entry.getValue()) {
                    writer.write("<span class='tag'>" + tag + "</span> ");
                }
                writer.write("</td>");
                writer.write("<td>" + test.getTime() + "s</td>");
                writer.write("</tr>%n");
            }
            writer.write("</table>%n");

            writer.write("</body>%n</html>");
        }
    }

    private void generateJsonReport(Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("{\n");
            writer.write("  \"executedTests\": [\n");

            int count = 0;
            for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                if (count++ > 0) writer.write(",\n");
                ExecutedTest test = entry.getKey();
                writer.write("    {\n");
                writer.write("      \"class\": \"" + escapeJson(test.getClassName()) + "\",\n");
                writer.write("      \"method\": \"" + escapeJson(test.getMethodName()) + "\",\n");
                writer.write("      \"time\": " + test.getTime() + ",\n");
                writer.write("      \"skipped\": " + test.isSkipped() + ",\n");
                writer.write("      \"failed\": " + test.isFailed() + ",\n");
                writer.write("      \"error\": " + test.isError() + ",\n");
                writer.write("      \"tags\": [");
                writer.write(entry.getValue().stream()
                        .map(tag -> "\"" + escapeJson(tag) + "\"")
                        .collect(Collectors.joining(", ")));
                writer.write("]\n");
                writer.write("    }");
            }

            writer.write("\n  ]\n");
            writer.write("}");
        }
    }

    private void generateXmlReport(Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<executedTestTagReport>\n");

            for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                ExecutedTest test = entry.getKey();
                writer.write("  <test class=\"" + escapeXml(test.getClassName()) + "\" ");
                writer.write("method=\"" + escapeXml(test.getMethodName()) + "\" ");
                writer.write("time=\"" + test.getTime() + "\" ");
                writer.write("skipped=\"" + test.isSkipped() + "\" ");
                writer.write("failed=\"" + test.isFailed() + "\" ");
                writer.write("error=\"" + test.isError() + "\">\n");
                for (String tag : entry.getValue()) {
                    writer.write("    <tag>" + escapeXml(tag) + "</tag>\n");
                }
                writer.write("  </test>\n");
            }

            writer.write("</executedTestTagReport>\n");
        }
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

    private String getStatusIcon(ExecutedTest test) {
        if (test.isFailed()) return "❌";
        if (test.isError()) return "⚠️";
        if (test.isSkipped()) return "⊘";
        return "✅";
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String escapeXml(String str) {
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // Helper classes
    static class ExecutedTest {
        private final String className;
        private final String methodName;
        private final boolean skipped;
        private final boolean failed;
        private final boolean error;
        private final String time;

        public ExecutedTest(String className, String methodName,
                            boolean skipped, boolean failed, boolean error, String time) {
            this.className = className;
            this.methodName = methodName;
            this.skipped = skipped;
            this.failed = failed;
            this.error = error;
            this.time = time;
        }

        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public boolean isSkipped() { return skipped; }
        public boolean isFailed() { return failed; }
        public boolean isError() { return error; }
        public String getTime() { return time; }
    }

    static class TestStats {
        int total = 0;
        int failed = 0;
        int errors = 0;
        int skipped = 0;
    }
}