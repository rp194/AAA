package io.rp194.aaa.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryPostgresPolicyEventStore implements PolicyEventStore {
  private final List<PolicyEvent> events = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void append(PolicyEvent event) {
    events.add(event);
  }

  public List<PolicyEvent> all() {
    return List.copyOf(events);
  }
}
