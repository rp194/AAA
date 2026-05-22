package io.rp194.aaa.server;

public interface AccessMetrics {
  AccessMetrics NOOP = new AccessMetrics() {
    @Override public void recordAccept() { }
    @Override public void recordReject() { }
  };

  void recordAccept();
  void recordReject();
}
