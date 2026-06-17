# DOC 2 v2.00 — Layer 3 & Layer 4: Java Spring Boot + JT400 REST API
## IBM i Integration Services + REST API Layer
### Platform: macOS · Language: Java 21 · Build: Maven · Framework: Spring Boot 3.3

> All test data in this project is entirely fictional. Portfolio owner names (Arthur Dent, Ford Prefect, Trillian, Zaphod Beeblebrox, Marvin) are characters from *The Hitchhiker's Guide to the Galaxy*. ISIN-like values are dummy identifiers, not real securities.

---

## Purpose

This layer extends the IBM i native core with a Java Spring Boot REST API, integrating with the AS400 system through JT400 (IBM Toolbox for Java). This is the standard modernisation pattern in banking environments: RPG/COBOL remains the system of record at the core, while a Java REST layer exposes it to distributed consumers.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Prerequisites](#2-prerequisites)
3. [Project Structure](#3-project-structure)
4. [Dependencies](#4-dependencies)
5. [Configuration](#5-configuration)
6. [Layer 3 — IBM i Integration Services](#6-layer-3--ibm-i-integration-services)
7. [Layer 4 — REST API Controller](#7-layer-4--rest-api-controller)
8. [Domain Models](#8-domain-models)
9. [Test Strategy (AAA Pattern)](#9-test-strategy-aaa-pattern)
10. [Running the Application](#10-running-the-application)

---

## 1. Architecture Overview

The Spring Boot application runs on a local development machine and connects to a live IBM i system (PUB400) using the JT400 library (IBM Toolbox for Java).

```
LAYER 4 — REST API (Spring Boot, Java)
  PortfolioController    → 8 REST endpoints
      GET  /portfolios
      GET  /portfolios/{id}
      PUT  /portfolios/{id}/value
      GET  /eligibility
      GET  /job-info
      POST /orders/enqueue
      GET  /orders/dequeue
      GET  /system/ping

LAYER 3 — IBM i Integration Services (JT400)
  IbmiConnectionConfig   → AS400 connection bean
  CommandExecutorService  → CL command runner (QCMDEXC)
  DataQueueService        → *DTAQ write/read (SNDDTAQ/RCVDTAQ)
  ProgramCallService      → *PGM caller (EBCDIC parameter handling)
  PortfolioRepository     → DB2 for i JDBC (CHAIN/READ/UPDATE)
```

Both layers connect to the same IBM i system and the same CODELIVER1 library built in DOC 1.

---

## 2. Prerequisites

- **Java 21** (Temurin LTS) — installed via Homebrew
- **Maven 3.9+** — project build tool
- **Git** — version control (already installed from DOC 1)
- **JAVA_HOME** — set in shell profile to point to the Java 21 installation
- **IBM i environment variables** — IBMI_HOST, IBMI_USER, IBMI_PASSWORD, IBMI_LIBRARY set in the shell profile. Credentials are never committed to source control.

---

## 3. Project Structure

```
ibmi-batch-simulator/
├── pom.xml
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/example/ibmi/
│   │   │   ├── IbmiApplication.java          ← Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   └── IbmiConnectionConfig.java ← AS400 connection bean
│   │   │   ├── model/
│   │   │   │   ├── Portfolio.java            ← PORTFOLIO table mapping
│   │   │   │   ├── TradeOrder.java           ← TRADE_ORDERS table mapping
│   │   │   │   └── ApiResponse.java          ← REST response wrapper
│   │   │   ├── service/ibmi/
│   │   │   │   ├── CommandExecutorService.java
│   │   │   │   ├── DataQueueService.java
│   │   │   │   └── ProgramCallService.java
│   │   │   ├── repository/
│   │   │   │   └── PortfolioRepository.java
│   │   │   └── controller/
│   │   │       └── PortfolioController.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/com/example/ibmi/
│       │   ├── unit/          ← H2 in-memory DB, mocked AS400
│       │   ├── integration/   ← Live PUB400 connection
│       │   └── system/        ← Full HTTP → JT400 → IBM i round-trips
│       └── resources/
│           ├── application-test.yml
│           ├── schema-h2.sql
│           └── data-test.sql
```

---

## 4. Dependencies

The Maven POM declares these key dependencies:

| Dependency | Purpose |
|------------|---------|
| **spring-boot-starter-web** | REST API framework (Tomcat, JSON serialisation) |
| **spring-boot-starter-jdbc** | JdbcTemplate for DB2 for i data access |
| **jt400** (IBM Toolbox for Java) | AS400 connection, ProgramCall, DataQueue, AS400JDBCDriver |
| **jackson-databind** | JSON serialisation/deserialisation for API payloads |
| **springdoc-openapi** | Auto-generated OpenAPI 3.0 spec and Swagger UI |
| **spring-boot-starter-cache + caffeine** | In-memory caching for frequently read portfolios |
| **spring-boot-starter-test** | JUnit 5, Mockito, MockMvc for testing |
| **h2** (test scope) | In-memory database that mimics DB2 for i in unit tests |

The build uses two Maven profiles: the default profile runs unit tests only (no IBM i connection needed), and the `integration` profile runs all tests including those that hit PUB400.

---

## 5. Configuration

### Application Configuration

The main configuration file connects Spring Boot to IBM i:

- **IBM i connection** — Host, user, password, and library are read from environment variables. The password is never hardcoded.
- **JDBC datasource** — Uses the AS400JDBCDriver from JT400 with a HikariCP connection pool (5 max connections, 30-second timeout).
- **Order queue** — Names the IBM i *DTAQ used for async order processing.

### Test Configuration

Unit tests use a separate profile that substitutes:
- H2 in-memory database in place of DB2 for i
- Dummy IBM i credentials (tests never connect to PUB400)
- Schema and seed data scripts that create identical table structures in H2

The seed data populates five fictional portfolios and five trade orders covering USD, CHF, EUR, and PLN currencies.

---

## 6. Layer 3 — IBM i Integration Services

### 6A. IbmiConnectionConfig

**Purpose:** Creates and manages the AS400 connection object used by all JT400 services.

**Logic:**
```
Read host, user, password, library from environment variables
Create AS400 connection object (host, user, password)
Disable GUI mode (no 5250 sign-on dialog in server context)
Expose the connection and library name as Spring beans
```

**IBM i concept:** The AS400 object is the JT400 entry point for all IBM i operations — analogous to opening a database connection handle or a message queue manager handle.

---

### 6B. CommandExecutorService

**Purpose:** Executes arbitrary CL commands on IBM i from Java, wrapping JT400's CommandCall.

**Logic:**
```
Accept a CL command string (e.g. "DSPLIBL OUTPUT(*PRINT)")
Create a CommandCall object bound to the AS400 connection
Execute the command
If failure: iterate through AS400Message objects and log each error
Return success/failure boolean
```

**IBM i concept:** CommandCall is the Java equivalent of calling the QCMDEXC system API from RPG. It can run any CL command — DSPLIBL, CRTPF, OVRDBF, SBMJOB — from within a Java application.

---

### 6C. DataQueueService

**Purpose:** Produces and consumes IBM i Data Queues (*DTAQ) for asynchronous order processing.

**Logic:**
```
ENQUEUE:
  Serialise TradeOrder to JSON
  Pad JSON bytes to fixed 200-byte entry length (IBM i *DTAQ entries are fixed-length)
  Write to *DTAQ using JT400 DataQueue.write()

DEQUEUE:
  Read from *DTAQ with a configurable wait timeout
  If no entry within timeout: return empty
  If entry found: trim null-padding, deserialise JSON to TradeOrder
  Return the order
```

**IBM i concept:** *DTAQ (Data Queue) is a FIFO message channel between IBM i jobs, created with CRTDTAQ and accessed with SNDDTAQ/RCVDTAQ. This models how a core banking system might receive orders from external systems asynchronously — similar to IBM MQ on z/OS or CICS Temporary Storage Queues.

---

### 6D. ProgramCallService

**Purpose:** Calls IBM i *PGM objects from Java with EBCDIC parameter conversion.

**Logic:**
```
CHECK ELIGIBILITY:
  Convert portfolioId to EBCDIC CHAR(10) using AS400Text with CCSID 37
  Convert ISIN to EBCDIC CHAR(12) using AS400Text with CCSID 37
  Define output parameter: CHAR(2) return code
  Call the *PGM via ProgramCall
  Convert output parameter back from EBCDIC to Java String
  Return result map with return code and eligibility status

GET JOB INFO:
  Create JT400 Job object for the current IBM i session
  Read job name, user, and job number (the IBM i job identity triple)
  Return as a map
```

**IBM i concept:** ProgramCall is the Java equivalent of CALL PGM() in CL. The critical detail is EBCDIC conversion — all parameters must be converted between Java Unicode and EBCDIC CCSID 37 using AS400Text. This is the same EBCDIC handling that RPG does natively and that COBOL handles via its USAGE DISPLAY fields.

---

### 6E. PortfolioRepository

**Purpose:** Data access layer for DB2 for i, using Spring JdbcTemplate with the AS400JDBCDriver.

**Operations:**

| Method | IBM i Equivalent | Logic |
|--------|-----------------|-------|
| `findById(id)` | CHAIN opcode | SELECT by primary key, pad ID to CHAR(10) for fixed-length field matching |
| `findAllActive()` | READ on logical file | SELECT from ACTIVE_PORTFOLIOS view (enforces STATUS='A' at DB level) |
| `updateValue(id, value)` | UPDATE + COMMIT | Journaled UPDATE with Spring @Transactional for commitment control |
| `findPendingOrders()` | DECLARE CURSOR | SELECT WHERE STATUS='PEND', ordered by date |
| `processOrder(id)` | UPDATE WHERE CURRENT OF | Transition order status from PEND to PROC, set process date |

**IBM i concept:** All DB2 for i tables use fixed-length CHAR fields (EBCDIC). The repository pads all key values to their declared length before querying. DB2 for i automatically journals every change — the audit trail is built into the database engine.

---

## 7. Layer 4 — REST API Controller

The PortfolioController exposes 8 REST endpoints, each mapping to a specific IBM i integration pattern. Every response includes an `ibmiConcept` field explaining the IBM i operation being performed — making every endpoint self-documenting for educational purposes.

| Endpoint | Method | IBM i Pattern |
|----------|--------|--------------|
| `/api/v1/system/ping` | GET | CommandCall → QCMDEXC (DSPLIBL) |
| `/api/v1/portfolios` | GET | READ loop on ACTIVE_PORTFOLIOS view |
| `/api/v1/portfolios/{id}` | GET | Keyed read → CHAIN opcode equivalent |
| `/api/v1/portfolios/{id}/value` | PUT | Journaled UPDATE → COMMIT |
| `/api/v1/orders/pending` | GET | DECLARE CURSOR WHERE STATUS='PEND' |
| `/api/v1/orders/enqueue` | POST | DataQueue.write() → SNDDTAQ |
| `/api/v1/orders/dequeue` | GET | DataQueue.read(timeout) → RCVDTAQ |
| `/api/v1/eligibility` | GET | ProgramCall with EBCDIC parameters |
| `/api/v1/job-info` | GET | QUSRJOBI system API → job identity triple |

**Design pattern:** RPG/CL remains the system of record at the core. The Java REST layer wraps it — not replacing the native programs, but exposing them through a modern JSON API. This is the standard modernisation pattern in banking environments where AS400 systems underpin critical transaction processing.

---

## 8. Domain Models

### Portfolio

Maps to the CODELIVER1.PORTFOLIO physical file. Fields:

| Field | DB2 Type | Description |
|-------|----------|-------------|
| portfId | CHAR(10) | Primary key — portfolio identifier |
| owner | CHAR(40) | Portfolio owner name |
| currency | CHAR(3) | ISO currency code (USD, EUR, CHF, GBP, PLN) |
| totalValue | DECIMAL(15,2) | Current portfolio value |
| status | CHAR(1) | A = Active, I = Inactive |
| lastUpd | DATE | Last modification date |

### TradeOrder

Maps to the CODELIVER1.TRADE_ORDERS physical file. Fields:

| Field | DB2 Type | Description |
|-------|----------|-------------|
| orderId | CHAR(20) | Primary key — order identifier |
| portfId | CHAR(10) | Foreign key to PORTFOLIO |
| isin | CHAR(12) | Security identifier (dummy values) |
| quantity | DECIMAL(15,4) | Number of units |
| price | DECIMAL(15,4) | Price per unit |
| status | CHAR(4) | Lifecycle: PEND → PROC → SETL / FAIL |

### ApiResponse

A generic wrapper for all REST responses. Contains two fields:
- `data` — the business payload (portfolio, order list, status message)
- `ibmiConcept` — a plain-English explanation of the IBM i operation performed

---

## 9. Test Strategy (AAA Pattern)

All tests follow the Arrange-Act-Assert (AAA) pattern across three tiers.

### Unit Tests (no IBM i connection)

Run against an H2 in-memory database and mocked AS400 objects.

**PortfolioRepository unit tests:**

```
TC-U-01: findById with known ID
  Arrange — seed H2 with test portfolios
  Act     — call findById("PF001")
  Assert  — result is present, owner = "Arthur Dent", currency = "USD"

TC-U-02: findById with unknown ID
  Arrange — no matching ID in H2
  Act     — call findById("XXXXX")
  Assert  — result is empty

TC-U-03: findAllActive returns only active portfolios
  Arrange — seed has 4 active, 1 inactive (PF003)
  Act     — call findAllActive()
  Assert  — result size = 4, all STATUS = "A", PF003 not present

TC-U-04: updateValue persists correctly
  Arrange — PF001 exists with value 150,000
  Act     — call updateValue("PF001", 200,000)
  Assert  — re-read PF001, value = 200,000

TC-U-05: updateValue returns false for non-existent portfolio
  Arrange — no portfolio "ZZZZ"
  Act     — call updateValue("ZZZZ", 1000)
  Assert  — returns false

TC-U-06: findPendingOrders returns only PEND status
  Arrange — seed has 5 PEND orders
  Act     — call findPendingOrders()
  Assert  — result size = 5, all STATUS = "PEND"
```

**DataQueue unit tests (mocked AS400):**

```
TC-U-10: enqueueOrder handles connection failure gracefully
  Arrange — AS400 is mocked (DataQueue.write will fail)
  Act     — call enqueueOrder(testOrder)
  Assert  — returns false, no exception thrown

TC-U-11: dequeueOrder returns empty on connection failure
  Arrange — AS400 is mocked (DataQueue.read will fail)
  Act     — call dequeueOrder(1)
  Assert  — returns empty Optional
```

### Integration Tests (live PUB400)

Run against the real DB2 for i on PUB400. Activated with the `integration` Maven profile.

```
TC-I-01: findById reads PF001 from live DB2
TC-I-02: findAllActive reads ACTIVE_PORTFOLIOS view
TC-I-03: findPendingOrders returns orders from TRADE_ORDERS
TC-I-04: updateValue persists to live DB2 and restores original value
```

### System Tests (full stack round-trip)

Exercise the complete HTTP → Spring Boot → JT400 → IBM i path.

```
TC-S-01: GET /system/ping confirms IBM i connectivity
TC-S-02: GET /portfolios returns active portfolios via view
TC-S-03: GET /portfolios/PF001 returns single portfolio
TC-S-04: POST enqueue + GET dequeue round-trip via *DTAQ
TC-S-05: GET /job-info returns real IBM i job details
```

---

## 10. Running the Application

### Build and Unit Tests

Build the project and run unit tests using Maven. Unit tests use H2 in-memory and require no IBM i connection — they run in any CI/CD pipeline.

### Integration and System Tests

Set the IBM i environment variables (host, user, password, library), then run the Maven build with the integration profile. These tests connect to a live IBM i system.

### Starting the Application

Start the Spring Boot application on port 8080. Once running, the REST endpoints are accessible for manual testing via a REST client or the included `.http` test file in VS Code.

### Sample API Response

```json
{
  "data": {
    "portfId": "PF001",
    "owner": "Arthur Dent",
    "currency": "USD",
    "totalValue": 150000.00,
    "status": "A"
  },
  "ibmiConcept": "DB2 for i JDBC keyed read — CHAIN opcode equivalent."
}
```

---

*End of DOC 2 v2.00 — Layer 3 & Layer 4: Java Spring Boot + JT400 REST API*
