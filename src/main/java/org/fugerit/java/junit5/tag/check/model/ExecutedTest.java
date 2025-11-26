package org.fugerit.java.junit5.tag.check.model;

/*

    {
      "class": "test.org.fugerit.java.demo.unittestdemoapp.DocResourceSicurezzaTest",
      "method": "testHtmlOkNoAdminRole",
      "time": 1.37,
      "skipped": false,
      "failed": false,
      "error": false,
      "tags": ["security", "authorized"]
    },

 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"class", "method", "time", "skipped", "failed", "error","tags"})
public class ExecutedTest {

    private final String className;
    private final String methodName;
    private final boolean skipped;
    private final boolean failed;
    private final boolean error;
    private final BigDecimal time;

    private List<String> tags;

    public ExecutedTest(String className, String methodName,
                        boolean skipped, boolean failed, boolean error, BigDecimal time) {
        this.className = className;
        this.methodName = methodName;
        this.skipped = skipped;
        this.failed = failed;
        this.error = error;
        this.time = time;
        this.tags = new ArrayList<>();
    }

    @JsonProperty( "class" )
    public String getClassName() { return className; }

    @JsonProperty( "method" )
    public String getMethodName() { return methodName; }

    public boolean isSkipped() { return skipped; }

    public boolean isFailed() { return failed; }

    public boolean isError() { return error; }

    public BigDecimal getTime() { return time; }

    public List<String> getTags() { return tags; }

}