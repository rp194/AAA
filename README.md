# AAA

Java-first **RADIUS-only** AAA **non-production skeleton** focused on multi-tenant session management, vendor-specific
attribute mapping, and a write-behind accounting ledger.

## Confirmed scope and assumptions
- **Protocol scope:** RADIUS (Access + Accounting + CoA/PoD). TACACS+ is out of current scope until a defined roadmap milestone (v2+).
- **Non-production skeleton:** cryptographic validation (shared-secret MD5 auth, password hiding) and full NAS interoperability
  are not implemented in this skeleton.
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

## Session TTL policy
- Redis session TTL defaults to **2x interim interval + 30s grace** to tolerate jitter; this is configurable via `SessionTtlPolicy`.

## Concurrency policy
- Access policy defaults to rejecting concurrent logins; optional PoD-on-oldest mode is available via `AccessPolicy.ConcurrencyPolicy`.

## CoA/PoD identifier completeness
- CoA/PoD builders include Acct-Session-Id, Framed-IP-Address, NAS-Port, NAS-Port-Id, and Calling-Station-Id when available.

## Crypto pipeline and backpressure
- Packet decoding and future cryptographic validation are expected to run in the worker pool off the Netty event loops.
- `TransportMetrics` tracks queue depth and drop counts for backpressure tuning.
- Tune worker count and queue capacity to match expected packet rates and CPU headroom.

## Benchmarks
- Load checks live in `AccountingBurstLoadTest` and report p95/p99 packet latency buckets.

## Build & test
```bash
mvn test
```
