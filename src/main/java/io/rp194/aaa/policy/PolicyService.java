package io.rp194.aaa.policy;

import io.rp194.aaa.accounting.InterimUpdate;
import java.util.Objects;

public final class PolicyService {
  private static final long OCTET_SPACE = 1L << 32;

  private final PolicyStateStore stateStore;
  private final PolicyEventStore eventStore;

  public PolicyService(PolicyStateStore stateStore, PolicyEventStore eventStore) {
    this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    this.eventStore = Objects.requireNonNull(eventStore, "eventStore");
  }

  public CoaAction onInterimUpdate(InterimUpdate update, PackageModel packageModel) {
    PolicyState prior = stateStore.find(update.getTenantId(), update.getSessionId())
        .orElseGet(() -> PolicyState.empty(update.getTenantId(), update.getSessionId()));

    long inDelta = delta(prior.lastInputOctets(), update.getInputOctets());
    long outDelta = delta(prior.lastOutputOctets(), update.getOutputOctets());
    long effectiveUsage = prior.totalUsageOctets() + inDelta + outDelta;

    CoaAction action = decideAction(effectiveUsage, packageModel);
    boolean duplicate = effectiveUsage == prior.lastUsageSnapshotOctets();

    PolicyState next = new PolicyState(
        prior.tenantId(),
        prior.sessionId(),
        effectiveUsage,
        update.getInputOctets(),
        update.getOutputOctets(),
        action,
        effectiveUsage);
    stateStore.upsert(next);

    if (!duplicate && action != prior.lastAction() && action != CoaAction.NONE) {
      eventStore.append(new PolicyEvent(
          update.getTenantId(),
          update.getSessionId(),
          action,
          effectiveUsage,
          packageModel.getQuotaOctets(),
          update.getEventTime()));
    }

    return action;
  }

  private static CoaAction decideAction(long usage, PackageModel packageModel) {
    if (packageModel.getExpiry().isBefore(java.time.Instant.now())) {
      return CoaAction.REDIRECT;
    }
    long quota = packageModel.getQuotaOctets();
    if (usage >= quota) {
      return switch (packageModel.getFupProfile()) {
        case REDIRECT -> CoaAction.REDIRECT;
        case THROTTLE -> CoaAction.DOWNGRADE;
        case NONE -> CoaAction.CAP_REACHED;
      };
    }
    return CoaAction.NONE;
  }

  private static long delta(long previous, long current) {
    if (previous < 0) {
      return current;
    }
    if (current >= previous) {
      return current - previous;
    }
    return (OCTET_SPACE - previous) + current;
  }
}
