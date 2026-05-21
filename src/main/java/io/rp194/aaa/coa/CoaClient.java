package io.rp194.aaa.coa;

import io.rp194.aaa.radius.RadiusPacket;

public interface CoaClient {
  CoaResult send(String nasIp, int port, RadiusPacket packet);
}
