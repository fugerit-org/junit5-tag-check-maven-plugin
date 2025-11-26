package org.fugerit.java.junit5.tag.check.model;

public class ExecutedTest {
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