# AS400 IBM i Batch Settlement Simulator

A self-directed skill-building project demonstrating foundational competency in the IBM i (AS400) technology stack. The system models a simplified portfolio management and trade-order settlement pipeline, spanning native IBM i programs (RPGLE, COBOL/400, CL), a Java Spring Boot REST API via JT400, and an optional 5250 green-screen UI.

Built on PUB400.com (free public IBM i 7.5 system).

> All test data is fictional. Portfolio owner names are characters from *The Hitchhiker's Guide to the Galaxy*. ISIN-like values are dummy identifiers.

## Architecture

```
Layer 4 — REST API (Spring Boot)
  PortfolioController   → 8 REST endpoints under /api/v1

Layer 3 — IBM i Integration Services (JT400)
  CommandExecutorService→ CL command runner (QCMDEXC)
  DataQueueService      → *DTAQ write/read (SNDDTAQ/RCVDTAQ)
  ProgramCallService    → *PGM caller (EBCDIC parameter handling)
  PortfolioRepository   → DB2 for i JDBC (CHAIN/READ/UPDATE)

Layer 2 — IBM i Programs (native RPG / COBOL / CL)
  PORTFINQ   *PGM    — RPGLE portfolio inquiry (embedded SQL)
  PORTFCBL   *PGM    — COBOL/400 portfolio inquiry (same logic)
  ORDPROC    *PGM    — CL driver calling both RPG and COBOL
  PORTFSVC   *SRVPGM — ILE service program (3 exported procedures)
  PORTFTEST  *PGM    — Functional test exercising PORTFSVC
  ORDRBATCH  *PGM    — Batch processor (SQL cursor + periodic COMMIT)
  PORTFUI    *PGM    — 5250 green-screen UI for interactive validation

Layer 1 — DB2 for i (shared database)
  PORTFOLIO         — Master portfolio data (USD, CHF, EUR, PLN)
  TRADE_ORDERS      — Trade order staging (PEND -> PROC lifecycle)
  ACTIVE_PORTFOLIOS — SQL view (active portfolios only)
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
- **[Layer 5 — 5250 Display File UI](docs/Layer5_5250_Display_UI.md)** — native green-screen interface with DDS display files
- [Compiling RPGLE from VS Code](docs/compiling-rpgle-from-vscode.md)
