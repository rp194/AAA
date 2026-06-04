package io.rp194.aaa.pod;

@FunctionalInterface
public interface PodService {
  PodService NOOP = action -> PodResult.TIMEOUT;

  PodResult disconnect(PodAction action);
}
