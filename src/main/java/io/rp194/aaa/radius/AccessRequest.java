package io.rp194.aaa.radius;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AccessRequest {
  private final String tenantId;
  private final String username;
  private final String sessionId;
  private final String nasIp;
  private final String nasIdentifier;
  private final String framedIpAddress;
  private final String nasPort;
  private final String nasPortId;
  private final String macAddress;
  private final int interimIntervalSeconds;
  private final List<RadiusAttribute> attributes;

  private AccessRequest(Builder builder) {
    this.tenantId = Objects.requireNonNull(builder.tenantId, "tenantId");
    this.username = Objects.requireNonNull(builder.username, "username");
    this.sessionId = Objects.requireNonNull(builder.sessionId, "sessionId");
    this.nasIp = Objects.requireNonNull(builder.nasIp, "nasIp");
    this.nasIdentifier = builder.nasIdentifier;
    this.framedIpAddress = builder.framedIpAddress;
    this.nasPort = builder.nasPort;
    this.nasPortId = builder.nasPortId;
    this.macAddress = builder.macAddress;
    this.interimIntervalSeconds = builder.interimIntervalSeconds;
    this.attributes = Collections.unmodifiableList(new ArrayList<>(builder.attributes));
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getUsername() {
    return username;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getNasIp() {
    return nasIp;
  }

  public String getNasIdentifier() {
    return nasIdentifier;
  }

  public String getFramedIpAddress() {
    return framedIpAddress;
  }

  public String getNasPort() {
    return nasPort;
  }

  public String getNasPortId() {
    return nasPortId;
  }

  public String getMacAddress() {
    return macAddress;
  }

  public int getInterimIntervalSeconds() {
    return interimIntervalSeconds;
  }

  public List<RadiusAttribute> getAttributes() {
    return attributes;
  }

  public Optional<String> findAttribute(String name) {
    return attributes.stream()
        .filter(attribute -> attribute.getName().equals(name))
        .map(RadiusAttribute::getValue)
        .findFirst();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String tenantId;
    private String username;
    private String sessionId;
    private String nasIp;
    private String nasIdentifier;
    private String framedIpAddress;
    private String nasPort;
    private String nasPortId;
    private String macAddress;
    private int interimIntervalSeconds = 300;
    private List<RadiusAttribute> attributes = new ArrayList<>();

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder nasIp(String nasIp) {
      this.nasIp = nasIp;
      return this;
    }

    public Builder nasIdentifier(String nasIdentifier) {
      this.nasIdentifier = nasIdentifier;
      return this;
    }

    public Builder framedIpAddress(String framedIpAddress) {
      this.framedIpAddress = framedIpAddress;
      return this;
    }

    public Builder nasPort(String nasPort) {
      this.nasPort = nasPort;
      return this;
    }

    public Builder nasPortId(String nasPortId) {
      this.nasPortId = nasPortId;
      return this;
    }

    public Builder macAddress(String macAddress) {
      this.macAddress = macAddress;
      return this;
    }

    public Builder interimIntervalSeconds(int interimIntervalSeconds) {
      this.interimIntervalSeconds = interimIntervalSeconds;
      return this;
    }

    public Builder attributes(List<RadiusAttribute> attributes) {
      this.attributes = new ArrayList<>(attributes);
      return this;
    }

    public Builder addAttribute(RadiusAttribute attribute) {
      this.attributes.add(attribute);
      return this;
    }

    public AccessRequest build() {
      return new AccessRequest(this);
    }
  }
}
