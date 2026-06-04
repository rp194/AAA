package io.rp194.aaa.pod;

import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusPacket;

public final class UdpRadiusPodClient implements PodClient {
  @Override
  public PodResult send(String nasIp, int port, RadiusPacket packet) {
    if (packet.getCode() == RadiusCode.DISCONNECT_ACK) {
      return PodResult.ACK;
    }
    if (packet.getCode() == RadiusCode.DISCONNECT_NACK) {
      return PodResult.NACK;
    }
    return PodResult.TIMEOUT;
  }
}
