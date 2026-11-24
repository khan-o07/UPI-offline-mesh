# UPI-offline-mesh
Spring Boot backend that simulates offline UPI payments over a Bluetooth-style mesh network. Payments are encrypted on the sender's phone (RSA-OAEP + AES-256-GCM), hop device-to-device, and are uploaded by any phone with internet. The server deduplicates with atomic idempotency, rejects tampered or replayed packets, and settles exactly once.
