package org.fugerit.java.junit5.tag.check;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Order( 1 )
class ExecutedTestTagReporterMojoTest {

    @TempDir
    Path tempDir;

    private ExecutedTestTagReporterMojo mojo;
    private MavenProject project;
    private File surefireReportsDir;
    private File outputFile;

    @BeforeEach
    void setUp() throws IOException {
        mojo = new ExecutedTestTagReporterMojo();
        project = new MavenProject();

        // Setup build
        Build build = new Build();
        File testClassesDir = tempDir.resolve("test-classes").toFile();
        testClassesDir.mkdirs();
        build.setTestOutputDirectory(testClassesDir.getAbsolutePath());
        project.setBuild(build);

        // Setup directories
        surefireReportsDir = tempDir.resolve("surefire-reports").toFile();
        surefireReportsDir.mkdirs();

        outputFile = tempDir.resolve("test-tag-report.txt").toFile();

        // Inject dependencies via reflection
        setField(mojo, "project", project);
        setField(mojo, "surefireReportsDirectory", surefireReportsDir);
        setField(mojo, "outputFile", outputFile);
        setField(mojo, "format", "text");
        setField(mojo, "includeSkipped", false);
        setField(mojo, "failOnMissingTag", false);
    }

    @Test
    void testExecuteWithNoReports() throws MojoExecutionException {
        // Given: no reports directory
        surefireReportsDir.delete();

        // When: execute
        mojo.execute();

        // Then: should not fail, just warn
        assertFalse(outputFile.exists());
    }

