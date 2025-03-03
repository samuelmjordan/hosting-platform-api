package com.mc_host.api.exceptions.resources;

public class SshException extends RuntimeException {

    public SshException(String message, Throwable cause) {
        super(message, cause);
    }
}