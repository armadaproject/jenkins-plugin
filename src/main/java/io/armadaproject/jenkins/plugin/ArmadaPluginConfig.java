package io.armadaproject.jenkins.plugin;

/**
 * Centralized configuration constants for the Armada Jenkins plugin. All timeouts, defaults, and
 * system properties are defined here.
 */
public class ArmadaPluginConfig {

  // ===== Launcher Configuration =====

  /**
   * Default timeout in seconds for agent connection (10 minutes)
   */
  public static final int DEFAULT_SLAVE_CONNECT_TIMEOUT = 600;

  /**
   * Interval in seconds for polling job status
   */
  public static final int POLL_INTERVAL_SECONDS = 1;

  /**
   * Interval in milliseconds for polling agent connection (1 second)
   */
  public static final long AGENT_CONNECTION_POLL_INTERVAL_MS = 1000L;

  /**
   * Interval in milliseconds for reporting progress (30 seconds)
   */
  public static final long REPORT_INTERVAL_MS = 30_000L;

  // ===== Websocket Configuration =====
  /**
   * Time in milliseconds to wait for checking whether the process immediately returned
   */
  public static final int COMMAND_FINISHED_TIMEOUT_MS = 200;
  /**
   * Environment variable name for Jenkins server cookie (process tracking)
   */
  public static final String COOKIE_VAR = "JENKINS_SERVER_COOKIE";
  /**
   * Shell exit command
   */
  public static final String EXIT = "exit";
  /**
   * Newline character
   */
  public static final String NEWLINE = "\n";
  /**
   * Ctrl+C character for killing processes
   */
  public static final char CTRL_C = '\u0003';
  /**
   * Default shell command for Unix systems
   */
  public static final String DEFAULT_SHELL = "sh";
  /**
   * Environment variable name validation regex
   */
  public static final String ENV_VAR_NAME_REGEX = "[a-zA-Z_][a-zA-Z0-9_]*";
  /**
   * Windows newline
   */
  public static final String WINDOWS_NEWLINE = "\r\n";
  /**
   * Unix newline
   */
  public static final String UNIX_NEWLINE = "\n";

  // ===== Shell and Environment Configuration =====
  /**
   * Windows environment variable set format
   */
  public static final String WINDOWS_ENV_SET_FORMAT = "set %s=%s";
  /**
   * Unix environment variable export format
   */
  public static final String UNIX_ENV_EXPORT_FORMAT = "export %s='%s'";
  /**
   * Jenkins secret environment variable
   */
  public static final String JENKINS_SECRET_ENV = "JENKINS_SECRET";
  /**
   * Suffix appended to the agent name to form the per-agent JNLP Secret name.
   */
  public static final String JNLP_SECRET_NAME_SUFFIX = "-jnlp-secret";
  /**
   * Timeout in seconds for the executor-cluster Pod object to appear with a UID after Armada
   * has reported JobPending. Once pending fires the Pod is created on the executor cluster
   * almost immediately, so this is intentionally much shorter than EVENT_WAIT_TIMEOUT_SECONDS.
   */
  public static final int POD_UID_WAIT_TIMEOUT_SECONDS = 30;
  /**
   * Key under which the JNLP HMAC is stored inside the per-agent Secret.
   */
  public static final String JNLP_SECRET_KEY = "jenkins-secret";
  /**
   * Jenkins name environment variable
   */
  public static final String JENKINS_NAME_ENV = "JENKINS_NAME";
  /**
   * Jenkins agent name environment variable
   */
  public static final String JENKINS_AGENT_NAME_ENV = "JENKINS_AGENT_NAME";
  /**
   * Jenkins agent work directory environment variable
   */
  public static final String JENKINS_AGENT_WORKDIR_ENV = "JENKINS_AGENT_WORKDIR";
  /**
   * Jenkins URL environment variable
   */
  public static final String JENKINS_URL_ENV = "JENKINS_URL";
  /**
   * Jenkins remoting options environment variable
   */
  public static final String REMOTING_OPTS_ENV = "REMOTING_OPTS";
  /**
   * JNLP container name
   */
  public static final String JNLP_CONTAINER_NAME = "jnlp";
  /**
   * Default Jenkins inbound agent Docker image
   */
  public static final String DEFAULT_AGENT_IMAGE = "jenkins/inbound-agent:latest";

