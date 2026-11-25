# UPI Offline Mesh Demo

A Java/Spring Boot teaching demo of **mesh-routed deferred settlement**. It models encrypted payment packets being carried by offline virtual phones and uploaded by a bridge device when it has connectivity.

> **Scope and safety:** This is a local software simulator, not UPI, a wallet, or a banking application. It has no NPCI or bank integration and is not suitable for real funds or public deployment. See [Security and limitations](docs/SECURITY.md).

## What the demo shows

- Hybrid packet encryption using RSA-OAEP and AES-256-GCM.
- A simulated device mesh that gossips packets to virtual devices.
- A bridge upload endpoint that decrypts a packet and attempts settlement.
- Atomic, process-local duplicate claims plus a database uniqueness constraint on settled packet hashes.
- A dashboard for running the demo and inspecting balances and the transaction ledger.

These are demonstrations of concepts, not production guarantees. In particular, the sender and mesh are simulated in the server process, and the idempotency cache is not shared across instances or restarts.

## Requirements

- JDK 17 or newer. The project was tested with Java 21.
- No separate Maven installation is needed; the Maven Wrapper is included.
- Internet access is needed on the first run to download Maven and dependencies.

## Run locally

From the project directory on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) for the dashboard. The server uses port `8080` by default; override it with the `SERVER_PORT` environment variable if necessary.

The app seeds four in-memory accounts at startup:

| Account | Initial balance |
| --- | ---: |
| alice | 5000.00 |
| bob | 1000.00 |
| carol | 750.00 |
| dave | 1250.00 |

The H2 database is in memory and configured with `create-drop`. Data, account balances, and transactions are cleared when the application stops.

## Run tests

```powershell
.\mvnw.cmd test
```

The tests cover hybrid encryption/decryption, authenticated-ciphertext tampering, and concurrent duplicate deliveries. They do not constitute a production readiness or security assessment.

## Try the dashboard flow

1. Select sender, receiver, amount, and a demo PIN, then choose **Inject into Mesh**.
2. Choose **Run Gossip Round** to copy the packet to the simulated devices.
3. Choose **Bridges Upload to Backend** to have the internet-enabled bridge submit its packets.
4. Observe the account balances and transaction ledger.
5. **Reset Mesh** clears virtual-device packets and the process-local idempotency cache. It does not undo settled transactions or restore balances.

The default mesh has one bridge, `phone-bridge`. Gossip is a simplified in-process broadcast, not Bluetooth or a realistic routing protocol.

## API

Endpoints are documented in [API reference](docs/API.md). The principal workflow is:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/` | Interactive demo dashboard |
| `GET` | `/api/server-key` | Public encryption key, Base64-encoded |
| `GET` | `/api/accounts` | Current demo account balances |
| `GET` | `/api/transactions` | Most recent 20 settled transactions |
| `POST` | `/api/demo/send` | Create an encrypted packet and inject it into the mesh |
| `POST` | `/api/mesh/gossip` | Run one simulated gossip round |
| `GET` | `/api/mesh/state` | Read virtual-device state |
| `POST` | `/api/mesh/flush` | Upload bridge-held packets |
| `POST` | `/api/mesh/reset` | Clear mesh packets and the idempotency cache |
| `POST` | `/api/bridge/ingest` | Submit a packet directly to the ingestion pipeline |

The H2 console is enabled at `/h2-console` for local development. It is not secured and must not be exposed outside a trusted local environment.

## Project layout

```text
src/main/java/com/demo/upimesh/
  controller/  HTTP API and dashboard routes
  crypto/      Ephemeral server key pair and hybrid encryption
  model/       JPA entities, repositories, and packet records
  service/     Demo setup, mesh simulation, ingestion, idempotency, settlement
src/main/resources/
  application.properties
  templates/dashboard.html
src/test/java/com/demo/upimesh/
  HybridCryptoTest.java
  IdempotencyConcurrencyTest.java
docs/
  API.md
  ARCHITECTURE.md
  SECURITY.md
```

## Configuration

Defaults are in `src/main/resources/application.properties`:

| Property | Default | Description |
| --- | --- | --- |
| `server.port` | `8080` | HTTP server port |
| `spring.datasource.url` | `jdbc:h2:mem:upimesh` | In-memory H2 database |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Recreates the schema at startup and drops it at shutdown |
| `spring.h2.console.enabled` | `true` | Enables the development database console |
| `demo.packet.ttl` | `5` | Initial TTL value attached to packets |
| `demo.packet.freshness-hours` | `24` | Maximum age accepted by the ingestion check |

Spring Boot configuration can override these properties, but changing a setting does not make the demo production-ready.

## Further documentation

- [Architecture and data flow](docs/ARCHITECTURE.md)
- [API reference and examples](docs/API.md)
- [Security, limitations, and production gaps](docs/SECURITY.md)
