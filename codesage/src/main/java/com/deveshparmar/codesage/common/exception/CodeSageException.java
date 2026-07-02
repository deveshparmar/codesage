package com.deveshparmar.codesage.common.exception;

public class CodeSageException extends RuntimeException {

    public CodeSageException(String message) {
        super(message);
    }

    public CodeSageException(String message, Throwable cause) {
        super(message, cause);
    }
}
