package com.enrichmeai.ref.platform;

import org.springframework.http.HttpStatus;

/**
 * The errors a service throws, as factory methods: {@code throw Errors.notFound("UNKNOWN_ZONE", "No zone Z");}
 * A concrete API leaves fewer ways to be wrong, for people and for models. Our first generation run, with only the
 * abstract DomainException to extend, failed every attempt (docs/evidence/run-before-errors-helper).
 */
public final class Errors {

    /** 404: the thing asked for does not exist. */
    public static DomainException notFound(String code, String detail) {
        return new PlatformError(HttpStatus.NOT_FOUND, code, detail);
    }

    /** 422: the request is well formed but breaks a business rule. */
    public static DomainException unprocessable(String code, String detail) {
        return new PlatformError(HttpStatus.UNPROCESSABLE_CONTENT, code, detail);
    }

    /** 409: the request conflicts with the current state. */
    public static DomainException conflict(String code, String detail) {
        return new PlatformError(HttpStatus.CONFLICT, code, detail);
    }

    static final class PlatformError extends DomainException {
        PlatformError(HttpStatus status, String code, String detail) {
            super(status, code, detail);
        }
    }

    private Errors() {}
}
