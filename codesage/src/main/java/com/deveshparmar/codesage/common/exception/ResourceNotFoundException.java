package com.deveshparmar.codesage.common.exception;

public class ResourceNotFoundException extends CodeSageException {

    public ResourceNotFoundException(String resource, String identifier) {
        super("%s not found: %s".formatted(resource, identifier));
    }
}
