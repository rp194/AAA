package io.rp194.aaa.transport;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WorkerPool {
  private final ThreadPoolExecutor executor;
  private final OverloadPolicy overloadPolicy;

  public WorkerPool(int workers, int queueCapacity, OverloadPolicy overloadPolicy, boolean useVirtualThreads) {
    this.overloadPolicy = Objects.requireNonNull(overloadPolicy, "overloadPolicy");
    this.executor = new ThreadPoolExecutor(
        workers,
        workers,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(queueCapacity),
        r -> {
          Thread t = new Thread(r);
          t.setName("radius-worker-" + t.getId() + (useVirtualThreads ? "-vt-hint" : ""));
          return t;
        },
        new ThreadPoolExecutor.AbortPolicy());
  }

  public boolean submit(Runnable task) {
    try {
      executor.execute(task);
      return true;
    } catch (RejectedExecutionException ignored) {
      return false;
    }
  }

  public int queueDepth() { return executor.getQueue().size(); }
  public int queueCapacity() { return queueDepth() + executor.getQueue().remainingCapacity(); }

  public void shutdown() { executor.shutdown(); }
}
