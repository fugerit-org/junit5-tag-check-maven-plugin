package org.fugerit.java.junit5.tag.check.facade;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.fugerit.java.core.util.result.Result;
import org.fugerit.java.junit5.tag.check.model.ExecutedTest;
import org.fugerit.java.junit5.tag.check.model.TagCheckResult;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TagCheckFacade {

    private TagCheckFacade() {}

    public static TagCheckResult checkHelper(Collection<String> requiredTags, Map<ExecutedTest, Set<String>> testTagMap) {
        TagCheckResult result = new TagCheckResult();
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
            result.setResultCode( Result.RESULT_CODE_KO );
            result.setMessage( String.format( "Missing required tags in executed tests: %s", String.join(", ", missingTags) ) );
        } else {
            result.setResultCode( Result.RESULT_CODE_OK );
            result.setMessage( String.format( "All required tags found in executed tests: %s", String.join(", ", requiredTags) ) );
        }
        return result;
    }

    public static void checkRequiredTags(Collection<String> requiredTags, boolean failOnMissingTag, Map<ExecutedTest, Set<String>> testTagMap) throws MojoExecutionException {
        TagCheckResult result = checkHelper(requiredTags, testTagMap);
        if ( result.isResultOk() ) {
            log.info( "Tag check [OK] %s", result.getMessage() );
        } else {
            if (failOnMissingTag) {
                throw new MojoExecutionException( result.getMessage() );
            } else {
                LogUtils.logWarningInBox( Arrays.asList( String.format( "Tag check [KO] %s", result.getMessage() ) ) );
            }
        }
    }

}
