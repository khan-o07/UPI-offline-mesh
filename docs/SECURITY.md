# Security, limitations, and production gaps

## Intended use

This code is a local educational demo of encrypted packet transport and deferred settlement. **Do not use it to hold, move, or authorize real money, and do not expose it to the public internet.** It is not an implementation of UPI or an approved banking system.

## Security properties demonstrated

- The payment payload is encrypted using a fresh AES-256 key and AES-GCM.
- The AES key is wrapped using the server's RSA public key and OAEP padding.
- AES-GCM authenticates ciphertext; modification of an encrypted packet causes decryption to fail.
- SHA-256 of the ciphertext is used as the cache key for duplicate claims.
- A unique database index on transaction packet hashes is a second duplicate constraint.
- Account updates and ledger insertion are performed inside a Spring transaction.
- Account entities use JPA optimistic locking.
- The ingestion service rejects instructions outside the configured timestamp freshness window.

These properties are deliberately limited. Encryption to the server's public key does **not** prove which sender created a packet. The timestamp is part of encrypted data, not a sender's digital signature. The demo has no sender signing keys, identity verification, or authenticated bridge nodes.

## Important limitations in the current implementation

- **No real UPI or bank connection:** No NPCI, bank core, settlement network, KYC, VPA, or payment authorization is integrated.
- **Sender and mesh are simulated:** `POST /api/demo/send` creates packets on the server. No Android/iOS client or Bluetooth/Wi-Fi Direct transport is implemented.
- **No authentication or authorization:** API operations, including direct packet ingestion, are unauthenticated. Optional bridge headers are ignored.
- **PIN is not verified:** The demo stores Java `String.hashCode()` of the supplied PIN in the encrypted payload and never checks it. This is not a secure password/PIN hash and is not an authentication mechanism.
- **Financial input validation is incomplete:** The API does not enforce positive amounts, sender/receiver inequality, limits, or robust request validation. Do not send untrusted traffic to it.
- **Ephemeral encryption key:** A new RSA key pair is generated at every startup. The private key is held in application memory and is not protected by an HSM, KMS, or secret-management system.
- **In-memory, single-process idempotency:** The `ConcurrentHashMap` does not coordinate across instances and loses state on restart. Failed or invalid attempts can retain their cache claim until reset or process exit.
- **In-memory database:** H2 uses `create-drop`; transactions and balances are not durable.
- **Demo reset is not a ledger rollback:** It clears packets and cache entries, not settled account balances or ledger rows.
- **H2 console is open:** The development console has no authentication and must not be exposed.
- **No operational controls:** TLS termination, rate limits, audit trails, alerting, abuse detection, backup/restore, and incident-response procedures are not configured.
- **Offline double-spend is not prevented:** A sender can create multiple offline instructions against the same displayed balance. The backend can only process what it receives; this demo has no pre-funded, hardware-backed offline balance mechanism.
- **No legal or regulatory assessment:** This repository does not establish compliance with payment, privacy, security, or financial regulations.

## Before any real financial deployment

A production system would require a new threat model and architecture review, plus at minimum:

1. Approved integrations and authorization flows with the relevant bank/payment network; do not treat this demo's settlement code as a substitute.
2. Strong, tested identity and transaction authorization. Use supported cryptographic key storage and an appropriate mobile trust model; never trust a client-supplied amount or account identity merely because it is encrypted.
3. Sender signatures or equivalent authenticated authorization, verified by the backend, with replay protection bound to the transaction identity.
4. Durable, distributed idempotency and ledger constraints with explicit handling of retries, partial failures, concurrency, and reconciliation.
5. A production database, schema migrations, backups, access controls, and a reviewed accounting/ledger model.
6. Authenticated and authorized bridge clients, transport security, key rotation/revocation, rate limits, and abuse monitoring.
7. Offline value controls that address double-spend and device compromise, with independent security review and hardware-backed protections where appropriate.
8. Input validation, error handling, logging without sensitive payloads, privacy controls, operational monitoring, recovery procedures, and compliance review.
9. Automated CI checks, dependency and code scanning, integration testing, load testing, deployment approvals, and a tested rollback plan.

Until that work is completed and independently reviewed, keep the application local and use synthetic demo data only.
