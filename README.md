# AAA

Java-first **RADIUS-only** AAA skeleton focused on multi-tenant session management, vendor-specific attribute mapping,
and a write-behind accounting ledger.

## Confirmed scope and assumptions
- **Protocol scope:** RADIUS (Access + Accounting + CoA/PoD). TACACS+ is out of current scope.
- **Tenancy model:** Tenant is an explicit attribute (`Attr-250` in this skeleton) and every state key is tenant scoped.
- **NAS inventory:** Device/NAS profiles are resolved by `tenant + NAS-Identifier` first, then `tenant + NAS-IP`.
- **Package schema:** User profile carries shaping and service profile fields used for authorization and policy projection.
- **Availability/SLA goal:** Prioritize packet-path responsiveness (non-blocking ingress + worker offload), async persistence, and cache-first lookups.

## Highlights
- Binary RADIUS packet decode (header/length/attributes) with standard attribute dictionary and VSA dictionary hooks.
- Netty UDP ingress with worker-pool offload and overload/drop metrics.
- Device profile lookup by NAS-IP/NAS-Identifier for vendor selection.
- Strategy/template-based vendor mappers (MikroTik, Cisco, template repository with per-NAS matching).
- Redis-style session store API with TTL and atomic counter increment script hook.
- Accounting flow with Interim handling, async ledger writer, and stale-session reconciler.
- Policy evaluation + CoA action mapping and UDP CoA client with retries/audit hooks.

## Build & test
```bash
mvn test
```
