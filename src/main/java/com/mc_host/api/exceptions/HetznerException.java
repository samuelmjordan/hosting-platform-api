package com.mc_host.api.exceptions;

public class HetznerException extends Exception {
    public HetznerException(String message) {
        super(message);
    }

    public HetznerException(String message, Throwable cause) {
        super(message, cause);
    }
}
