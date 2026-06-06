package com.labassist.common.exception;

/** Thrown on a uniqueness/state conflict (e.g. duplicate username); mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
