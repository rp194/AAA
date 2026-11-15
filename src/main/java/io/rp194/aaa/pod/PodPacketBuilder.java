package io.rp194.aaa.pod;

import io.rp194.aaa.radius.RadiusAttribute;
import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusPacket;
import java.util.ArrayList;
import java.util.List;

public final class PodPacketBuilder {
  public RadiusPacket build(PodAction action, int identifier) {
    List<RadiusAttribute> attributes = new ArrayList<>();
    attributes.add(new RadiusAttribute("User-Name", action.getUsername()));
    attributes.add(new RadiusAttribute("Acct-Session-Id", action.getSessionId()));
    addIfPresent(attributes, "Framed-IP-Address", action.getFramedIpAddress());
    addIfPresent(attributes, "NAS-Port", action.getNasPort());
    addIfPresent(attributes, "NAS-Port-Id", action.getNasPortId());
    addIfPresent(attributes, "Calling-Station-Id", action.getCallingStationId());
    return new RadiusPacket(RadiusCode.DISCONNECT_REQUEST, identifier, attributes);
  }

  private void addIfPresent(List<RadiusAttribute> attributes, String name, String value) {
    if (value != null && !value.isBlank()) {
      attributes.add(new RadiusAttribute(name, value));
    }
  }
}
