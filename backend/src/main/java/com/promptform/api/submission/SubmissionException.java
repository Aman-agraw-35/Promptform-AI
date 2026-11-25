package com.promptform.api.submission;

public class SubmissionException extends RuntimeException {
    public SubmissionException(String message) { super(message); }
    public SubmissionException(String message, Throwable cause) { super(message, cause); }
}
