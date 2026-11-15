package io.rp194.aaa.transport;

import io.rp194.aaa.accounting.AccountingService;
import io.rp194.aaa.radius.RadiusPacket;
import io.rp194.aaa.server.RadiusAccessHandler;
import java.util.Objects;
import java.util.Optional;

public final class RadiusRequestRouter {
  private final RadiusAccessHandler accessHandler;
  private final AccountingService accountingService;

  public RadiusRequestRouter(RadiusAccessHandler accessHandler, AccountingService accountingService) {
    this.accessHandler = Objects.requireNonNull(accessHandler, "accessHandler");
    this.accountingService = Objects.requireNonNull(accountingService, "accountingService");
  }

  public Optional<RadiusPacket> route(RadiusPacketCodec.Decoded decoded) {
    return switch (decoded.type()) {
      case ACCESS -> Optional.of(accessHandler.handleAccessRequest(decoded.accessRequest(), decoded.identifier()));
      case ACCOUNTING -> {
        accountingService.handleInterimUpdate(decoded.interimUpdate());
        yield Optional.empty();
      }
    };
  }
}
