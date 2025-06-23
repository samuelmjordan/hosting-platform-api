package com.mc_host.api.model.resource.pterodactyl.games;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum Egg {
    SPONGE_MINECRAFT(1L),
    VANILLA_MINECRAFT(2L),
    PAPER_MINECRAFT(3L),
    BUNGEECORD_MINECRAFT(4L),
    FORGE_MINECRAFT(5L);

    private static final Map<Long, Egg> EGG_LOOKUP =
        Arrays.stream(Egg.values())
            .collect(Collectors.toMap(Egg::getId, Function.identity()));

    public final Long id;

    Egg(Long id) {
        this.id = id;
    }

    public static Egg getById(Long id) {
        return EGG_LOOKUP.get(id);
    }

    @JsonCreator
    public static Egg fromValue(String value) {
        return Egg.valueOf(value);
    }

    @JsonValue
    public String getName() {
        return this.name();
    }
}
