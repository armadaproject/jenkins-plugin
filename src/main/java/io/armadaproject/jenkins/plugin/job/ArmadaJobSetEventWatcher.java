package io.armadaproject.jenkins.plugin.job;

import api.EventOuterClass;
import com.sun.istack.NotNull;
import io.armadaproject.ArmadaClient;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ArmadaJobSetEventWatcher implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ArmadaJobSetEventWatcher.class.getName());

    public interface ArmadaMessageCallback {
        void onMessage(EventOuterClass.EventStreamMessage eventStreamMessage);
    }

    private final Object cancellationLock = new Object();
    private final AtomicReference<Context.CancellableContext> innerContextRef = new AtomicReference<>();
    private final AtomicBoolean reconnect = new AtomicBoolean(false);
    private final AtomicBoolean waitForReconnect = new AtomicBoolean(false);
    private final ArmadaClientProvider armadaClientProvider;
    private final ArmadaMessageCallback messageCallback;
    private volatile String queue;
    private final String jobSetId;
    private final String fromMessageId;

    public ArmadaJobSetEventWatcher(ArmadaClientProvider armadaClientProvider, ArmadaMessageCallback messageCallback, String queue, String jobSetId, String fromMessageId) {
        this.armadaClientProvider = armadaClientProvider;
        this.messageCallback = messageCallback;
        this.queue = queue;
        this.jobSetId = jobSetId;
        this.fromMessageId = fromMessageId;
    }

    public void close() {
        synchronized (cancellationLock) {
            cancellationLock.notify();
        }
    }

    public void forceReconnect(String queue, boolean invalidConfig) {
        this.queue = queue;
        reconnect.set(true);
        waitForReconnect.set(invalidConfig);
        var innerContext = innerContextRef.get();
        if(innerContext != null) {
            innerContext.cancel(null);
        }
    }

    @Override
    public void run() {
        try(var cancellableContext = Context.current().withCancellation()) {
            var cancelThread = new Thread(() -> {
                try {
                    synchronized (cancellationLock) {
                        cancellationLock.wait();
                    }
                } catch (InterruptedException e) {
                    // left empty
                }
                cancellableContext.cancel(null);
            });
            cancelThread.setDaemon(true);
            cancelThread.start();

            cancellableContext.run(() -> {
                final AtomicReference<String> fromMessageId = new AtomicReference<>(this.fromMessageId);
                var requestBuilder = EventOuterClass.JobSetRequest.newBuilder()
                        .setId(jobSetId)
                        .setErrorIfMissing(false)
                        .setWatch(true);

                while(!cancellableContext.isCancelled()) {
                    try(var client = getClient()) {
                        var builder = requestBuilder.setQueue(queue);
                        var msgId = fromMessageId.get();
                        if(msgId != null) {
                            builder = builder.setFromMessageId(msgId);
                        }
                        LOGGER.info("Starting to stream events for queue " + queue + " and jobSetId " + jobSetId);

                        final var completed = new CountDownLatch(1);
                        final var error = new AtomicReference<Throwable>();
                        final var innerContext = cancellableContext.withCancellation();
                        innerContextRef.set(innerContext);
                        final var request = builder.build();

                        innerContext.run(() -> {
                            if(waitForReconnect.get()) {
                                LOGGER.info("Config invalidated, waiting for reconnect...");
                                var cancelled = new CountDownLatch(1);
                                Context.CancellationListener cancellationListener = context -> {
                                    cancelled.countDown();
                                };
                                innerContext.addListener(cancellationListener, runnable -> {
                                    cancelled.countDown();
                                });
                                try {
                                    cancelled.await();
                                } catch (InterruptedException e) {
                                    // left empty
                                }
                                innerContext.removeListener(cancellationListener);
                            } else {

                                client.streamEvents(request, new StreamObserver<>() {
                                    @Override
                                    public void onNext(EventOuterClass.EventStreamMessage eventStreamMessage) {
                                        try {
                                            messageCallback.onMessage(eventStreamMessage);
                                        } catch (Throwable e) {
                                            // avoid throwing here by all costs, it kills the grpc cancellable and this whole thing falls apart
                                        }
                                        fromMessageId.set(eventStreamMessage.getId());
                                    }

                                    @Override
                                    public void onError(Throwable throwable) {
                                        var isError = true;
                                        if (throwable instanceof StatusRuntimeException) {
                                            var status = (StatusRuntimeException) throwable;
                                            isError = status.getStatus().getCode() != Status.Code.CANCELLED;
                                        }
                                        if (isError) {
                                            LOGGER.log(Level.SEVERE, "Failed to stream events for queue " + queue + " and jobSetId " + jobSetId, throwable);
                                            error.set(throwable);
                                        }
                                        completed.countDown();
                                    }

                                    @Override
                                    public void onCompleted() {
                                        LOGGER.info("Finished streaming events for queue " + queue + " and jobSetId " + jobSetId);
                                        completed.countDown();
                                    }
                                });
                                try {
                                    if (reconnect.compareAndExchange(true, false)) {
                                        innerContext.cancel(null);
                                    }
                                    completed.await();

                                    var lastError = error.get();
                                    if (lastError != null) {
                                        LOGGER.log(Level.WARNING, "error while listening to armada events", lastError);
                                        if (lastError instanceof StatusRuntimeException) {
                                            var statusEx = (StatusRuntimeException) lastError;
                                            var code = statusEx.getStatus().getCode();
                                            LOGGER.info("Failed to watch armada jobset " + jobSetId + " code: " + code + " ...waiting before retrying...");
                                            Thread.sleep(5000);
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    LOGGER.log(Level.SEVERE, "Thread interrupted while waiting for stream completion");
                                }
                                LOGGER.info("Finished streaming events for queue " + queue + (!cancellableContext.isCancelled() ? " reconnecting..." : ""));
                            }
                        });
                    }
                }
            });
        }
    }

    private ArmadaClient getClient() {
        try {
            return armadaClientProvider.get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
