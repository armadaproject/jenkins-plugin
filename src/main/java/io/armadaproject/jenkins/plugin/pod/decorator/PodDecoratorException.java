package io.armadaproject.jenkins.plugin.pod.decorator;

/**
 * A fatal exception raised by a {@link PodDecorator} implementation.
 */
public class PodDecoratorException extends RuntimeException {
    public PodDecoratorException(String message) {
        super(message);
    }

    public PodDecoratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
