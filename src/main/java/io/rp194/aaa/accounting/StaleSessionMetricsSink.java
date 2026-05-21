package io.rp194.aaa.accounting;

public interface StaleSessionMetricsSink {
  void emit(SessionStaleMetric metric);
}
