package com.promptform.api.form;

public class FormException extends RuntimeException {
    public FormException(String message) { super(message); }
    public FormException(String message, Throwable cause) { super(message, cause); }
}
