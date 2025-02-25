package com.mc_host.api.model.pterodactyl.games;

import lombok.Getter;

@Getter
public enum Nest {
    MINECRAFT(1);

    public final Integer id;

    Nest(Integer id) {
        this.id = id;
    }
}
