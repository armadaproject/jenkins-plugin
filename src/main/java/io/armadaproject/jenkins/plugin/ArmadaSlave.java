package io.armadaproject.jenkins.plugin;

import api.SubmitOuterClass.CancellationResult;
import api.SubmitOuterClass.JobCancelRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudSlave;
import io.armadaproject.ArmadaClient;
import java.io.IOException;
import java.io.Serial;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.durabletask.executors.OnceRetentionStrategy;

public class ArmadaSlave extends AbstractCloudSlave {

  @Serial
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(ArmadaSlave.class.getName());

  private final String cloudName;

  @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED",
      justification = "The template holds the build's TaskListener and is deliberately not "
          + "persisted; an agent deserialized after a controller restart has none, which "
          + "_terminate() handles")
  private transient final ArmadaJobTemplate template;

  public ArmadaSlave(
      @Nonnull ArmadaCloud cloud,
      @Nonnull ArmadaJobTemplate template) throws Descriptor.FormException, IOException {

    super(
        template.getLabel(),
        null,
        ArmadaPluginConfig.DEFAULT_REMOTE_FS,
        ArmadaPluginConfig.NUM_EXECUTORS,
        Mode.NORMAL,
        template.getLabel() != null ? template.getLabel() : "",
        new ArmadaLauncher(),
        new OnceRetentionStrategy(cloud.getRetentionTimeout()),
        Collections.emptyList()
    );

    this.cloudName = cloud.name;
    this.template = template;
    template.getListener().getLogger()
        .println("Created slave for label: " + template.getLabel());
  }

  public static String generateLabel(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }

  public String getCloudName() {
    return cloudName;
  }

  public ArmadaJobTemplate getTemplate() {
    return template;
  }

  @Override
  public ArmadaComputer createComputer() {
    return new ArmadaComputer(this);
  }

  @Override
  protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
    // After a controller restart the template is gone, so fall back to the caller's listener.
    TaskListener effectiveListener = template != null ? template.getListener() : listener;
    effectiveListener.getLogger().println(
        "Terminating agent: " + (template != null ? template.getLabel() : getNodeName()));

    // Cancel the job via Armada API
    try {
      cancelArmadaJob(effectiveListener);
    } catch (Exception e) {
      String message = "Failed to terminate job: " + e.getMessage();
      LOGGER.severe(message);
      listener.error(message).close();
    }
  }

  private void cancelArmadaJob(TaskListener listener) throws IOException {
    try {
      // Get cloud configuration
      ArmadaCloud cloud = ArmadaCloud.resolveCloud(cloudName);

      // Get job ID from computer
      ArmadaComputer computer = (ArmadaComputer) getComputer();
      if (computer == null) {
        listener.getLogger().println("Computer is null, cannot cancel job");
        LOGGER.warning("Computer is null, cannot retrieve job ID for cancellation");
        return;
      }

      String jobId = computer.getArmadaJobId();
      String jobSetId = computer.getArmadaJobSetId();

      if (jobId == null || jobId.isEmpty()) {
        String message = "No job ID found, skipping cancellation";
        listener.getLogger().println(message);
        LOGGER.warning(message);
        return;
      }

      listener.getLogger().println("Cancelling job with id: " + jobId);

      // Cancel the job via Armada API
      try (ArmadaClient armadaClient = cloud.createArmadaClient()) {

        JobCancelRequest cancelRequest =
            JobCancelRequest.newBuilder()
                .setJobId(jobId)
                .setQueue(cloud.getArmadaQueue())
                .setJobSetId(
                    jobSetId != null && !jobSetId.isEmpty() ? jobSetId : cloud.getArmadaJobSetId())
                .build();

        CancellationResult result = armadaClient.cancelJob(cancelRequest);

        if (result != null && result.getCancelledIdsCount() > 0) {
          String message = "Job successfully cancelled";
          listener.getLogger().println(message);
          LOGGER.fine(message);
        } else {
          String message = "Job cancellation completed (may have already finished)";
          listener.getLogger().println(message);
          LOGGER.warning(message);
        }
      }

    } catch (Exception e) {
      String message = "Failed to cancel job: " + e.getMessage();
      LOGGER.severe(message);
      listener.error(message).close();
      throw new IOException("Failed to cancel Armada job", e);
    }
  }
}
