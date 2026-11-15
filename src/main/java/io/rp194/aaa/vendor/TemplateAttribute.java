package io.rp194.aaa.vendor;

import java.util.Objects;

public record TemplateAttribute(String name, String valueTemplate) {
  public TemplateAttribute {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(valueTemplate, "valueTemplate");
  }
}
