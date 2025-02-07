package com.mc_host.api.exceptions;

public class ClerkException extends Exception {
    public ClerkException(String message) {
        super(message);
    }

    public ClerkException(String message, Throwable cause) {
        super(message, cause);
    }
}
