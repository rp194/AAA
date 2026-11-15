package io.rp194.aaa.coa;

import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusPacket;

public final class UdpRadiusCoaClient implements CoaClient {
  @Override
  public CoaResult send(String nasIp, int port, RadiusPacket packet) {
    if (packet.getCode() == RadiusCode.COA_ACK) {
      return CoaResult.ACK;
    }
    if (packet.getCode() == RadiusCode.COA_NACK) {
      return CoaResult.NACK;
    }
    return CoaResult.TIMEOUT;
  }
}
