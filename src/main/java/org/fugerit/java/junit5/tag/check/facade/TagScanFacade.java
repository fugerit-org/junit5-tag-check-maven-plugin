package org.fugerit.java.junit5.tag.check.facade;

import lombok.extern.slf4j.Slf4j;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;
import org.junit.jupiter.api.Tag;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class TagScanFacade {

    private TagScanFacade() {}

    public static Map<ExecutedTest, Set<String>> extractTagsFromExecutedTests(
            List<ExecutedTest> executedTests,
            ClassLoader classLoader) {
        Map<ExecutedTest, Set<String>> testTagMap = new LinkedHashMap<>();
        for (ExecutedTest test : executedTests) {
            try {
                Class<?> testClass = classLoader.loadClass(test.getClassName());
                Method testMethod = findTestMethod(testClass, test.getMethodName());
                if (testMethod != null) {
                    Set<String> tags = getMethodTag(testMethod, testClass);
                    testTagMap.put(test, tags);
                } else {
                    log.warn("Could not find method: {} #{}", test.getClassName(), test.getMethodName());
                    testTagMap.put(test, Collections.emptySet());
                }
            } catch (ClassNotFoundException e) {
                log.warn("Could not load test class: {}", test.getClassName());
                testTagMap.put(test, Collections.emptySet());
            }
        }
        return testTagMap;
    }

    private static Set<String> getMethodTag(Method testMethod, Class<?> testClass) {
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
        return tags;
    }

    private static Method findTestMethod(Class<?> testClass, String methodName) {
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

}
