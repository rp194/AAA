package io.rp194.aaa.vendor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.device.VendorType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateValidatorTest {
  @Test
  void detectsUnsupportedAttributes() {
    TemplateValidator validator = new TemplateValidator();
    TemplateDefinition template = new TemplateDefinition("tenant-a", VendorType.GENERIC, null, "v1", 100, Instant.EPOCH,
        List.of(new TemplateAttribute("Unknown-Attr", "${username}")));

    List<String> errors = validator.validate(template);
    assertFalse(errors.isEmpty());
  }

  @Test
  void acceptsSimpleTemplate() {
    TemplateValidator validator = new TemplateValidator();
    TemplateDefinition template = new TemplateDefinition("tenant-a", VendorType.GENERIC, null, "v1", 100, Instant.EPOCH,
        List.of(new TemplateAttribute("Reply-Message", "hello ${username}")));

    assertTrue(validator.validate(template).isEmpty());
  }
}