    @Test
    @Tag("helper")
    void testHelperMethods() throws Exception {
        // Given: a sample Surefire report
        createSampleSurefireReport("TEST-SampleHelperMethodsTest.xml",
                "com.example.helper.SampleTest",
                Arrays.asList(
                        new TestCase("testMethodA", "0.111", false, false, false),
                        new TestCase("testMethodB", "0.222", false, false, false)
                )
        );

        setField(mojo, "format", "pdf");
        outputFile = tempDir.resolve("test-tag-report.pdf").toFile();
        setField(mojo, "outputFile", outputFile);

        // When: execute
        mojo.execute();
        // Then: output file should be created
        assertTrue(outputFile.exists());

        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("PDF"));
    }


    @Test
    void testExecuteGeneratesTextReport() throws Exception {
        // Given: a sample Surefire report
        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false),
                        new TestCase("testMethod2", "0.456", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: output file should be created
        assertTrue(outputFile.exists());

        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("EXECUTED TEST TAG REPORT"));
        assertTrue(content.contains("EXECUTION SUMMARY"));
        assertTrue(content.contains("Total Tests:"));
    }

    @Test
    void testExecuteGeneratesJsonReport() throws Exception {
        // Given: JSON format
        setField(mojo, "format", "json");
        outputFile = tempDir.resolve("test-tag-report.json").toFile();
        setField(mojo, "outputFile", outputFile);

        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: JSON file should be created
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("\"executedTests\""));
        assertTrue(content.contains("\"class\""));
        assertTrue(content.contains("\"method\""));
    }

    @Test
    void testExecuteGeneratesXmlReport() throws Exception {
        // Given: XML format
        setField(mojo, "format", "xml");
        outputFile = tempDir.resolve("test-tag-report.xml").toFile();
        setField(mojo, "outputFile", outputFile);

        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: XML file should be created
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        log.info( "xml content : {}", content );
        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(content.contains("<executedTestTagReport>"));
    }

    @Test
    void testExecuteGeneratesHtmlReport() throws Exception {
        // Given: HTML format
        setField(mojo, "format", "html");
        outputFile = tempDir.resolve("test-tag-report.html").toFile();
        setField(mojo, "outputFile", outputFile);

        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: HTML file should be created
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("<!doctype html>"));
        assertTrue(content.contains("<title>Executed Test Tag Report</title>"));
        assertTrue(content.contains("Executed Test Tag Report</h1>"));
    }

    @Test
    void testExecuteWithSkippedTests() throws Exception {
        // Given: report with skipped tests
        setField(mojo, "includeSkipped", true);

        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false),
                        new TestCase("testMethod2", "0.000", true, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: should include skipped tests in report
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("Skipped:"));
    }

    @Test
    void testExecuteWithFailedTests() throws Exception {
        // Given: report with failed tests
        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false),
                        new TestCase("testMethod2", "0.456", false, true, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: should show failed tests
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("Failed:"));
    }

    @Test
    void testExecuteWithErrorTests() throws Exception {
        // Given: report with error tests
        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false),
                        new TestCase("testMethod2", "0.456", false, false, true)
                )
        );

        // When: execute
        mojo.execute();

        // Then: should show error tests
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("Errors:"));
    }

    @Test
    void testExecuteWithRequiredTagsPresent() throws Exception {
        // Given: required tags
        setField(mojo, "requiredTags", Arrays.asList("integration", "smoke"));
        setField(mojo, "failOnMissingTag", false);

        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: should complete without exception
        assertTrue(outputFile.exists());
    }

    @Test
    void testExecuteWithRequiredTagsMissing_NoFail() throws Exception {
        // Given: required tags that are missing, but failOnMissingTag = false
        setField(mojo, "requiredTags", Arrays.asList("missing-tag"));
        setField(mojo, "failOnMissingTag", false);

        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: should complete with warning
        assertTrue(outputFile.exists());
    }

    @Test
    void testExecuteWithRequiredTagsMissing_Fail() throws Exception {
        // Given: required tags that are missing, and failOnMissingTag = true
        setField(mojo, "requiredTags", Arrays.asList("missing-tag"));
        setField(mojo, "failOnMissingTag", true);

        createSampleSurefireReport("TEST-SampleTest.xml",
                "com.example.SampleTest",
                Arrays.asList(
                        new TestCase("testMethod1", "0.123", false, false, false)
                )
        );

        // When/Then: should throw exception
        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> mojo.execute()
        );

        log.info( "Error logging : "+exception.getMessage(), exception );

        assertTrue(exception.getCause().getMessage().contains("Missing required tags"));
    }

    @Test
    void testExecuteWithMultipleReportFiles() throws Exception {
        // Given: multiple Surefire report files
        createSampleSurefireReport("TEST-Test1.xml",
                "com.example.Test1",
                Arrays.asList(
                        new TestCase("testA", "0.111", false, false, false),
                        new TestCase("testB", "0.222", false, false, false)
                )
        );

        createSampleSurefireReport("TEST-Test2.xml",
                "com.example.Test2",
                Arrays.asList(
                        new TestCase("testC", "0.333", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: should process all reports
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        // Il formato potrebbe avere o meno spazi, quindi controlliamo entrambi
        assertTrue(content.contains("Total Tests:    3") ||
                content.contains("Total Tests: 3") ||
                content.matches("(?s).*Total Tests:\\s+3.*"));
    }

    @Test
    void testExecuteHandlesInvalidXml() throws Exception {
        // Given: invalid XML file
        File invalidReport = new File(surefireReportsDir, "TEST-Invalid.xml");
        try (FileWriter writer = new FileWriter(invalidReport)) {
            writer.write("This is not valid XML");
        }

        // When: execute - should handle gracefully
        assertDoesNotThrow(() -> mojo.execute());

        // Then: should not throw exception (just logs warning)
    }

    @Test
    void testExecuteCreatesOutputDirectory() throws Exception {
        // Given: output file in non-existent directory
        File newOutputDir = tempDir.resolve("reports/nested/dir").toFile();
        outputFile = new File(newOutputDir, "report.txt");
        setField(mojo, "outputFile", outputFile);

        assertFalse(newOutputDir.exists());

        createSampleSurefireReport("TEST-Test.xml",
                "com.example.Test",
                Arrays.asList(
                        new TestCase("test1", "0.123", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: should create directory and file
        assertTrue(newOutputDir.exists());
        assertTrue(outputFile.exists());
    }

    @Test
    void testTextReportFormat() throws Exception {
        // Given: a test report
        createSampleSurefireReport("TEST-FormatTest.xml",
                "com.example.FormatTest",
                Arrays.asList(
                        new TestCase("passedTest", "0.100", false, false, false),
                        new TestCase("failedTest", "0.200", false, true, false),
                        new TestCase("errorTest", "0.300", false, false, true)
                )
        );

        // When: execute
        mojo.execute();

        // Then: check report structure
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));

        // Verifica sezioni principali
        assertTrue(content.contains("EXECUTED TEST TAG REPORT"));
        assertTrue(content.contains("EXECUTION SUMMARY"));
        assertTrue(content.contains("SUMMARY BY TAG"));
        assertTrue(content.contains("ALL EXECUTED TESTS WITH TAGS"));

        // Verifica statistiche
        assertTrue(content.matches("(?s).*Total Tests:\\s+3.*"));
        assertTrue(content.matches("(?s).*Passed:\\s+1.*"));
        assertTrue(content.matches("(?s).*Failed:\\s+1.*"));
        assertTrue(content.matches("(?s).*Errors:\\s+1.*"));
    }

    @Test
    void testJsonReportStructure() throws Exception {
        // Given: JSON format
        setField(mojo, "format", "json");
        outputFile = tempDir.resolve("report.json").toFile();
        setField(mojo, "outputFile", outputFile);

        createSampleSurefireReport("TEST-JsonTest.xml",
                "com.example.JsonTest",
                Arrays.asList(
                        new TestCase("test1", "0.123", false, false, false),
                        new TestCase("test2", "0.456", false, true, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: verify JSON structure
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        log.info( "outputFile json : {}", outputFile.toPath() );
        assertTrue(content.trim().startsWith("{"));
        assertTrue(content.trim().endsWith("}"));
        assertTrue(content.contains("\"executedTests\""));
        assertTrue(content.contains("\"class\""));
        assertTrue(content.contains("\"method\""));
        assertTrue(content.contains("\"time\""));
        assertTrue(content.contains("\"failed\""));
        assertTrue(content.contains("\"tags\""));

        // Verifica che ci siano 2 test
        long testCount = content.chars().filter(ch -> ch == '{').count() - 1; // -1 per l'oggetto root
        assertTrue(testCount >= 2);
    }

    @Test
    void testXmlReportStructure() throws Exception {
        // Given: XML format
        setField(mojo, "format", "xml");
        outputFile = tempDir.resolve("report.xml").toFile();
        setField(mojo, "outputFile", outputFile);

        createSampleSurefireReport("TEST-XmlTest.xml",
                "com.example.XmlTest",
                Arrays.asList(
                        new TestCase("test1", "0.111", false, false, false)
                )
        );

        // When: execute
        mojo.execute();

        // Then: verify XML structure
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));

        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(content.contains("<executedTestTagReport>"));
        assertTrue(content.contains("</executedTestTagReport>"));
        assertTrue(content.contains("<test "));
        assertTrue(content.contains("class="));
        assertTrue(content.contains("method="));
    }

    // Helper methods

    private void createSampleSurefireReport(String filename, String className,
                                            List<TestCase> testCases) throws IOException {
        File reportFile = new File(surefireReportsDir, filename);
        try (StringWriter writer = new StringWriter();
             FileWriter fileWriter = new FileWriter(reportFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<testsuite name=\"" + className + "\" ");
            writer.write("tests=\"" + testCases.size() + "\" ");
            writer.write("failures=\"" + testCases.stream().filter(tc -> tc.failed).count() + "\" ");
            writer.write("errors=\"" + testCases.stream().filter(tc -> tc.error).count() + "\" ");
            writer.write("skipped=\"" + testCases.stream().filter(tc -> tc.skipped).count() + "\">\n");

            for (TestCase testCase : testCases) {
                writer.write("  <testcase name=\"" + testCase.name + "\" ");
                writer.write("classname=\"" + className + "\" ");
                writer.write("time=\"" + testCase.time + "\"");

                if (!testCase.skipped && !testCase.failed && !testCase.error) {
                    writer.write("/>\n");
                } else {
                    writer.write(">\n");
                    if (testCase.skipped) {
                        writer.write("    <skipped/>\n");
                    }
                    if (testCase.failed) {
                        writer.write("    <failure message=\"Test failed\"/>\n");
                    }
                    if (testCase.error) {
                        writer.write("    <error message=\"Test error\"/>\n");
                    }
                    writer.write("  </testcase>\n");
                }
            }
            writer.write("</testsuite>\n");
            log.info( "report, file : {}, content : {}", outputFile.getAbsolutePath(), writer );
            fileWriter.write(writer.toString());
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    static class TestCase {
        String name;
        String time;
        boolean skipped;
        boolean failed;
        boolean error;

        TestCase(String name, String time, boolean skipped, boolean failed, boolean error) {
            this.name = name;
            this.time = time;
            this.skipped = skipped;
            this.failed = failed;
            this.error = error;
        }
    }
}