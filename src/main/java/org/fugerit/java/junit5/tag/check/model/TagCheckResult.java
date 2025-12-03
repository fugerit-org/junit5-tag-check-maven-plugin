package org.fugerit.java.junit5.tag.check.model;

import org.fugerit.java.core.util.result.BasicResult;

public class TagCheckResult extends BasicResult {

    public TagCheckResult() {
        super( RESULT_CODE_KO );
    }

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
