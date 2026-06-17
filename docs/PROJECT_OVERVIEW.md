# AS400 IBM i Batch Settlement Simulator

## Project Overview

This project was built to demonstrate foundational competency in the IBM i (AS400) technology stack and to develop a practical, intentional understanding of batch-processing business flows common in banking and wealth management. It spans the full stack: native IBM i programs at the core, a Java REST API as the modern integration layer, and an optional 5250 green-screen UI for interactive validation.

The system models a simplified portfolio management and trade-order settlement pipeline. Portfolios hold value in multiple currencies (USD, CHF, EUR, PLN). Trade orders move through a lifecycle (Pending, Processed, Settled) driven by a nightly batch run with cursor-based processing and periodic commits for recoverability.

> All test data is entirely fictional. Portfolio owner names (Arthur Dent, Ford Prefect, Trillian, Zaphod Beeblebrox, Marvin) are characters from *The Hitchhiker's Guide to the Galaxy*. ISIN-like values are dummy identifiers and do not represent real securities.

---

## Business Context

In wealth management and private banking, core transaction engines typically run on IBM i systems. These systems serve as the book of record for client portfolios, trade orders, settlement processing, and regulatory reporting. The daily operational cycle involves:

- **Intraday processing** — real-time inquiries against portfolio and order data, validated through business rules before execution
- **Nightly batch settlement** — pending trade orders are swept, validated, processed, and committed in controlled batches with full audit trails via DB2 journaling
- **Cross-system integration** — core IBM i data is exposed to distributed systems (Java, REST APIs, message queues) for front-office tooling, client reporting, and regulatory feeds

This project replicates these patterns at a foundational level, using a public IBM i system (PUB400) as the development and test environment.

---

## Architecture

```
Layer 4 — REST API (Java Spring Boot)
  8 endpoints exposing portfolio queries, order management, and system operations
  Self-documenting responses explain the IBM i operation behind each call
      |
      |  JT400 (IBM Toolbox for Java)
      v
Layer 3 — IBM i Integration Services (Java)
  AS400 connection management, DB2 JDBC, DataQueue I/O, ProgramCall with EBCDIC handling
      |
      |  AS400JDBCDriver / ProgramCall / DataQueue
      v
Layer 2 — IBM i Native Programs (RPG, COBOL, CL)
  PORTFINQ    — RPGLE portfolio inquiry (embedded SQL)
  PORTFCBL    — COBOL/400 portfolio inquiry (same logic, same table)
  ORDPROC     — CL driver calling both RPG and COBOL programs
  PORTFSVC    — ILE service program (3 exported validation procedures)
  PORTFTEST   — Functional test exercising PORTFSVC
  ORDRBATCH   — Batch order processor (SQL cursor + periodic COMMIT)
  PORTFUI     — 5250 green-screen UI for interactive validation
      |
      v
Layer 1 — DB2 for i (Database)
  PORTFOLIO         — Master portfolio data (5 accounts, 4 currencies)
  TRADE_ORDERS      — Trade order staging (pending -> processed lifecycle)
  ACTIVE_PORTFOLIOS — SQL view filtering active portfolios only
```

---

## What Each Layer Demonstrates

### Layer 1 — DB2 for i

| Concept | Detail |
|---------|--------|
| Physical files | PORTFOLIO and TRADE_ORDERS tables with primary keys and foreign key constraints |
| Logical file equivalent | ACTIVE_PORTFOLIOS SQL view as a filtered access path |
| Journaling | Required for commitment control in batch processing |
| Multi-currency data | USD, CHF, EUR (French), PLN (Polish) — reflecting international banking operations |

### Layer 2 — IBM i Native Programs

| Program | Language | What It Proves |
|---------|----------|---------------|
| PORTFINQ | RPGLE (SQLRPGLE) | Fully-free RPG, embedded SQL, SQLCODE handling, DCL-PI parameter interface |
| PORTFCBL | COBOL/400 | Same SQL logic in COBOL — demonstrates that z/OS COBOL skills transfer directly to IBM i |
| ORDPROC | CL (CLLE) | Cross-language integration: one CL driver calling both RPGLE and COBOL programs |
| PORTFSVC | RPGLE (*SRVPGM) | ILE service program with exported procedures — shared library pattern, the architectural centrepiece |
| PORTFTEST | RPGLE | Service program consumer with file-based test output |
| ORDRBATCH | RPGLE (SQLRPGLE) | Batch settlement: SQL cursor, row-by-row processing, periodic COMMIT every 10 records, ROLLBACK on failure |
| PORTFUI | RPGLE + DDS | Native 5250 green-screen UI calling PORTFSVC for interactive portfolio validation |

