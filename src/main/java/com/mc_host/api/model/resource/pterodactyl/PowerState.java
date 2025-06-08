package com.mc_host.api.model.resource.pterodactyl;

public enum PowerState {
    START,
    STOP,
    KILL,
    RESTART;

    public static PowerState lookup(String string)  {
        try {
            return PowerState.valueOf(string.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("No PowerState for string '%s'", string));
        }
    }

    public String toString() {
        return this.name().toLowerCase();
    }
}
