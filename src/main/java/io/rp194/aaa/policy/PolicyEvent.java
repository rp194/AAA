package io.rp194.aaa.policy;

import java.time.Instant;

public record PolicyEvent(
    String tenantId,
    String sessionId,
    CoaAction action,
    long usageOctets,
    long quotaOctets,
    Instant eventTime) {
}
