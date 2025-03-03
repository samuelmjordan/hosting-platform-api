package com.mc_host.api.exceptions.resources;

public class CloudflareException extends RuntimeException {

    public CloudflareException(String message, Throwable cause) {
        super(message, cause);
    }
}
