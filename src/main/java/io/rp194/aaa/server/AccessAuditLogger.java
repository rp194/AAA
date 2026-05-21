package io.rp194.aaa.server;

public interface AccessAuditLogger {
  void log(AccessAuditEvent event);

  AccessAuditLogger NOOP = event -> {
  };
}
