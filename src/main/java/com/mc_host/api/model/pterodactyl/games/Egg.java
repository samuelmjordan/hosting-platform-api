package com.mc_host.api.model.pterodactyl.games;

import lombok.Getter;

@Getter
public enum Egg {
    SPONGE_MINECRAFT(1),
    VANILLA_MINECRAFT(2),
    PAPER_MINECRAFT(3),
    BUNGEECORD_MINECRAFT(4),
    FORGE_MINECRAFT(5);

    public final Integer id;

    Egg(Integer id) {
        this.id = id;
    }
}
