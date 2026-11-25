# API reference

All endpoints are served by the Spring Boot application on port `8080` by default. JSON request and response bodies use `Content-Type: application/json`.

## Dashboard

### `GET /`

Returns the interactive HTML demo.

## Server public key

### `GET /api/server-key`

Returns the current server RSA public key in Base64-encoded X.509 SubjectPublicKeyInfo format:

```json
{
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A..."
}
```

The matching private key is generated in memory at application startup. The key is ephemeral and changes after restart.

## Accounts

### `GET /api/accounts`

Returns demo accounts and current balances. The initial values are seeded at startup (see the root README).

Example:

```json
[
  {
    "id": 1,
    "accountName": "alice",
    "balance": 5000.00,
    "version": 0
  }
]
```

## Transactions

### `GET /api/transactions`

Returns up to 20 most recent transaction records, ordered by descending transaction ID. An empty ledger returns an empty array.

## Create and inject a demo payment

### `POST /api/demo/send`

Request:

```json
{
  "sender": "alice",
  "receiver": "bob",
  "amount": 500.00,
  "pin": "1234"
}
```

Example response:

```json
{
  "packetId": "550e8400-e29b-41d4-a716-446655440000",
  "ttl": 5,
  "ciphertextLength": 588
}
```

This endpoint is a demo helper: the server constructs and encrypts the instruction, then injects it into `phone-alice`. It does not authenticate a user or validate a banking PIN. See [Security and limitations](SECURITY.md).

## Mesh operations

### `GET /api/mesh/state`

Returns each seeded virtual device with its internet flag and packet count:

```json
[
  {
    "id": "phone-alice",
    "hasInternet": false,
    "packetCount": 1
  },
  {
    "id": "phone-bridge",
    "hasInternet": true,
    "packetCount": 0
  }
]
```

### `POST /api/mesh/gossip`

Runs one simplified broadcast round. Returns the number of newly stored packet copies and the updated device state:

```json
{
  "newDeliveries": 4,
  "mesh": []
}
```

### `POST /api/mesh/flush`

Submits all packets held by internet-enabled devices to the local ingestion service. Returns each ingestion result and current mesh state:

```json
{
  "uploads": [
    {
      "outcome": "SETTLED",
      "packetHash": "a3f8c9...",
      "reason": null,
      "transactionId": 1
    }
  ],
  "mesh": []
}
```

Possible `outcome` values are `SETTLED`, `DUPLICATE_DROPPED`, and `INVALID`. Packets remain on devices after upload; a later flush can therefore attempt them again.

`POST /api/mesh/flushBridges` is an alias for this endpoint.

### `POST /api/mesh/reset`

Clears virtual-device packets and the in-memory idempotency cache:

```json
{
  "reset": true,
  "mesh": []
}
```

It does not delete transactions or restore account balances.

## Direct bridge ingestion

### `POST /api/bridge/ingest`

Accepts a `MeshPacket` JSON object:

```json
{
  "packetId": "550e8400-e29b-41d4-a716-446655440000",
  "ttl": 2,
  "createdAt": "2026-11-25T07:30:00Z",
  "ciphertext": "base64-encoded-RSA-and-AES-envelope"
}
```

Returns an ingestion result:

```json
{
  "outcome": "SETTLED",
  "packetHash": "a3f8c9...",
  "reason": null,
  "transactionId": 1
}
```

Optional headers `X-Bridge-Node-Id` and `X-Hop-Count` are accepted but are currently not used for authentication, authorization, or validation. A valid payload must have been encrypted for the public key of the currently running server instance.

## Development-only H2 console

The H2 console is available at `/h2-console`. Use JDBC URL `jdbc:h2:mem:upimesh`, username `sa`, and an empty password when connecting to the running application. The console is enabled without authentication in this demo; do not expose it to untrusted networks.
