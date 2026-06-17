# AS400 IBM i Batch Settlement Simulator

A focused IBM i (AS400) portfolio management and batch settlement system built on **DB2 for i** and **COBOL/400** alongside RPGLE, CL, and a Java Spring Boot REST API. Covers the full production stack: database design with journaling and commitment control, native program development, ILE service program architecture, and a modern REST integration layer — the same technology mix used in banking and wealth management environments.

Built on PUB400.com (free public IBM i 7.5 system).

> All test data is fictional. Portfolio owner names are characters from *The Hitchhiker's Guide to the Galaxy*. ISIN-like values are dummy identifiers.

## Core Skills Demonstrated

| Skill | How It Appears in This Project |
|-------|-------------------------------|
| **COBOL/400** | PORTFCBL — embedded SQL portfolio inquiry with EXEC SQL, SQLCA, EVALUATE SQLCODE, LINKAGE SECTION. Identical syntax to z/OS COBOL. |
| **DB2 for i** | Physical files with journaling, foreign keys, SQL views, embedded SQL in both COBOL and RPG, cursor-based batch processing with COMMIT/ROLLBACK |
| **RPGLE** | Fully-free format, ILE service programs (*SRVPGM), exported procedures, batch settlement with SQL cursors |
| **CL (Control Language)** | Job driver calling both COBOL and RPG programs, MONMSG error handling, cross-language integration |
| **Java / Spring Boot** | REST API layer wrapping IBM i via JT400 — DB2 JDBC, DataQueue I/O, ProgramCall with EBCDIC conversion |
| **5250 Green Screen** | DDS display file + RPGLE workstation program for interactive validation |

## Architecture

```
Layer 4 — REST API (Spring Boot)
  PortfolioController   → 8 REST endpoints under /api/v1

Layer 3 — IBM i Integration Services (JT400)
  CommandExecutorService→ CL command runner (QCMDEXC)
  DataQueueService      → *DTAQ write/read (SNDDTAQ/RCVDTAQ)
  ProgramCallService    → *PGM caller (EBCDIC parameter handling)
  PortfolioRepository   → DB2 for i JDBC (CHAIN/READ/UPDATE)

Layer 2 — IBM i Programs (native COBOL / RPG / CL)
  PORTFCBL   *PGM    — COBOL/400 portfolio inquiry (embedded SQL, SQLCA)
  PORTFINQ   *PGM    — RPGLE portfolio inquiry (same DB2 table, same logic)
  ORDPROC    *PGM    — CL driver calling both COBOL and RPG programs
  PORTFSVC   *SRVPGM — ILE service program (3 exported validation procedures)
  PORTFTEST  *PGM    — Functional test exercising PORTFSVC
  ORDRBATCH  *PGM    — Batch processor (DB2 cursor + periodic COMMIT)
  PORTFUI    *PGM    — 5250 green-screen UI for interactive validation

Layer 1 — DB2 for i (database engine)
  PORTFOLIO         — Master portfolio data (USD, CHF, EUR, PLN)
  TRADE_ORDERS      — Trade order staging with journaling (PEND -> PROC)
  ACTIVE_PORTFOLIOS — SQL view filtering active portfolios only
```

### DB2 for i Patterns Used

| Pattern | Where |
|---------|-------|
| Embedded SQL (SELECT INTO) | PORTFINQ (RPG), PORTFCBL (COBOL) |
| SQL cursor with FETCH loop | ORDRBATCH batch settlement |
| Commitment control (COMMIT/ROLLBACK) | ORDRBATCH periodic commit every 10 rows |
| Journaling (STRJRNPF) | Required for TRADE_ORDERS under commitment control |
| SQL views as logical files | ACTIVE_PORTFOLIOS view over PORTFOLIO |
| Foreign key constraints | TRADE_ORDERS.PORTF_ID references PORTFOLIO |

### COBOL/400 to z/OS COBOL Comparison

| Feature | COBOL/400 (this project) | z/OS COBOL |
|---------|-------------------------|------------|
| EXEC SQL SELECT INTO | Identical | Identical |
| SQLCA / SQLCODE handling | Identical | Identical |
| LINKAGE SECTION | Identical | Identical |
| PIC S9(13)V99 COMP-3 | Identical | Identical |
| EVALUATE SQLCODE | Identical | Identical |
| Compile command | CRTSQLCBLI | IGYCRCTL |

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
# Unit tests only (no IBM i required — runs against H2 in-memory)
mvn test

# Integration + system tests (requires live PUB400 connection)
mvn test -Pintegration

# Start the application
mvn spring-boot:run
```

## Test Suite

| Tier | Scope | Count |
|------|-------|-------|
| Unit | H2 in-memory DB, mocked AS400, JUnit 5 | 16 Java + 6 RPGLE |
| Integration | Live DB2 for i on PUB400 | 4 Java + 3 CL |
| System | Full HTTP to IBM i round-trip | 5 Java + 4 RPGLE |

## Security

- All credentials sourced from environment variables — nothing hardcoded.
- `.gitignore` excludes `.env`, credential files, and IDE-specific files.

## Documentation

- **[Project Overview](docs/PROJECT_OVERVIEW.md)** — architecture, business context, and full-stack summary
- **[Layer 1 & 2 — IBM i Native Development](docs/Layer1_Layer2_IBMi_Native.md)** — DB2 design, RPG/COBOL/CL program logic, ILE service programs, batch processing
- **[Layer 3 & 4 — Java Spring Boot + JT400](docs/Layer3_Layer4_Java_REST.md)** — JT400 integration services, REST API, domain models, Java test strategy
- **[Layer 2 — 5250 Display File UI](docs/Layer2_5250_Display_UI.md)** — native green-screen interface with DDS display files (extends Layer 2)
- [Compiling RPGLE from VS Code](docs/compiling-rpgle-from-vscode.md)
