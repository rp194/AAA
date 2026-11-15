package io.rp194.aaa.coa;

import io.rp194.aaa.radius.RadiusAttribute;
import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CoaPacketBuilder {
  private final DefaultVendorEnforcementMapperRegistry mapperRegistry;

  public CoaPacketBuilder(DefaultVendorEnforcementMapperRegistry mapperRegistry) {
    this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry");
  }

  public RadiusPacket build(CoaAction action, int identifier) {
    List<RadiusAttribute> attributes = new ArrayList<>();
    attributes.add(new RadiusAttribute("User-Name", action.getUsername()));
    attributes.add(new RadiusAttribute("Acct-Session-Id", action.getSessionId()));
    attributes.add(new RadiusAttribute("Framed-IP-Address", action.getFramedIpAddress()));
    addIfPresent(attributes, "NAS-Port", action.getNasPort());
    addIfPresent(attributes, "NAS-Port-Id", action.getNasPortId());
    addIfPresent(attributes, "Calling-Station-Id", action.getCallingStationId());
    attributes.addAll(mapperRegistry.mapperFor(action.getVendorType()).enforcementAttributes(action));
    return new RadiusPacket(RadiusCode.COA_REQUEST, identifier, attributes);
  }

  private void addIfPresent(List<RadiusAttribute> attributes, String name, String value) {
    if (value != null && !value.isBlank()) {
      attributes.add(new RadiusAttribute(name, value));
    }
  }
}
