package io.armadaproject.jenkins.plugin;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.armadaproject.jenkins.plugin.pod.decorator.PodDecorator;
import io.fabric8.kubernetes.api.model.CapabilitiesBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.SeccompProfileBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import java.util.List;
import java.util.logging.Logger;

/**
 * <p>
 * {@link PodDecorator} allowing to inject in all containers a {@code securityContext} allowing to use the
 * {@code restricted} <a href="https://kubernetes.io/docs/concepts/security/pod-security-standards/">Pod Security Standard</a>.
 * </p>
 * <p>
 * See <a href="https://issues.jenkins.io/browse/JENKINS-71639">JENKINS-71639</a> for more details.
 * </p>
 */
@Extension
public class RestrictedPssSecurityContextInjector implements PodDecorator {
    private static final Logger LOGGER = Logger.getLogger(RestrictedPssSecurityContextInjector.class.getName());
    private static final String SECCOMP_RUNTIME_DEFAULT = "RuntimeDefault";
    private static final String CAPABILITIES_ALL = "ALL";

    @NonNull
    @Override
    public Pod decorate(@NonNull ArmadaCloud armadaCloud, @NonNull Pod pod) {
        if (armadaCloud.isRestrictedPssSecurityContext()) {
            var metadata = pod.getMetadata();
            if (metadata == null) {
                // be defensive, this won't happen in real usage
                LOGGER.warning("No metadata found in the pod, skipping the security context update");
                return pod;
            }
            var ns = metadata.getNamespace();
            var name = metadata.getName();
            LOGGER.fine(() -> "Updating pod + " + ns + "/" + name
                    + "  containers security context due to the configured restricted Pod Security Admission");
            var spec = pod.getSpec();
            if (spec == null) {
                // be defensive, this won't happen in real usage
                LOGGER.warning("No spec found in the pod, skipping the security context update");
                return pod;
            }
            var containers = spec.getContainers();
            if (containers != null) {
                for (var container : containers) {
                    var securityContext = container.getSecurityContext();
                    if (securityContext == null) {
                        securityContext = new SecurityContext();
                        container.setSecurityContext(securityContext);
                    }
                    if (securityContext.getAllowPrivilegeEscalation() == null) {
                        securityContext.setAllowPrivilegeEscalation(false);
                    }
                    if (securityContext.getRunAsNonRoot() == null) {
                        securityContext.setRunAsNonRoot(true);
                    }
                    var seccompProfile = securityContext.getSeccompProfile();
                    if (seccompProfile == null) {
                        securityContext.setSeccompProfile(new SeccompProfileBuilder()
                                .withType(SECCOMP_RUNTIME_DEFAULT)
                                .build());
                    }
                    var capabilities = securityContext.getCapabilities();
                    if (capabilities == null) {
                        securityContext.setCapabilities(new CapabilitiesBuilder()
                                .withDrop(List.of(CAPABILITIES_ALL))
                                .build());
                    }
                }
            }
        }
        return pod;
    }
}
