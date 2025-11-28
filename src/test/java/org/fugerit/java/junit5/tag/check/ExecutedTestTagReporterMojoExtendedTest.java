package org.fugerit.java.junit5.tag.check;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.fugerit.java.doc.base.config.DocConfig;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Order( 2 ) // must run after ExecutedTestTagReporterMojoTest
class ExecutedTestTagReporterMojoExtendedTest {

    @Test
    void testExtended() throws MojoExecutionException {
        String outputFormat = DocConfig.TYPE_HTML;
        File reportFile = new File( String.format( "target/test-extended.%s", outputFormat ) );
        log.info( "output file delete : {}", reportFile.delete() );
        ExecutedTestTagReporterMojo mojo = new ExecutedTestTagReporterMojo() {
            @Override
            public void execute() throws MojoExecutionException {
                this.format = outputFormat;
                this.includeSkipped = Boolean.TRUE;
                this.failOnMissingTag = Boolean.FALSE;
                this.surefireReportsDirectory = new File( "src/test/sample-surefire-reports" );
                this.outputFile = reportFile;
                this.project = new MavenProject();
                this.requiredTags = Arrays.asList( "helper", "not-found" );
                Build build = new Build();
                File testClassesDir = new File( "target/test-classes" );
                build.setTestOutputDirectory(testClassesDir.getAbsolutePath());
                project.setBuild(build);
                super.execute();
            }
        };
        mojo.execute();
        assertTrue(reportFile.exists());
    }

}