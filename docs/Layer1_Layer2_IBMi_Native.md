# DOC 1 v2.00 — Layer 1 & Layer 2: IBM i Native Development
## DB2 for i Database + RPG / COBOL / CL Programs on PUB400

An end-to-end IBM i portfolio management and batch settlement system. Covers DB2 for i database design, native COBOL/400 and RPGLE programs, ILE service program architecture, CL job drivers, and a full test suite across unit, integration, and system tiers. All source code is available in the GitHub repository.

> All test data in this project is fictional. Portfolio owner names (Arthur Dent, Ford Prefect, Trillian, Zaphod Beeblebrox, Marvin) are characters from *The Hitchhiker's Guide to the Galaxy*. ISIN-like identifiers are dummy values and do not represent real securities.

---

## Table of Contents

1. [What You Are Building](#1-what-you-are-building)
2. [Development Environment](#2-development-environment)
3. [VS Code Setup for IBM i](#3-vs-code-setup-for-ibm-i)
4. [Connecting to PUB400](#4-connecting-to-pub400)
5. [Layer 1 — DB2 for i: Create the Database](#5-layer-1--db2-for-i-create-the-database)
6. [Layer 2A — PORTFINQ: RPGLE Portfolio Inquiry](#6-layer-2a--portfinq-rpgle-portfolio-inquiry)
7. [Layer 2B — PORTFCBL: COBOL/400 Portfolio Inquiry](#7-layer-2b--portfcbl-cobol400-portfolio-inquiry)
8. [Layer 2C — ORDPROC: CL Driver Program](#8-layer-2c--ordproc-cl-driver-program)
9. [Layer 2D — PORTFSVC: ILE Service Program](#9-layer-2d--portfsvc-ile-service-program)
10. [Layer 2E — PORTFTEST: Service Program Caller](#10-layer-2e--portftest-service-program-caller)
11. [Layer 2F — ORDRBATCH: Batch Order Processor](#11-layer-2f--ordrbatch-batch-order-processor)
12. [Test Suite (AAA Format)](#12-test-suite-aaa-format)
13. [Self-Verification Checklist](#13-self-verification-checklist)
14. [Source in the Git Repository](#14-source-in-the-git-repository)

---

## 1. What You Are Building

Layer 1 and Layer 2 form the IBM i native core. Everything runs directly on PUB400, a free public IBM i 7.5 system. Source is written in VS Code, uploaded to PUB400, compiled with native IBM i compilers, and executed on the IBM i OS.

```text
LAYER 2 — IBM i Programs (native RPG / COBOL / CL)
  PORTFINQ   *PGM    — RPGLE portfolio inquiry (embedded SQL)
  PORTFCBL   *PGM    — COBOL/400 portfolio inquiry (same logic, same table)
  ORDPROC    *PGM    — CL driver calling both RPGLE and COBOL programs
  PORTFSVC   *SRVPGM — ILE service program (3 exported validation procedures)
  PORTFTEST  *PGM    — Caller that exercises PORTFSVC
  ORDRBATCH  *PGM    — Batch processor with SQL cursor + periodic COMMIT

LAYER 1 — DB2 for i (shared database)
  CODELIVER1.PORTFOLIO         — Physical file (master portfolio data)
  CODELIVER1.TRADE_ORDERS      — Physical file (trade order staging)
  CODELIVER1.ACTIVE_PORTFOLIOS — SQL View (active portfolios only)
```

**Working library/schema:** `CODELIVER1`

---

## 2. Development Environment

The following tools are required on macOS:

- **Homebrew** — macOS package manager
- **SSH** — built in on macOS; used to connect to PUB400 PASE shell
- **VS Code** with the following extensions:
  - Code for IBM i (Halcyon-Tech) — edit, compile, and run RPG/CL/COBOL on IBM i
  - RPG & CL (Halcyon-Tech) — syntax highlighting for RPGLE and CL
  - IBM i Languages (Halcyon-Tech) — COBOL/400 syntax support
  - GitLens, YAML, REST Client — supporting tools
- **IBM i Access Client Solutions (ACS)** — Java-based desktop tool providing the 5250 green-screen terminal, useful for native IBM i verification (WRKOBJ, DSPJOBLOG, WRKSPLF)
- **Java 21** (Temurin LTS) — required to run ACS and the Spring Boot layer
- **Git** — version control

ACS is configured with a system entry pointing to `pub400.com` on telnet port 23. After sign-on, the green screen provides a native IBM i command line.

---

## 3. VS Code Setup for IBM i

A Code for IBM i connection profile is created in VS Code with the PUB400 host, user credentials, and default library set to `CODELIVER1`.

Two object browser filters are configured:

| Filter | Purpose |
|--------|---------|
| `CODELIVER1-SRC` | Browse source physical files and members (QRPGLESRC, QCBLLESRC, QCLSRC, QSQLSRC, etc.) |
| `CODELIVER1-OBJ` | Browse compiled IBM i objects (*PGM, *MODULE, *SRVPGM, *BNDDIR) |

The Spooled File Browser extension is also configured to view job logs and compile listings directly in VS Code.

---

## 4. Connecting to PUB400

An SSH config entry for PUB400 is set up on port 2222. After connecting, the PASE shell provides access to the `system` command, which runs CL commands from bash.

At every session start, the job CCSID is set to 37 to prevent EBCDIC encoding issues:

```text
system "CHGJOB CCSID(37)"
```

The 5250 green screen is available via ACS for native IBM i operations like WRKOBJ, DSPJOBLOG, and WRKSPLF.

---

## 5. Layer 1 — DB2 for i: Create the Database

### 5.1 Source Physical Files

Six source physical files are created in library CODELIVER1 to hold source members of different types:

| Source File | Purpose |
|-------------|---------|
| QRPGLESRC | RPG source members |
| QCLSRC | CL source members |
| QCBLLESRC | COBOL source members |
| QSQLSRC | SQL source members |
| QDDSSRC | DDS source members |
| QSRVSRC | Binder source members |

Each is created with RCDLEN(112) to allow 100 usable source columns after the 12-byte sequence/date prefix.

### 5.2 Database Tables

The SQL source member CRTTABLES defines three database objects:

**PORTFOLIO table (physical file)**

| Column | Type | Description |
|--------|------|-------------|
| PORTF_ID | CHAR(10) | Primary key |
| OWNER | CHAR(40) | Portfolio owner name |
| CURRENCY | CHAR(3) | Currency code (USD, EUR, CHF, PLN, GBP) |
| TOTAL_VALUE | DECIMAL(15,2) | Portfolio total value |
| STATUS | CHAR(1) | A=Active, I=Inactive |
| LAST_UPD | DATE | Last update date |

**TRADE_ORDERS table (physical file)**

| Column | Type | Description |
|--------|------|-------------|
| ORDER_ID | CHAR(20) | Primary key |
| PORTF_ID | CHAR(10) | Foreign key to PORTFOLIO |
| ISIN | CHAR(12) | Security identifier |
| QUANTITY | DECIMAL(15,4) | Order quantity |
| PRICE | DECIMAL(15,4) | Order price |
| ORDER_DT | DATE | Order date |
| PROCESS_DT | DATE | Processing date (populated by batch) |
| STATUS | CHAR(4) | PEND=Pending, PROC=Processed |

**ACTIVE_PORTFOLIOS view** — an SQL view over PORTFOLIO filtered to STATUS='A'. This is the logical file equivalent on IBM i: a secondary access path over a physical file.

### 5.3 Seed Data

Five fictional portfolios and five trade orders are inserted:

| ID | Owner | Currency | Value | Status |
|----|-------|----------|-------|--------|
| PF001 | Arthur Dent | USD | 150,000.00 | Active |
| PF002 | Ford Prefect | CHF | 280,000.00 | Active |
| PF003 | Trillian | EUR | 95,000.00 | Inactive |
| PF004 | Zaphod Beeblebrox | EUR | 320,000.00 | Active |
| PF005 | Marvin | PLN | 175,000.00 | Active |

The SQL is executed via RUNSQLSTM from the PASE shell. The script is rerunnable: it drops existing objects before recreating them.

---

## 6. Layer 2A — PORTFINQ: RPGLE Portfolio Inquiry

### What It Demonstrates

| IBM i Concept | Detail |
|---------------|--------|
| `**FREE` fully-free format | No column restrictions |
| `CTL-OPT` | Control options — activation group, debug settings |
| `DCL-PI` | Procedure Interface — equivalent to COBOL LINKAGE SECTION |
| `EXEC SQL` embedded SQL | SQLRPGLE — SQL inside RPG |
| `SQLCODE` handling | 0=found, 100=not found, negative=error |
| `*INLR = *ON` | Clean program exit — equivalent to COBOL STOP RUN |

### Program Logic (Pseudocode)

```text
PROGRAM PORTFINQ
  PARAMETERS:
    IN:  portfolio-id (CHAR 10)
    OUT: owner (CHAR 40), total-value (DECIMAL 15,2), currency (CHAR 3), return-code (CHAR 2)

  EXEC SQL SELECT owner, total_value, currency
           INTO working variables
           FROM PORTFOLIO
           WHERE portf_id = input portfolio-id

  IF SQLCODE = 0 THEN
    Copy working variables to output parameters
    Set return-code = '00'
  ELSE IF SQLCODE = 100 THEN
    Clear output parameters
    Set return-code = '10'  (not found)
  ELSE
    Set return-code = '99'  (SQL error)

  Set *INLR = *ON and RETURN
```

### Compile and Run

The program is compiled with CRTSQLRPGI (SQL precompile + RPG compile in one step) and called with padded CHAR parameters. The job log shows the return code and retrieved data.

---

## 7. Layer 2B — PORTFCBL: COBOL/400 Portfolio Inquiry

### What It Demonstrates

PORTFCBL is the COBOL/400 version of PORTFINQ. It reads the same DB2 table, uses the same embedded SQL, and returns the same outputs — but in standard COBOL syntax. This demonstrates that COBOL expertise transfers directly to IBM i.

| COBOL/400 Syntax | z/OS COBOL Equivalent | Notes |
|------------------|----------------------|-------|
| IDENTIFICATION DIVISION | Same | Identical |
| LINKAGE SECTION | Same | Identical |
| EXEC SQL SELECT INTO | Same | Same SQL dialect |
| EVALUATE SQLCODE | Same | Same logic |
| PIC S9(13)V99 COMP-3 | Same | Packed decimal — identical |
| CRTSQLCBLI | IGYCRCTL | Different compile command, same concept |

### Program Logic (Pseudocode)

```text
PROGRAM PORTFCBL
  LINKAGE SECTION:
    IN:  portfolio-id (PIC X(10))
    OUT: owner (PIC X(40)), total-value (PIC S9(13)V99 COMP-3), currency (PIC X(3)), return-code (PIC X(2))

  EXEC SQL SELECT owner, total_value, currency
           INTO working-storage variables
           FROM PORTFOLIO
           WHERE portf_id = input portfolio-id

  EVALUATE SQLCODE
    WHEN 0:   Move results to linkage, set return-code = '00'
    WHEN 100: Clear linkage fields, set return-code = '10'
    OTHER:    Set return-code = '99'

  STOP RUN
```

Compiled with CRTSQLCBLI. The COBOL/400 column format (Area A cols 8-11, Area B cols 12-72) is identical to z/OS.

---

## 8. Layer 2C — ORDPROC: CL Driver Program

### What It Demonstrates

ORDPROC is a CL (Control Language) program that acts as a job driver. CL is IBM i's equivalent of JCL, but it is a real programming language with variables, conditional logic, and error handling.

| CL Concept | z/OS Equivalent |
|------------|-----------------|
| PGM / ENDPGM | JCL EXEC PGM= block |
| DCL VAR(&X) TYPE(*CHAR) | JCL symbolic or COBOL WORKING-STORAGE |
| MONMSG MSGID(CPF0000) | JCL COND= or COBOL ON EXCEPTION |
| CALL PGM(LIB/PGM) PARM(...) | EXEC PGM= with PARM or COBOL CALL |
| SNDPGMMSG | DSPLY in COBOL or SYSOUT |

### Program Logic (Pseudocode)

```text
PROGRAM ORDPROC
  DECLARE variables: portfolio-id, owner, total-value, currency, return-code, message
  SET global error handler via MONMSG

  LOG "Calling PORTFINQ (RPGLE)"
  CALL PORTFINQ with portfolio-id = 'PF001'
  IF return-code = '00' THEN
    LOG "RPGLE result: <owner> <currency>"

  Reset output variables

  LOG "Calling PORTFCBL (COBOL/400)"
  CALL PORTFCBL with portfolio-id = 'PF001'
  IF return-code = '00' THEN
    LOG "COBOL result: <owner> <currency>"

  EXIT (or jump to error handler on failure)
```

This demonstrates cross-language integration: a CL driver calling both RPGLE and COBOL/400 programs that read the same DB2 table.

---

## 9. Layer 2D — PORTFSVC: ILE Service Program

### What It Demonstrates

A `*SRVPGM` (Service Program) is a shared library of reusable exported procedures. It stays resident in memory across calls, analogous to a shared library or DLL.

| IBM i ILE Concept | Modern Equivalent |
|-------------------|-------------------|
| *MODULE | .o object file before linking |
| *SRVPGM | Shared library / JAR-style utility library |
| Exported procedure | Public method or function |
| *BNDDIR | Dependency management / library search path |
| Binder source | API contract and versioning metadata |
| CRTRPGMOD | Compile source to module |
| CRTSRVPGM | Link/build shared library |

PORTFSVC exports three reusable validation procedures:

```text
VALIDATEPORTFOLIO   — checks status and value, returns '00', '10', or '20'
VALIDATECURRENCY    — checks currency code against supported list (USD, EUR, CHF, GBP, PLN)
FORMATPORTFOLIOMSG  — builds a human-readable summary string
```

### Program Logic (Pseudocode)

```text
SERVICE PROGRAM PORTFSVC  (CTL-OPT NOMAIN — no standalone entry point)

  PROCEDURE validatePortfolio(status, totalValue) RETURNS CHAR(2)
    IF status <> 'A' THEN RETURN '10'  (inactive)
    IF totalValue <= 0 THEN RETURN '20' (zero/negative value)
    RETURN '00'  (valid)

  PROCEDURE validateCurrency(currency) RETURNS INDICATOR
    IF currency IN ('USD', 'EUR', 'CHF', 'GBP', 'PLN') THEN RETURN TRUE
    ELSE RETURN FALSE

  PROCEDURE formatPortfolioMsg(portfolioId, owner, totalValue, currency) RETURNS VARCHAR(100)
    RETURN portfolioId + ' | ' + owner + ' | ' + currency + ' ' + totalValue
```

### Two-Step ILE Build

```text
Source (.rpgle)
    |
    v  CRTRPGMOD
*MODULE  (compiled but not yet runnable)
    |
    v  CRTSRVPGM EXPORT(*ALL)
*SRVPGM  (shareable library of exported procedures)
```

The z/OS equivalent: IGYCRCTL compile, then IEWL link-edit into a shared load module.

### Enterprise Patterns

Production IBM i environments typically use:

1. **Shared prototype members** — one RPGLEINC member containing all DCL-PR prototypes, included by consumers via /COPY
2. **Binder source** — explicit API contracts via STRPGMEXP/ENDPGMEXP instead of EXPORT(*ALL)
3. **Binding directories** — centralized dependency management via CRTBNDDIR + ADDBNDDIRE

---

## 10. Layer 2E — PORTFTEST: Service Program Caller

### What It Demonstrates

PORTFTEST calls all three PORTFSVC procedures using DCL-PR prototypes. The prototype provides compile-time type checking by declaring the exact signature of each external procedure.

### Program Logic (Pseudocode)

```text
PROGRAM PORTFTEST
  DECLARE file PORTFOUT for output (100-byte records)
  DECLARE prototypes for validatePortfolio, validateCurrency, formatPortfolioMsg
  INITIALIZE test data: status='A', value=150000.00, currency='USD', portfolioId='PF001', owner='Arthur Dent'

  TEST 1: Call validatePortfolio(status, value)
    IF result = '00' THEN write "TEST1 PASS" to PORTFOUT
    ELSE write "TEST1 FAIL" to PORTFOUT

  TEST 2: Call validateCurrency(currency)
    IF result = TRUE THEN write "TEST2 PASS" to PORTFOUT
    ELSE write "TEST2 FAIL" to PORTFOUT

  TEST 3: Call formatPortfolioMsg(portfolioId, owner, value, currency)
    Write "TEST3 MSG: <formatted output>" to PORTFOUT

  Set *INLR and RETURN
```

Expected output in PORTFOUT (viewable via DSPPFM):

```text
TEST1 PASS: Portfolio is valid (status=A, value>0)
TEST2 PASS: Currency USD is valid
TEST3 MSG: PF001 | Arthur Dent | USD 150000.00
```

Compiled with CRTBNDRPG using the PORTFBNDD binding directory that references the PORTFSVC service program.

---

## 11. Layer 2F — ORDRBATCH: Batch Order Processor

### What It Demonstrates

ORDRBATCH demonstrates the most operationally relevant pattern: nightly batch settlement with cursor-based processing and periodic commits. It models the kind of batch cycle common in wealth management systems — processing pending orders, transitioning their status, and committing at intervals for recoverability.

| IBM i Concept | z/OS Equivalent |
|---------------|-----------------|
| COMMIT(*CHG) on CRTSQLRPGI | EXEC SQL SET TRANSACTION / CICS SYNCPOINT |
| SQL cursor with ORDER BY | COBOL cursor fetch |
| EXEC SQL COMMIT / ROLLBACK | Same syntax |
| %REM(count:10) for periodic commit | FUNCTION MOD(count, 10) in COBOL |

### Program Logic (Pseudocode)

```text
PROGRAM ORDRBATCH
  LOG "ORDRBATCH started"

  DECLARE CURSOR C_ORDERS FOR
    SELECT order_id, portf_id, isin, quantity, price
    FROM TRADE_ORDERS WHERE status = 'PEND'
    ORDER BY order_dt, order_id

  OPEN C_ORDERS
  IF open fails THEN log error and exit

  LOOP:
    FETCH next row from C_ORDERS
    IF no more rows THEN exit loop
    IF fetch error THEN ROLLBACK, log fatal, exit loop

    UPDATE TRADE_ORDERS
      SET status = 'PROC', process_dt = CURRENT_DATE
      WHERE order_id = fetched order_id AND status = 'PEND'

    IF update error THEN ROLLBACK, log fatal, exit loop

    INCREMENT processed count
    IF processed count is multiple of 10 THEN
      COMMIT
      IF commit error THEN ROLLBACK, log fatal, exit loop
      LOG "committed <count> rows"

  END LOOP

  IF remaining uncommitted rows THEN COMMIT (final batch)
  CLOSE C_ORDERS
  LOG "ORDRBATCH ended. Processed=<count>, Committed=<committed>"
```

### Journaling Requirement

Because ORDRBATCH compiles with COMMIT(*CHG), the TRADE_ORDERS physical file must be journaled before the program can update under commitment control. A journal receiver and journal are created once, then journaling is started on the physical file. Without journaling, DB2 returns SQL7008 and the updates fail.

---

## 12. Test Suite (AAA Format)

All tests follow the **Arrange, Act, Assert** pattern. Each test writes PASS/FAIL messages to the job log. On failure, an escape message (CPF9898) is sent so the failure is visible in DSPJOBLOG.

### 12.1 UTEST01 — Unit Test: PORTFSVC Validation

**Scope:** Tests the three PORTFSVC procedures in isolation (no database access).

| Test Case | Arrange | Act | Assert |
|-----------|---------|-----|--------|
| TC-U-01 | Status='A', Value=150000 | validatePortfolio | Returns '00' |
| TC-U-02 | Status='I', Value=150000 | validatePortfolio | Returns '10' |
| TC-U-03 | Status='A', Value=0 | validatePortfolio | Returns '20' |
| TC-U-04 | Currency='USD' | validateCurrency | Returns TRUE |
| TC-U-05 | Currency='XXX' | validateCurrency | Returns FALSE |
| TC-U-06 | PF001, Arthur Dent, USD | formatPortfolioMsg | Output contains all fields |

Compiled with CRTBNDRPG and the PORTFBNDD binding directory.

### 12.2 ITEST01 — Integration Test: DB2 Round-Trip

**Scope:** Tests real database access through both inquiry programs.

| Test Case | Program | Input | Expected |
|-----------|---------|-------|----------|
| TC-I-01 | PORTFINQ | PF001 (exists) | Return code '00' |
| TC-I-02 | PORTFINQ | XXXXX (not found) | Return code '10' |
| TC-I-03 | PORTFCBL | PF002 (exists) | Return code '00' |

Written in CL (CLLE), compiled with CRTBNDCL.

### 12.3 STEST01 — System Test: End-to-End Batch Settlement

**Scope:** Tests the full business flow automatically:

```text
TC-S-01: Reset all orders to PEND (Arrange)
TC-S-02: Call ORDRBATCH (Act)
TC-S-03: Assert zero PEND rows remain
TC-S-04: Assert all rows have PROCESS_DT populated
```

Written in SQLRPGLE, compiled with CRTSQLRPGI. This exercises DB2 plus the batch program from end to end with embedded SQL assertions.

---

## 13. Self-Verification Checklist

### Expected Object Inventory

| Object | Type | Description |
|--------|------|-------------|
| PORTFOLIO | *FILE (PF) | Master portfolio physical file |
| TRADE_ORDERS | *FILE (PF) | Trade orders physical file |
| PORTFINQ | *PGM | RPGLE portfolio inquiry |
| PORTFCBL | *PGM | COBOL/400 portfolio inquiry |
| ORDPROC | *PGM | CL driver |
| PORTFOUT | *FILE (PF) | Persistent output for PORTFTEST |
| PORTFSVC | *MODULE | Compiled module |
| PORTFSVC | *SRVPGM | ILE service program (3 exports) |
| PORTFBNDD | *BNDDIR | Binding directory |
| PORTFTEST | *PGM | Service program caller |
| ORDRBATCH | *PGM | Batch order processor |
| UTEST01 | *PGM | Unit test |
| ITEST01 | *PGM | Integration test |
| STEST01 | *PGM | System test |

### Database Verification

After running STEST01:

- PORTFOLIO table contains 5 rows (PF001 through PF005)
- TRADE_ORDERS: all rows have STATUS='PROC' and PROCESS_DT populated
- ACTIVE_PORTFOLIOS view returns 4 rows (excludes inactive PF003)

### Service Program Verification

DSPSRVPGM PORTFSVC with DETAIL(*PROCEXP) shows three exports:

```text
VALIDATEPORTFOLIO
VALIDATECURRENCY
FORMATPORTFOLIOMSG
```

---

## 14. Source in the Git Repository

All IBM i source is stored in the GitHub repository under the `src/ibmi/` directory so the source code can be reviewed without requiring access to a 5250 terminal.

```text
as400-ibmi-batch-simulator/
  src/ibmi/
    sql/         CRTTABLES.sql
    rpgle/       PORTFSVC.rpgle, PORTFTEST.rpgle, UTEST01.rpgle
    sqlrpgle/    PORTFINQ.sqlrpgle, ORDRBATCH.sqlrpgle, STEST01.sqlrpgle
    cobol/       PORTFCBL.cbl
    clle/        ORDPROC.clle, ITEST01.clle, LOGMSG.clle
    srvsrc/      PORTFSVC.bnd
    include/     PORTFSVCPR.rpgleinc
    dds/         PORTFDSPL.dspf
```

Each source file includes a comment header explaining its purpose, the IBM i concept it demonstrates, and the z/OS equivalent where applicable.

---

*End of DOC 1 v2.00 — Layer 1 & Layer 2: IBM i Native Development*
