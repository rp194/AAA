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
    attributes.addAll(mapperRegistry.mapperFor(action.getVendorType()).enforcementAttributes(action.getActionType()));
    return new RadiusPacket(RadiusCode.COA_REQUEST, identifier, attributes);
  }
}
