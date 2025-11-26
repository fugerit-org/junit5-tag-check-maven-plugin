package org.fugerit.java.junit5.tag.check.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.fugerit.java.core.io.helper.HelperIOException;
import org.fugerit.java.core.xml.dom.DOMIO;
import org.fugerit.java.doc.base.config.DocConfig;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;
import org.fugerit.java.junit5.tag.check.model.ReportModel;
import org.fugerit.java.junit5.tag.check.model.TestStats;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TagReportFacade {

    public static void generateReport(String format, boolean includeSkipped, File outputFile, Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        TagReportFacade facade = new TagReportFacade(format, includeSkipped, outputFile);
        facade.generateReport(testTagMap);
    }

    private File outputFile;

    private String format;

    private boolean includeSkipped;

    private TagReportFacade(String format, boolean includeSkipped, File outputFile) {
        this.format = format;
        this.includeSkipped = includeSkipped;
        this.outputFile = outputFile;
    }

    private void generateReport(Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        outputFile.getParentFile().mkdirs();

        ReportModel model = this.toReportModel(testTagMap);
        switch (format.toLowerCase()) {
            case DocConfig.TYPE_JSON:
                generateJsonReport(model);
                break;
            case DocConfig.TYPE_XML:
                generateXmlReport(testTagMap);
                break;
            case "html":
                generateHtmlReport(testTagMap);
                break;
            default:
                generateTextReport(testTagMap);
        }
    }

    private ReportModel toReportModel(Map<ExecutedTest, Set<String>> testTagMap) {
        ReportModel model = new ReportModel();
        for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
            ExecutedTest current = entry.getKey();
            current.getTags().addAll(entry.getValue());
            model.getExecutedTests().add( current );
        }
        log.info( "report model, getExecutedTests().size() : {}", model.getExecutedTests().size() );
        return model;
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
                    stats.increaseTotal();
                    if (test.isFailed()) stats.increaseFailed();
                    if (test.isError()) stats.increaseErrors();
                    if (test.isSkipped()) stats.increaseSkipped();
                }
            }

            writer.write("SUMMARY BY TAG:\n");
            writer.write(line + "\n");
            writer.write(String.format("%-20s | %5s | %5s | %5s | %5s%n",
                    "Tag", "Total", "Pass", "Fail", "Error"));
            writer.write(line + "\n");

            for (Map.Entry<String, TestStats> entry : tagStats.entrySet()) {
                TestStats stats = entry.getValue();
                int passed = stats.getTotal() - stats.getFailed() - stats.getErrors() - stats.getSkipped();
                writer.write(String.format("%-20s | %5d | %5d | %5d | %5d%n",
                        entry.getKey(), stats.getTotal(), passed, stats.getFailed(), stats.getErrors()));
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
                            Double.parseDouble(test.getTime().toString())));
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
            writer.write("<!DOCTYPE html>\n<html>\n<head>\n");
            writer.write("<meta charset=\"UTF-8\">\n");
            writer.write("<title>Executed Test Tag Report</title>\n");
            writer.write("<style>\n");
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            writer.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
            writer.write("th { background-color: #4CAF50; color: white; }\n");
            writer.write("tr:nth-child(even) { background-color: #f2f2f2; }\n");
            writer.write(".pass { color: green; }\n");
            writer.write(".fail { color: red; }\n");
            writer.write(".error { color: orange; }\n");
            writer.write(".skip { color: gray; }\n");
            writer.write(".tag { background-color: #e7f3ff; padding: 2px 8px; ");
            writer.write("border-radius: 3px; margin: 2px; display: inline-block; }\n");
            writer.write("</style>\n");
            writer.write("</head>\n<body>\n");
            writer.write("<h1>Executed Test Tag Report</h1>\n");

            // Summary
            writer.write("<h2>Summary</h2>\n");
            writer.write("<table>\n");
            writer.write("<tr><th>Metric</th><th>Count</th></tr>\n");
            writer.write("<tr><td>Total Tests</td><td>" + testTagMap.size() + "</td></tr>\n");

            long passed = testTagMap.keySet().stream()
                    .filter(t -> !t.isFailed() && !t.isError()).count();
            long failed = testTagMap.keySet().stream().filter(ExecutedTest::isFailed).count();
            long errors = testTagMap.keySet().stream().filter(ExecutedTest::isError).count();

            writer.write("<tr><td>Passed</td><td class='pass'>" + passed + "</td></tr>\n");
            writer.write("<tr><td>Failed</td><td class='fail'>" + failed + "</td></tr>\n");
            writer.write("<tr><td>Errors</td><td class='error'>" + errors + "</td></tr>\n");
            writer.write("</table>\n");

            // Tags summary
            Map<String, List<ExecutedTest>> tagToTests = new HashMap<>();
            for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                for (String tag : entry.getValue()) {
                    tagToTests.computeIfAbsent(tag, k -> new ArrayList<>()).add(entry.getKey());
                }
            }

            writer.write("<h2>Tags Summary</h2>\n");
            writer.write("<table>\n");
            writer.write("<tr><th>Tag</th><th>Tests</th></tr>\n");
            for (Map.Entry<String, List<ExecutedTest>> entry : tagToTests.entrySet()) {
                writer.write("<tr><td>" + entry.getKey() + "</td><td>" +
                        entry.getValue().size() + "</td></tr>\n");
            }
            writer.write("</table>\n");

            // All tests
            writer.write("<h2>All Executed Tests</h2>\n");
            writer.write("<table>\n");
            writer.write("<tr><th>Status</th><th>Test</th><th>Tags</th><th>Time</th></tr>\n");
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
                writer.write("</tr>\n");
            }
            writer.write("</table>\n");

            writer.write("</body>\n</html>");
        }
    }

    private void generateJsonReport(ReportModel report)
            throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(writer, report);
        }
    }

    private void generateXmlReport(Map<ExecutedTest, Set<String>> testTagMap)
            throws IOException {
        HelperIOException.apply( () -> {
            try (FileWriter writer = new FileWriter(outputFile)) {
                Document document = DOMIO.newSafeDocumentBuilderFactory().newDocumentBuilder().newDocument();
                Element root = document.createElement("executedTestTagReport");
                for (Map.Entry<ExecutedTest, Set<String>> entry : testTagMap.entrySet()) {
                    ExecutedTest test = entry.getKey();
                    Element current = document.createElement("test");
                    current.setAttribute("class", test.getClassName());
                    current.setAttribute("method", test.getMethodName());
                    current.setAttribute("time", test.getTime().toString());
                    current.setAttribute("skipped", String.valueOf( test.isSkipped() ) );
                    current.setAttribute("failed", String.valueOf( test.isFailed() ) );
                    current.setAttribute("error", String.valueOf( test.isError() ) );
                    for (String tag : entry.getValue()) {
                        Element tagElement = document.createElement( "tag" );
                        tagElement.appendChild( document.createTextNode(tag) );
                        current.appendChild(tagElement);
                    }
                    root.appendChild(current);
                }
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                DOMIO.writeDOMIndent( root, writer );
            }
        } );
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
    
}
