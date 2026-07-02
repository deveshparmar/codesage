package com.deveshparmar.codesage.scm.domain;

public record ScmAccessToken(String value) {

    public ScmAccessToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SCM access token must not be blank");
        }
    }
}
