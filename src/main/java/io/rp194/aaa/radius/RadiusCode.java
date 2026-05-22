package io.rp194.aaa.radius;

public enum RadiusCode {
  ACCESS_REQUEST(1),
  ACCESS_ACCEPT(2),
  ACCESS_REJECT(3),
  ACCOUNTING_REQUEST(4),
  ACCOUNTING_RESPONSE(5),
  DISCONNECT_REQUEST(40),
  DISCONNECT_ACK(41),
  DISCONNECT_NACK(42),
  COA_REQUEST(43),
  COA_ACK(44),
  COA_NACK(45);

  private final int code;

  RadiusCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static RadiusCode fromCode(int code) {
    for (RadiusCode value : values()) {
      if (value.code == code) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unsupported RADIUS code: " + code);
  }
}
