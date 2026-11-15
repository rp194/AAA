package io.rp194.aaa.radius;

import java.util.Objects;

public final class RadiusAttribute {
  private final String name;
  private final String value;

  public RadiusAttribute(String name, String value) {
    this.name = Objects.requireNonNull(name, "name");
    this.value = Objects.requireNonNull(value, "value");
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RadiusAttribute)) {
      return false;
    }
    RadiusAttribute that = (RadiusAttribute) other;
    return name.equals(that.name) && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    return name + '=' + value;
  }
}
