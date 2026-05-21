package io.rp194.aaa.coa;

import io.rp194.aaa.radius.RadiusAttribute;
import java.util.List;

public final class MikroTikEnforcementMapper implements VendorEnforcementMapper {
  @Override
  public List<RadiusAttribute> enforcementAttributes(CoaActionType actionType) {
    return switch (actionType) {
      case CAP_REACHED -> List.of(new RadiusAttribute("Mikrotik-Rate-Limit", "1M/1M"));
      case PLAN_UPGRADE -> List.of(new RadiusAttribute("Mikrotik-Rate-Limit", "50M/50M"));
      case SUSPEND -> List.of(new RadiusAttribute("Mikrotik-Address-List", "suspended"));
      case WALLED_GARDEN -> List.of(new RadiusAttribute("Mikrotik-Address-List", "walled-garden"));
    };
  }
}
