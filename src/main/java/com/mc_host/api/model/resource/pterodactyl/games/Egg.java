package com.mc_host.api.model.resource.pterodactyl.games;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum Egg {
    @EggConfig(filename = "sponge.json", id = 1)
    SPONGE,
    @EggConfig(filename = "vanilla.json", id = 2)
    VANILLA,
    @EggConfig(filename = "paper.json", id = 3)
    PAPER,
    @EggConfig(filename = "bungeecord.json", id = 4)
    BUNGEECORD,
    @EggConfig(filename = "forge.json", id = 5)
    FORGE;
    /*
    @EggConfig(filename = "fabric.json", id = 19)
    FABRIC,
    @EggConfig(filename = "tekkit.json", id = 17)
    TEKKIT,
    @EggConfig(filename = "tekkit-2.json", id = 16)
    TEKKIT_2,
    @EggConfig(filename = "curse-forge.json", id = 18)
    CURSE_FORGE,
    @EggConfig(filename = "bedrock.json", id = 20)
    BEDROCK,
    @EggConfig(filename = "bedrock-ARM64.json", id = 21)
    BEDROCK_ARM64;
    */

    private static final Map<Egg, EggDefinition> definitionCache = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @JsonCreator
    public static Egg fromValue(String value) {
        return Egg.valueOf(value);
    }

    @JsonValue
    public String getName() {
        return this.name();
    }

    public EggDefinition getDefinition() {
        return definitionCache.computeIfAbsent(this, this::loadDefinition);
    }

    public static void preloadAllDefinitions() {
        for (Egg type : values()) {
            type.getDefinition();
        }
    }

    private EggDefinition loadDefinition(Egg eggType) {
        try {
            EggConfig annotation = getEggConfigAnnotation();
            if (annotation == null) {
                throw new IllegalStateException("EggType " + this + " missing @EggConfig annotation");
            }

            var resource = resolver.getResource("classpath:eggs/" + annotation.filename());
            if (!resource.exists()) {
                throw new IllegalStateException("Egg config file not found: " + annotation.filename());
            }

            Map<String, Object> eggJson = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, Object>>() {}
            );

            return buildEgg(annotation, eggJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load egg definition for " + this, e);
        }
    }

    private EggConfig getEggConfigAnnotation() {
        try {
            Field field = Egg.class.getField(this.name());
            return field.getAnnotation(EggConfig.class);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private EggDefinition buildEgg(EggConfig annotation, Map<String, Object> eggJson) {
        Map<String, String> dockerImagesMap = (Map<String, String>) eggJson.get("docker_images");
        String startup = (String) eggJson.get("startup");

        List<Map<String, Object>> variablesJson = (List<Map<String, Object>>) eggJson.get("variables");
        List<EggVariable> variables = variablesJson != null ?
            variablesJson.stream()
                .map(this::buildEggVariable)
                .toList() :
            List.of();

        return new EggDefinition(
            annotation.id(),
            this,
            (String) eggJson.get("name"),
            (String) eggJson.get("description"),
            dockerImagesMap,
            startup,
            variables
        );
    }

    private EggVariable buildEggVariable(Map<String, Object> varJson) {
        return new EggVariable(
            (String) varJson.get("name"),
            (String) varJson.get("description"),
            (String) varJson.get("env_variable"),
            (String) varJson.get("default_value"),
            (Boolean) varJson.get("user_viewable"),
            (Boolean) varJson.get("user_editable")
        );
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EggConfig {
        String filename();
        long id();
    }
}