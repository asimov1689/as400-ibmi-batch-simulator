# AS400 IBM i Batch Simulator

IBM i Portfolio Management System demonstrating enterprise integration between
native IBM i programs (RPGLE, COBOL/400, CL) and a Java Spring Boot REST API
via JT400 (IBM Toolbox for Java). Connects to PUB400.com (free public IBM i 7.5
system).

## Architecture

```
Layer 4 — REST API (Spring Boot Controller)
  PortfolioController   → 8 REST endpoints under /api/v1

Layer 3 — Service Layer (Spring Boot)
  PortfolioService      → orchestration, model-to-DTO mapping
  CommandExecutorService→ CL command runner (QCMDEXC)
  DataQueueService      → *DTAQ write/read (SNDDTAQ/RCVDTAQ)
  ProgramCallService    → *PGM caller (EBCDIC parameter handling)

Repository Layer
  PortfolioRepository   → DB2 for i JDBC (CHAIN/READ/UPDATE)

Layer 2 — IBM i Programs (native RPG/COBOL/CL)
  PORTFINQ, PORTFCBL, ORDPROC, PORTFSVC, PORTFTEST, ORDRBATCH

Layer 1 — DB2 for i (shared database)
  CODELIVER1.PORTFOLIO, CODELIVER1.TRADE_ORDERS, CODELIVER1.ACTIVE_PORTFOLIOS
```

## Project Structure

```
ibmi-batch-simulator/
├── pom.xml
├── .gitignore
├── src/
│   ├── main/java/com/example/ibmi/
│   │   ├── IbmiApplication.java
│   │   ├── config/
│   │   │   └── IbmiConnectionConfig.java
│   │   ├── controller/
│   │   │   └── PortfolioController.java
│   │   ├── dto/
│   │   │   ├── ApiResponse.java
│   │   │   ├── EnqueueRequest.java
│   │   │   ├── PortfolioDto.java
│   │   │   └── TradeOrderDto.java
│   │   ├── model/
│   │   │   ├── Portfolio.java
│   │   │   └── TradeOrder.java
│   │   ├── repository/
│   │   │   └── PortfolioRepository.java
│   │   └── service/
│   │       ├── PortfolioService.java
│   │       └── ibmi/
│   │           ├── CommandExecutorService.java
│   │           ├── DataQueueService.java
│   │           └── ProgramCallService.java
│   ├── main/resources/
│   │   └── application.yml
│   ├── ibmi/                          ← IBM i native source (DOC 1)
│   │   ├── clle/
│   │   ├── cobol/
│   │   ├── include/
│   │   ├── rpgle/
│   │   ├── sql/
│   │   ├── sqlrpgle/
│   │   └── srvsrc/
│   └── test/
│       ├── java/com/example/ibmi/
│       │   ├── unit/                  ← H2 in-memory, mocked AS400
│       │   ├── integration/           ← live PUB400 DB2 + JT400
│       │   └── system/                ← full HTTP round-trip to IBM i
│       ├── resources/
│       │   ├── application-test.yml
│       │   ├── schema-h2.sql
│       │   └── data-test.sql
│       └── http/
│           └── ibmi-tests.http
└── docs/
    ├── DOC1_Layer1_IBMi_Native_Development.md
    ├── DOC2_Layer3_Layer4_Java_SpringBoot_JT400.md
    └── ...
```

## Prerequisites

- Java 21+ (Temurin LTS)
- Maven 3.9+
- Environment variables (never committed):
  - `IBMI_HOST` — IBM i hostname
  - `IBMI_USER` — IBM i user profile
  - `IBMI_PASSWORD` — IBM i password
  - `IBMI_LIBRARY` — IBM i library/schema

## Running

```bash
# Unit tests only (no IBM i required)
mvn test

# Integration + system tests (requires live PUB400)
mvn test -Pintegration

# Start the application
mvn spring-boot:run
```

## Security

- All credentials are sourced from environment variables — nothing is hardcoded.
- `application.yml` contains no passwords, hostnames, or user profiles.
- `.gitignore` excludes `.env`, credential files, and IDE-specific files.

## Documentation

- [DOC1: IBM i Native Development](docs/DOC1_Layer1_IBMi_Native_Development.md)
- [DOC2: Java Spring Boot + JT400 REST API](docs/DOC2_Layer3_Layer4_Java_SpringBoot_JT400.md)
- [Compiling RPGLE from VS Code](docs/compiling-rpgle-from-vscode.md)
