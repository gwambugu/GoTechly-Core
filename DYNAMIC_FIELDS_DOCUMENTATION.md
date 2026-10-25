# Dynamic Additional Fields Documentation

## Overview
To support dynamic and custom fields that vary by Payment Service Provider (PSP) or specific transaction types (e.g., `email`, `phoneNumber`, `customMetadata`), the `PaymentLog` entity has been enhanced with 5 JSON-backed `CLOB` columns. These columns store additional metadata as key-value pairs (`Map<String, String>`).

## Architectural Changes

### 1. Database Schema (Liquibase)
A new changelog `20260223150000_added_additional_fields_PaymentLog.xml` adds the following columns to the `payment_log` table:
- `sender_additional_fields` (CLOB)
- `receiver_additional_fields` (CLOB)
- `details_additional_fields` (CLOB)
- `cbs_additional_fields` (CLOB)
- `psp_additional_fields` (CLOB)

### 2. JPA Persistence Mapping
- **`JsonMapConverter.java`**: A JPA `AttributeConverter` that automatically serializes Java `Map<String, String>` to a JSON string before saving to the database and deserializes it back to a `Map` when reading.
- **`PaymentLog.java`**: The entity now includes these maps, annotated with `@Convert(converter = JsonMapConverter.class)` and `@Lob`.

### 3. Utility Classes
- **`JsonUtil.java`**: Provides static methods for JSON/Map conversion using Jackson's `ObjectMapper`. It includes safety checks and error handling for malformed JSON.

### 4. DTO & Mapping
- **`PaymentLogDTO.java`**: Updated to include the same 5 map fields to allow dynamic data to flow through the REST API.
- **`PaymentLogMapper.java`**: Updated to ensure seamless transfer of these maps between the Entity and DTO layers.
- **`PaymentLogService.java`**: The `toSPi` method now maps these fields to the `PaymentLogSPI` structure, ensuring plugins can access the custom data.

## Usage Instructions

### Working with the Entity
The `PaymentLog` entity provides convenient helper methods to manage dynamic fields without manually manipulating the maps:

```java
PaymentLog log = new PaymentLog();

// Adding dynamic fields
log.addSenderField("email", "john.doe@example.com");
log.addDetailsField("promoCode", "SAVE20");
log.addPspField("transactionHash", "0xabc123...");

// Retrieving dynamic fields
String email = log.getSenderField("email");

// Removing dynamic fields
log.removeDetailsField("promoCode");
```

### Working with the API (JSON)
When sending or receiving requests via the REST API, the additional fields are represented as standard JSON objects:

```json
{
  "requestId": "REQ-123",
  "amount": 100.00,
  "senderAdditionalFields": {
    "email": "john.doe@example.com",
    "loyaltyId": "L-998"
  },
  "detailsAdditionalFields": {
    "deviceFingerprint": "a1b2c3d4e5"
  }
}
```

## Benefits
- **Schema Stability**: No need for database migrations when a new PSP requires a custom field.
- **Type Safety**: Java code interacts with a typed `Map<String, String>` instead of raw JSON strings.
- **Automation**: Serialization and deserialization are handled transparently by JPA.
