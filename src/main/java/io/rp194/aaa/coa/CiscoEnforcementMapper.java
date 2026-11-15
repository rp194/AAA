package io.rp194.aaa.coa;

import io.rp194.aaa.radius.RadiusAttribute;
import java.util.List;

public final class CiscoEnforcementMapper implements VendorEnforcementMapper {
  @Override
  public List<RadiusAttribute> enforcementAttributes(CoaAction action) {
    return switch (action.getActionType()) {
      case CAP_REACHED -> List.of(new RadiusAttribute("Cisco-AVPair", "subscriber:command=activate-service level-capped"));
      case PLAN_UPGRADE -> List.of(new RadiusAttribute("Cisco-AVPair", "subscriber:command=activate-service level-upgraded"));
      case SUSPEND -> List.of(new RadiusAttribute("Cisco-AVPair", "subscriber:command=deactivate-service"));
      case WALLED_GARDEN -> List.of(new RadiusAttribute("Cisco-AVPair", "subscriber:command=activate-service walled-garden"));
    };
  }
}
