package io.rp194.aaa.accounting;

public interface StaleSessionAlerter {
  void alert(SessionStaleMetric metric);
}
