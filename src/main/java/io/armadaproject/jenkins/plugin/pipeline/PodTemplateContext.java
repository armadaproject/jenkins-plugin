package io.armadaproject.jenkins.plugin.pipeline;

import java.io.Serializable;

/**
 * Context object for PodTemplate during pipeline execution
 */
public class PodTemplateContext implements Serializable {
    private static final long serialVersionUID = 3065143885759619305L;

    private final String name;

    public PodTemplateContext(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
