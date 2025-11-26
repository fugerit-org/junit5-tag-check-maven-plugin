package org.fugerit.java.junit5.tag.check;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.fugerit.java.junit5.tag.check.facade.TagCheckFacade;
import org.fugerit.java.junit5.tag.check.facade.TagReportFacade;
import org.fugerit.java.junit5.tag.check.facade.TagScanFacade;
import org.fugerit.java.junit5.tag.check.facade.TagSurefireFacade;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

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
            List<ExecutedTest> executedTests = TagSurefireFacade.parseSurefireReports( this.surefireReportsDirectory, this.includeSkipped );

            getLog().info("Found " + executedTests.size() + " executed tests");

            // Extract tags from executed tests
            Map<ExecutedTest, Set<String>> testTagMap =
                    TagScanFacade.extractTagsFromExecutedTests(executedTests, classLoader);

            // Generate report
            TagReportFacade.generateReport( this.format, this.includeSkipped, this.outputFile, testTagMap);

            // Check for required tags
            if (requiredTags != null && !requiredTags.isEmpty()) {
                TagCheckFacade.checkRequiredTags( this.requiredTags, this.failOnMissingTag, testTagMap);
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

}