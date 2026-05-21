# AAA

Java-first RADIUS AAA skeleton focused on multi-tenant session management, vendor-specific attribute mapping,
and a write-behind accounting ledger.

## Highlights
- RADIUS request handling with tenant-scoped session tracking.
- Device profile lookup by NAS-IP/NAS-Identifier for vendor selection.
- Strategy-based vendor mappers (MikroTik and Cisco examples).
- Redis-style session store with TTL aligned to Interim-Update intervals.
- Asynchronous ledger writer representing PostgreSQL persistence.

## Build & test
```bash
mvn test
```
