package io.rp194.aaa.policy;

public interface PolicyEventStore {
  void append(PolicyEvent event);
}
