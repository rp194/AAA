package io.rp194.aaa.coa;

import io.rp194.aaa.radius.RadiusAttribute;
import java.util.List;

public interface VendorEnforcementMapper {
  List<RadiusAttribute> enforcementAttributes(CoaAction action);
}
