package com.mc_host.api.exceptions.resources;

public class HetznerException extends RuntimeException {

    public HetznerException(String message, Throwable cause) {
        super(message, cause);
    }
}

