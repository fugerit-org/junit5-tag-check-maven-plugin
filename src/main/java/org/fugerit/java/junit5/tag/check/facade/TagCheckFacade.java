package org.fugerit.java.junit5.tag.check.facade;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TagCheckFacade {

    private TagCheckFacade() {}

    public static void checkRequiredTags(Collection<String> requiredTags, boolean failOnMissingTag, Map<ExecutedTest, Set<String>> testTagMap)
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
                LogUtils.logWarningInBox( Arrays.asList( message ) );
            }
        } else {
            log.info("All required tags found in executed tests: {}",
                    String.join(", ", requiredTags));
        }
    }



}