  // ===== Jenkins Environment Variable Names =====
  /**
   * Default working directory for Jenkins agent
   */
  public static final String DEFAULT_WORKING_DIR = "/home/jenkins/agent";
  /**
   * Default remote filesystem path for agent
   */
  public static final String DEFAULT_REMOTE_FS = "/home/jenkins/agent";
  /**
   * Workspace volume name
   */
  public static final String WORKSPACE_VOLUME_NAME = "workspace-volume";
  /**
   * Number of executors per agent
   */
  public static final int NUM_EXECUTORS = 1;
  /**
   * Default CPU request for JNLP container
   */
  public static final String DEFAULT_JNLP_CONTAINER_CPU_REQUEST = "256m";
  /**
   * Default CPU limit for JNLP container
   */
  public static final String DEFAULT_JNLP_CONTAINER_CPU_LIMIT = "256m";

  // ===== Pod/Container Defaults =====
  /**
   * Default memory request for JNLP container
   */
  public static final String DEFAULT_JNLP_CONTAINER_MEMORY_REQUEST = "256Mi";
  /**
   * Default memory limit for JNLP container
   */
  public static final String DEFAULT_JNLP_CONTAINER_MEMORY_LIMIT = "256Mi";
  /**
   * No reconnect after timeout setting for JNLP agent
   */
  public static final String NO_RECONNECT_AFTER_TIMEOUT = "1d";
  /**
   * Default retention timeout in minutes for agents
   */
  public static final int DEFAULT_RETENTION_TIMEOUT_MINUTES = 5;
  /**
   * Timeout in seconds for waiting for Armada JobRunningEvent
   */
  public static final int EVENT_WAIT_TIMEOUT_SECONDS = 60;
  /**
   * Default Kubernetes namespace
   */
  public static final String DEFAULT_NAMESPACE = "default";

  // ===== Resource Defaults =====
  /**
   * Date format pattern for job set ID generation
   */
  public static final String JOB_SET_DATE_FORMAT = "-ddMMyyyy";
  /**
   * Interval in minutes for periodic timer tasks
   */
  public static final long TIMER_INTERVAL_MINUTES = 1L;
  private static final String WEBSOCKET_CONNECTION_MAX_RETRY_SYSTEM_PROPERTY =
      ArmadaPluginConfig.class.getPackageName() + ".websocketConnectionMaxRetries";
  /**
   * Maximum number of times to retry failed websocket connection
   */
  public static final int WEBSOCKET_CONNECTION_MAX_RETRY =
      Integer.getInteger(WEBSOCKET_CONNECTION_MAX_RETRY_SYSTEM_PROPERTY, 5);

  // ===== Agent Configuration =====
  private static final String WEBSOCKET_CONNECTION_MAX_RETRY_BACKOFF_SYSTEM_PROPERTY =
      ArmadaPluginConfig.class.getPackageName() + ".websocketConnectionMaxRetryBackoff";
  /**
   * Maximum backoff time in seconds for retrying failed websocket connection
   */
  public static final int WEBSOCKET_CONNECTION_MAX_RETRY_BACKOFF =
      Integer.getInteger(WEBSOCKET_CONNECTION_MAX_RETRY_BACKOFF_SYSTEM_PROPERTY, 30);

  // ===== Event Management =====
  private static final String WEBSOCKET_CONNECTION_TIMEOUT_SYSTEM_PROPERTY =
      ArmadaPluginConfig.class.getPackageName() + ".websocketConnectionTimeout";

  // ===== Kubernetes Defaults =====
  /**
   * Time to wait in seconds for websocket to connect
   */
  public static final int WEBSOCKET_CONNECTION_TIMEOUT =
      Integer.getInteger(WEBSOCKET_CONNECTION_TIMEOUT_SYSTEM_PROPERTY, 30);

  // ===== Date Formatting =====
  private static final String STDIN_BUFFER_SIZE_SYSTEM_PROPERTY =
      ArmadaPluginConfig.class.getPackageName() + ".stdinBufferSize";

  // ===== Timer Configuration =====
  /**
   * Stdin buffer size in bytes for commands sent to Kubernetes exec API (16KB)
   */
  public static final int STDIN_BUFFER_SIZE =
      Integer.getInteger(STDIN_BUFFER_SIZE_SYSTEM_PROPERTY, 16 * 1024);

  // ===== Private Constructor =====

  private ArmadaPluginConfig() {
    // Utility class - prevent instantiation
  }
}
