package io.rp194.aaa.radius;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RadiusPacket {
  private final RadiusCode code;
  private final int identifier;
  private final List<RadiusAttribute> attributes;

  public RadiusPacket(RadiusCode code, int identifier, List<RadiusAttribute> attributes) {
    this.code = Objects.requireNonNull(code, "code");
    this.identifier = identifier;
    this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));
  }

  public RadiusCode getCode() {
    return code;
  }

  public int getIdentifier() {
    return identifier;
  }

  public List<RadiusAttribute> getAttributes() {
    return attributes;
  }
}
