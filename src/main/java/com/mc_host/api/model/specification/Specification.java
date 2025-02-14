package com.mc_host.api.model.specification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = JavaServerSpecification.class, name = "JAVA_SERVER")
})
public sealed interface Specification permits JavaServerSpecification{
    String specification_id();
    String title();
    String description();
    SpecificationType type();
}