### Layer 3 — Java Integration Services

| Service | IBM i Pattern |
|---------|--------------|
| IbmiConnectionConfig | AS400 connection lifecycle management |
| CommandExecutorService | CL command execution from Java (QCMDEXC equivalent) |
| DataQueueService | Asynchronous message exchange via *DTAQ (SNDDTAQ/RCVDTAQ) |
| ProgramCallService | Native *PGM invocation with EBCDIC parameter conversion |
| PortfolioRepository | DB2 for i JDBC access (CHAIN, READ, UPDATE equivalents) |

### Layer 4 — REST API

Eight endpoints mapping to IBM i integration patterns:

| Endpoint | IBM i Operation |
|----------|----------------|
| `GET /portfolios` | Read loop on ACTIVE_PORTFOLIOS view |
| `GET /portfolios/{id}` | Keyed read (CHAIN equivalent) |
| `PUT /portfolios/{id}/value` | Journaled UPDATE + COMMIT |
| `GET /orders/pending` | Cursor-based read of pending orders |
| `POST /orders/enqueue` | DataQueue write (SNDDTAQ) |
| `GET /orders/dequeue` | DataQueue read with timeout (RCVDTAQ) |
| `GET /eligibility` | ProgramCall with EBCDIC parameters |
| `GET /system/ping` | CL command execution health check |

---

## Test Strategy

Testing spans three tiers, all following the Arrange-Act-Assert (AAA) pattern:

| Tier | Scope | Technology | Count |
|------|-------|-----------|-------|
| **Unit** | Business logic in isolation | H2 in-memory DB, Mockito, JUnit 5 | 16 Java tests + 6 RPGLE tests (UTEST01) |
| **Integration** | Live DB2 for i round-trips | PUB400 via JDBC + CL test driver (ITEST01) | 4 Java tests + 3 CL tests |
| **System** | Full HTTP to IBM i end-to-end | Spring MockMvc + live PUB400 + RPGLE batch test (STEST01) | 5 Java tests + 4 RPGLE tests |

The Java unit tests run against H2 with no IBM i connection required, making them suitable for any CI/CD pipeline. Integration and system tests require a live IBM i connection and are activated via a Maven profile.

---

## Technology Stack

| Layer | Technologies |
|-------|-------------|
| **IBM i native** | RPGLE (fully-free), COBOL/400, CL (CLLE), DDS, SQLRPGLE |
| **Database** | DB2 for i with journaling and commitment control |
| **Java** | Java 21, Spring Boot 3.3, JT400 20.0, Maven |
| **Testing** | JUnit 5, Mockito, MockMvc, H2, AAA-pattern native RPGLE/CL tests |
| **IDE** | VS Code with Code for IBM i extension |
| **IBM i access** | ACS 5250 emulator, Tn5250j, SSH (PASE) |
| **Target system** | PUB400.com (public IBM i 7.5) |

---

## Repository Structure

```
as400-ibmi-batch-simulator/
  docs/
    PROJECT_OVERVIEW.md          <- This file
    Layer1_Layer2_IBMi_Native.md <- IBM i programs and DB2 (pseudocode + concepts)
    Layer3_Layer4_Java_REST.md   <- Spring Boot + JT400 REST API
    Layer5_5250_Display_UI.md    <- Optional 5250 green-screen UI
  src/
    ibmi/                        <- IBM i source (RPG, COBOL, CL, SQL, DDS)
    main/java/                   <- Spring Boot application
    test/java/                   <- Java test suite (unit + integration + system)
    test/resources/              <- H2 schema, seed data, test config
  pom.xml
```

---

## Supporting Documentation

For detailed design, pseudocode, and IBM i concept explanations, see:

- **[Layer 1 & 2 — IBM i Native Development](Layer1_Layer2_IBMi_Native.md)** — DB2 database design, RPG/COBOL/CL program logic, ILE service program architecture, batch processing patterns, and native test suite
- **[Layer 3 & 4 — Java Spring Boot + JT400 REST API](Layer3_Layer4_Java_REST.md)** — JT400 integration services, REST endpoint design, domain models, and Java test strategy
- **[Layer 5 — 5250 Display File UI](Layer5_5250_Display_UI.md)** — Optional native green-screen interface demonstrating DDS display files and interactive workstation programming

---

*Built on PUB400.com (IBM i 7.5) as a self-directed skill-building project.*
