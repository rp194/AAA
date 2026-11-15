package io.rp194.aaa.vendor;

import io.rp194.aaa.device.VendorType;
import io.rp194.aaa.radius.RadiusAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateVendorMapper implements VendorMapper {
  private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
  private final VendorType vendorType;
  private final TemplateRepository repository;
  private final VendorMapper fallback;

  public TemplateVendorMapper(VendorType vendorType, TemplateRepository repository, VendorMapper fallback) {
    this.vendorType = vendorType;
    this.repository = repository;
    this.fallback = fallback;
  }

  @Override
  public List<RadiusAttribute> mapAttributes(MapperContext context) {
    String deviceKey = context.getDeviceProfile().map(d -> d.getNasIp() + ":" + Optional.ofNullable(d.getNasIdentifier()).orElse("")).orElse(null);
    Optional<TemplateDefinition> template = repository.findTemplate(
        context.getProfile().getTenantId(), vendorType, deviceKey, context.getProfile().getUsername(), context.getNow());
    if (template.isEmpty()) {
      return fallback.mapAttributes(context);
    }
    Map<String, String> variables = context.variables();
    List<RadiusAttribute> attributes = new ArrayList<>();
    for (TemplateAttribute t : template.get().attributes()) {
      attributes.add(new RadiusAttribute(t.name(), substitute(t.valueTemplate(), variables)));
    }
    return attributes;
  }

  private String substitute(String template, Map<String, String> vars) {
    Matcher matcher = VAR_PATTERN.matcher(template);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String replacement = vars.getOrDefault(matcher.group(1), "");
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
