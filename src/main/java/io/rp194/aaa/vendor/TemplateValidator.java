package io.rp194.aaa.vendor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateValidator {
  private static final Set<String> ALLOWED_ATTRIBUTES = Set.of(
      "Mikrotik-Rate-Limit", "Mikrotik-Address-List", "Cisco-AVPair", "Reply-Message");
  private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

  public List<String> validate(TemplateDefinition template) {
    List<String> errors = new ArrayList<>();
    for (TemplateAttribute attribute : template.attributes()) {
      if (!ALLOWED_ATTRIBUTES.contains(attribute.name())) {
        errors.add("Unsupported attribute: " + attribute.name());
      }
      Matcher matcher = VAR_PATTERN.matcher(attribute.valueTemplate());
      while (matcher.find()) {
        String variable = matcher.group(1);
        if (!variable.matches("[a-zA-Z0-9_.]+")) {
          errors.add("Invalid variable expression: " + variable);
        }
      }
    }
    return errors;
  }
}
