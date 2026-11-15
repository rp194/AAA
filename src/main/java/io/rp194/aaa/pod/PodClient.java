package io.rp194.aaa.pod;

import io.rp194.aaa.radius.RadiusPacket;

@FunctionalInterface
public interface PodClient {
  PodResult send(String nasIp, int port, RadiusPacket packet);
}
