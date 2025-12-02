package org.fugerit.java.junit5.tag.check;

import org.fugerit.java.junit5.tag.check.facade.TagScanFacade;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

@Order( 3 )
class TagScanFacadeTest {

    @Test
    void testClassNotFound() {
        List<ExecutedTest> executedTests = new ArrayList<>();
        ExecutedTest test = new ExecutedTest( "acme.MyClass", "myMethod", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, BigDecimal.ONE );
        executedTests.add( test );
        Map<ExecutedTest, Set<String>>  testTagMap = TagScanFacade.extractTagsFromExecutedTests( executedTests, Thread.currentThread().getContextClassLoader() );
        Assertions.assertTrue( testTagMap.get( test ).isEmpty() );
    }

    @Test
    void testMethodNotFound() {
        List<ExecutedTest> executedTests = new ArrayList<>();
        ExecutedTest test = new ExecutedTest( TagScanFacadeTest.class.getName(), "myMethod", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, BigDecimal.ONE );
        executedTests.add( test );
        Map<ExecutedTest, Set<String>>  testTagMap = TagScanFacade.extractTagsFromExecutedTests( executedTests, Thread.currentThread().getContextClassLoader() );
        Assertions.assertTrue( testTagMap.get( test ).isEmpty() );
    }

    @Test
    void testTagNotFound() {
        List<ExecutedTest> executedTests = new ArrayList<>();
        ExecutedTest test = new ExecutedTest( ExecutedTest.class.getName(), "testExtended", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, BigDecimal.ONE );
        executedTests.add( test );
        Map<ExecutedTest, Set<String>>  testTagMap = TagScanFacade.extractTagsFromExecutedTests( executedTests, Thread.currentThread().getContextClassLoader() );
        Assertions.assertTrue( testTagMap.get( test ).isEmpty() );
    }

    @Test
    void testClassTagFound() {
        List<ExecutedTest> executedTests = new ArrayList<>();
        ExecutedTest test = new ExecutedTest( ExecutedTestTagReporterMojoExtendedTest.class.getName(), "testExtended", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, BigDecimal.ONE );
        executedTests.add( test );
        Map<ExecutedTest, Set<String>>  testTagMap = TagScanFacade.extractTagsFromExecutedTests( executedTests, Thread.currentThread().getContextClassLoader() );
        Assertions.assertFalse( testTagMap.get( test ).isEmpty() );
    }

}
