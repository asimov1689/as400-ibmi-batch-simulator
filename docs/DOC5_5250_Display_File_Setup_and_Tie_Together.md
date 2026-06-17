# DOC 5 — 5250 Display File Setup, Verification & Project Tie-Together
## How DOC 4 Connects to the Existing Layers
### Platform: macOS · IDE: VS Code + ACS 5250 · Build: IBM i native compile

---

## Table of Contents

1. [The Big Picture — Where DOC 4 Fits](#1-the-big-picture--where-doc-4-fits)
2. [Prerequisites — What Must Already Work](#2-prerequisites--what-must-already-work)
3. [Repository File Structure After DOC 4](#3-repository-file-structure-after-doc-4)
4. [Promoting Source to the Repository](#4-promoting-source-to-the-repository)
5. [Compiling on PUB400 — Step by Step](#5-compiling-on-pub400--step-by-step)
6. [End-to-End Verification Checklist](#6-end-to-end-verification-checklist)
7. [Running the Full Stack Together](#7-running-the-full-stack-together)
8. [What the Interviewer Sees on GitHub](#8-what-the-interviewer-sees-on-github)
9. [Git Workflow — Committing DOC 4 Changes](#9-git-workflow--committing-doc-4-changes)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. The Big Picture — Where DOC 4 Fits

DOC 4 adds a native 5250 green-screen front end that calls the **same PORTFSVC
service program** already used by DOC 1 (PORTFTEST) and DOC 2 (REST API via
ProgramCallService). No business logic is duplicated — only a new presentation
layer is added.

```
┌─────────────────────────────────────────────────────────────────┐
│  YOUR LOCAL macOS MACHINE                                       │
│                                                                  │
│  LAYER 4 — REST API (Spring Boot, port 8080)       [DOC 2]      │
│  PortfolioController → 8 REST endpoints                          │
│  Swagger UI → /swagger-ui.html                                   │
│      │                                                           │
│  LAYER 3 — IBM i Integration Services (JT400)      [DOC 2]      │
│  PortfolioService (cached) → Repository → DB2 for i             │
│  DataQueueService → *DTAQ   ProgramCallService → *PGM            │
│      │                                                           │
└──────┼───────────────────────────────────────────────────────────┘
       │ JT400 / TCP
       │
┌──────▼───────────────────────────────────────────────────────────┐
│  IBM i (PUB400.COM)   Library: CODELIVER1                        │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  DOC 4 — 5250 Display File UI (NEW)                        │  │
│  │  PORTFDSPL  *FILE DSPF  — green-screen layout              │  │
│  │  PORTFUI    *PGM        — RPGLE workstation program        │  │
│  │      │                                                      │  │
│  │      └──── calls ────┐                                      │  │
│  └──────────────────────┼─────────────────────────────────────┘  │
│                         ▼                                        │
│  LAYER 2 — IBM i Programs (native RPG / COBOL / CL)  [DOC 1]   │
│  PORTFSVC   *SRVPGM ←── shared business rules                   │
│  PORTFTEST  *PGM         PORTFINQ  *PGM                         │
│  ORDRBATCH  *PGM         PORTFCBL  *PGM                         │
│  ORDPROC    *PGM                                                 │
│                                                                  │
│  LAYER 1 — DB2 for i (shared database)              [DOC 1]     │
│  CODELIVER1.PORTFOLIO         *FILE (PF)                        │
│  CODELIVER1.TRADE_ORDERS      *FILE (PF)                        │
│  CODELIVER1.ACTIVE_PORTFOLIOS  SQL View                         │
│                                                                  │
│  ORDERQ  *DTAQ — trade order queue                  [DOC 2]     │
└─────────────────────────────────────────────────────────────────┘

GitHub: github.com/asimov1689/as400-ibmi-batch-simulator
```

### Key point for interviews

Three different front ends — PORTFTEST (batch output), REST API (Spring Boot),
and PORTFUI (5250 screen) — all call the **same PORTFSVC service program**.
This demonstrates ILE separation of concerns: the business rules are written
once and consumed by any presentation layer.

| Front end | Technology | Where it runs | Calls PORTFSVC via |
|-----------|-----------|--------------|-------------------|
| PORTFTEST | RPGLE batch | PUB400 | Direct ILE bind (BNDDIR) |
| REST API | Spring Boot + JT400 | Local macOS | ProgramCallService / JDBC |
| PORTFUI | 5250 display file | PUB400 (ACS) | Direct ILE bind (BNDDIR) |

---

## 2. Prerequisites — What Must Already Work

Before starting DOC 4, verify these exist on PUB400:

```bash
ssh -p 2222 CODELIVER@pub400.com
system "CHGJOB CCSID(37)"

# Service program must exist and export 3 procedures
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
# Expected: VALIDATEPORTFOLIO, VALIDATECURRENCY, FORMATPORTFOLIOMSG

# Binding directory must contain PORTFSVC
system "DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)"
# Expected: PORTFSVC *SRVPGM

# QDDSSRC source physical file must exist
system "WRKOBJ OBJ(CODELIVER1/QDDSSRC) OBJTYPE(*FILE)"
# If missing: system "CRTSRCPF FILE(CODELIVER1/QDDSSRC) RCDLEN(112) TEXT('DDS Source')"

# PORTFTEST should work (proves PORTFSVC is healthy)
system "CLRPFM FILE(CODELIVER1/PORTFOUT)"
system "CALL CODELIVER1/PORTFTEST"
system "DSPPFM FILE(CODELIVER1/PORTFOUT)"
# Expected: TEST1 PASS, TEST2 PASS, TEST3 MSG
```

If any of these fail, go back to DOC 1 and fix the service program first.
Do not use DOC 4 to debug PORTFSVC.

---

## 3. Repository File Structure After DOC 4

DOC 4 adds two files to the existing `src/ibmi/` source tree:

```
ibmi-batch-simulator/
├── .gitignore
├── pom.xml
├── README.md
├── docs/
│   ├── DOC1_Layer1_IBMi_Native_Development.md
│   ├── DOC2_Layer3_Layer4_Java_SpringBoot_JT400.md
│   ├── DOC4_Optional_IBMi_5250_Display_File_UI.md
│   ├── DOC5_5250_Display_File_Setup_and_Tie_Together.md    ← this doc
│   └── ...
├── src/
│   ├── ibmi/                          ← IBM i native source
│   │   ├── clle/
│   │   │   ├── ITEST01.clle
│   │   │   ├── LOGMSG.clle
│   │   │   └── ORDPROC.clle
│   │   ├── cobol/
│   │   │   └── PORTFCBL.cbl
│   │   ├── dds/
│   │   │   └── PORTFDSPL.dspf         ← NEW (DOC 4) — display file
│   │   ├── include/
│   │   │   └── PORTFSVCPR.rpgleinc
│   │   ├── rpgle/
│   │   │   ├── PORTFSVC.rpgle
│   │   │   ├── PORTFTEST.rpgle
│   │   │   ├── PORTFUI.rpgle          ← NEW (DOC 4) — workstation program
│   │   │   └── UTEST01.rpgle
│   │   ├── sql/
│   │   │   └── CRTTABLES.sql
│   │   ├── sqlrpgle/
│   │   │   ├── ORDRBATCH.sqlrpgle
│   │   │   ├── PORTFINQ.sqlrpgle
│   │   │   └── STEST01.sqlrpgle
│   │   └── srvsrc/
│   │       └── PORTFSVC.bnd
│   ├── main/                          ← Java Spring Boot (DOC 2)
│   │   ├── java/com/example/ibmi/...
│   │   └── resources/...
│   └── test/                          ← Java tests (DOC 2)
│       ├── java/com/example/ibmi/...
│       ├── resources/...
│       └── http/...
```

Note: DOC 4 originally suggests a separate repository
(`ibmi-5250-portfolio-screen-validator`). We keep everything in one repo
instead — it is the same project, same library, same service program.

---

## 4. Promoting Source to the Repository

The source files are already promoted to the repo at:

```text
src/ibmi/dds/PORTFDSPL.dspf      ← DDS display file source
src/ibmi/rpgle/PORTFUI.rpgle     ← RPGLE workstation program source
```

These files are the local Git copies of the IBM i source members:

| Repo file | IBM i source member | Source physical file |
|-----------|-------------------|---------------------|
| `src/ibmi/dds/PORTFDSPL.dspf` | `CODELIVER1/QDDSSRC/PORTFDSPL` | QDDSSRC |
| `src/ibmi/rpgle/PORTFUI.rpgle` | `CODELIVER1/QRPGLESRC/PORTFUI` | QRPGLESRC |

To upload them to PUB400, use the VS Code IBM i panel:

1. Open `CODELIVER1-SRC` in the Object Browser
2. Right-click `QDDSSRC` → **New Member** → name `PORTFDSPL`, type `DSPF`
3. Paste the contents of `src/ibmi/dds/PORTFDSPL.dspf`
4. Save
5. Right-click `QRPGLESRC` → **New Member** → name `PORTFUI`, type `RPGLE`
6. Paste the contents of `src/ibmi/rpgle/PORTFUI.rpgle`
7. Save

---

## 5. Compiling on PUB400 — Step by Step

Both compiles must run on PUB400 — either via SSH/PASE or a 5250 session.

### Step 1 — Set CCSID (every session)

```bash
ssh -p 2222 CODELIVER@pub400.com
system "CHGJOB CCSID(37)"
```

### Step 2 — Compile the display file

```bash
system "CRTDSPF FILE(CODELIVER1/PORTFDSPL) SRCFILE(CODELIVER1/QDDSSRC) SRCMBR(PORTFDSPL)"
```

Expected:

```text
Display file PORTFDSPL created in library CODELIVER1.
```

### Step 3 — Compile the workstation program

```bash
system "CRTBNDRPG PGM(CODELIVER1/PORTFUI) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFUI) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
```

Expected:

```text
Program PORTFUI created in library CODELIVER1.
```

### Step 4 — Verify both objects

```bash
system "WRKOBJ OBJ(CODELIVER1/PORTFDSPL) OBJTYPE(*FILE)"
system "WRKOBJ OBJ(CODELIVER1/PORTFUI) OBJTYPE(*PGM)"
```

---

## 6. End-to-End Verification Checklist

### 6.1 Layer 1 — DB2 for i

```bash
system "STRSQL"
# SELECT COUNT(*) FROM CODELIVER1.PORTFOLIO      → 3 rows
# SELECT COUNT(*) FROM CODELIVER1.ACTIVE_PORTFOLIOS → 2 rows
```

### 6.2 Layer 2 — IBM i programs

```bash
# Service program healthy
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
# Expected: 3 exported procedures

# Batch test still works
system "CLRPFM FILE(CODELIVER1/PORTFOUT)"
system "CALL CODELIVER1/PORTFTEST"
system "DSPPFM FILE(CODELIVER1/PORTFOUT)"
# Expected: TEST1 PASS, TEST2 PASS, TEST3 MSG
```

### 6.3 DOC 4 — 5250 display file UI

This step **requires a real 5250 session** (ACS or similar). It will not work
from PASE bash or the VS Code integrated terminal.

From a 5250 command line:

```cl
CALL PGM(CODELIVER1/PORTFUI)
```

Test cases:

| Test | Input | Expected result |
|------|-------|----------------|
| PASS case | Status=A, Currency=USD, Value=150000.00 | `PASS: PF001 \| Richard Papen \| USD 150000.00` |
| FAIL — inactive | Status=I | `FAIL: Portfolio status is inactive.` |
| FAIL — zero value | Value=0 | `FAIL: Portfolio value is zero or negative.` |
| FAIL — bad currency | Currency=XYZ | `FAIL: Currency is not valid.` |
| Exit | Press F3 | Screen closes cleanly |

### 6.4 Layer 3+4 — Java REST API

```bash
cd ~/projects/ibmi-batch-simulator

# Unit tests (no IBM i needed)
mvn test
# Expected: 16 tests pass

# Full integration tests (requires PUB400 env vars)
mvn test -Pintegration
# Expected: 28 tests pass

# Start the app
mvn spring-boot:run

# Quick smoke test
curl -s http://localhost:8080/api/v1/system/ping | python3 -m json.tool
curl -s http://localhost:8080/api/v1/portfolios | python3 -m json.tool

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

### 6.5 All objects on PUB400

Run this final check to confirm every IBM i object exists:

```bash
system "WRKOBJ OBJ(CODELIVER1/*ALL) OBJTYPE(*ALL)"
```

Expected objects (minimum):

```text
PORTFOLIO     *FILE       Physical file (DOC 1)
TRADE_ORDERS  *FILE       Physical file — system name TRADE00001 (DOC 1)
ACTIVE_PORTFOLIOS         SQL View (DOC 1)
PORTFINQ      *PGM        RPGLE inquiry (DOC 1)
PORTFCBL      *PGM        COBOL/400 inquiry (DOC 1)
ORDPROC       *PGM        CL driver (DOC 1)
PORTFSVC      *MODULE     ILE module (DOC 1)
PORTFSVC      *SRVPGM     ILE service program (DOC 1)
PORTFBNDD     *BNDDIR     Binding directory (DOC 1)
PORTFTEST     *PGM        Service program caller (DOC 1)
PORTFOUT      *FILE       Test output file (DOC 1)
ORDRBATCH     *PGM        Batch processor (DOC 1)
ORDERQ        *DTAQ       Trade order data queue (DOC 2)
PORTFDSPL     *FILE DSPF  Display file (DOC 4)
PORTFUI       *PGM        Workstation program (DOC 4)
```

---

## 7. Running the Full Stack Together

### Demo sequence (under 10 minutes)

**Prep — have these open before starting:**

```text
Browser    : GitHub repo + Swagger UI tab
VS Code    : IBM i Source Browser showing CODELIVER1 members
ACS        : 5250 session signed in, command line ready
Terminal   : Spring Boot running (mvn spring-boot:run)
```

**Step 1 — GitHub (1 min)**
Show the repo structure. Point to `src/ibmi/` for native source and
`src/main/` for the Java layer. Show the README architecture diagram.

**Step 2 — Swagger UI (2 min)**
Open `http://localhost:8080/swagger-ui.html`. Hit `GET /api/v1/portfolios`
and `GET /api/v1/system/ping`. Show that every response includes an
`ibmiConcept` field explaining the IBM i operation.

**Step 3 — 5250 green screen (3 min)**
Switch to ACS. Run `CALL PGM(CODELIVER1/PORTFUI)`.
- Show the PASS case with default values
- Change Status to `I` → FAIL
- Change Currency to `XYZ` → FAIL
- Press F3 to exit

**Step 4 — Architecture one-liner (1 min)**

> "Three front ends — batch output, REST API, and 5250 screen — all call the
> same PORTFSVC service program. The business rules are written once in RPGLE
> and consumed by every presentation layer. That is the ILE separation of
> concerns pattern used in production IBM i banking systems."

---

## 8. What the Interviewer Sees on GitHub

When the interviewer opens the repo, they see:

```text
README.md                              ← architecture, endpoints, how to run
docs/
  DOC1_...md                           ← IBM i native development guide
  DOC2_...md                           ← Java Spring Boot + JT400 guide
  DOC4_...md                           ← 5250 display file guide
  DOC5_...md                           ← this tie-together doc
src/ibmi/
  dds/PORTFDSPL.dspf                   ← DDS — screen layout (24x80, fields, F3)
  rpgle/PORTFUI.rpgle                  ← RPGLE — EXFMT loop, calls PORTFSVC
  rpgle/PORTFSVC.rpgle                 ← shared service program source
  rpgle/PORTFTEST.rpgle                ← batch test calling same service program
  sqlrpgle/ORDRBATCH.sqlrpgle          ← batch processor with SQL cursor + COMMIT
  cobol/PORTFCBL.cbl                   ← COBOL/400 — identical EXEC SQL to z/OS
  ...
src/main/java/com/example/ibmi/
  controller/PortfolioController.java  ← REST API with OpenAPI annotations
  service/PortfolioService.java        ← orchestration with @Cacheable
  service/ibmi/...                     ← JT400 integration services
  repository/PortfolioRepository.java  ← DB2 for i JDBC
  ...
```

The key narrative: this is not a toy project with disconnected pieces. Every
layer talks to the same CODELIVER1 library on the same IBM i system. The
service program is the shared business logic consumed by three different
front ends.

---

## 9. Git Workflow — Committing DOC 4 Changes

### 9.1 What to commit

```text
src/ibmi/dds/PORTFDSPL.dspf                           ← new
src/ibmi/rpgle/PORTFUI.rpgle                           ← new
docs/DOC4_Optional_IBMi_5250_Display_File_UI.md        ← new
docs/DOC5_5250_Display_File_Setup_and_Tie_Together.md  ← new
README.md                                              ← updated
```

### 9.2 Commit message

```bash
git add src/ibmi/dds/PORTFDSPL.dspf \
        src/ibmi/rpgle/PORTFUI.rpgle \
        docs/DOC4_Optional_IBMi_5250_Display_File_UI.md \
        docs/DOC5_5250_Display_File_Setup_and_Tie_Together.md \
        README.md

git commit -m "Add 5250 display file UI (DOC 4) and tie-together guide (DOC 5)

PORTFDSPL display file and PORTFUI workstation program call the same
PORTFSVC service program used by PORTFTEST and the REST API.
Three front ends, one shared business logic layer."
```

### 9.3 Branch and PR workflow

```bash
# Already on branch: 5250-display-ui
git push -u origin 5250-display-ui

# Create PR
gh pr create --title "Add 5250 display file UI layer" \
  --body "Adds PORTFDSPL (DDS display file) and PORTFUI (RPGLE workstation program).
Both call PORTFSVC — the same service program used by PORTFTEST and the REST API.
Includes DOC4 guide and DOC5 tie-together documentation."

# After review, merge
gh pr merge --merge --delete-branch
```

---

## 10. Troubleshooting

### PORTFDSPL not found during PORTFUI compile

The display file must be compiled before the program. Run:

```bash
system "CRTDSPF FILE(CODELIVER1/PORTFDSPL) SRCFILE(CODELIVER1/QDDSSRC) SRCMBR(PORTFDSPL)"
```

### PORTFUI binding errors

Verify the binding directory and service program:

```bash
system "DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)"
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
```

The three exported procedures must match the `DCL-PR EXTPROC()` names in
PORTFUI exactly: `VALIDATEPORTFOLIO`, `VALIDATECURRENCY`, `FORMATPORTFOLIOMSG`.

### Screen does not display from VS Code terminal

That is expected. 5250 display files require a real 5250 terminal session.
Use IBM ACS (5250 Emulator) or any TN5250 client. Do not attempt to run
`CALL PGM(CODELIVER1/PORTFUI)` from PASE bash.

### F3 does not exit

Confirm the DDS has `CA03(03 'Exit')` on the file level and the RPG checks
`*IN03` in the DOW loop.

### %DEC conversion error on VALUEIN

The VALUEIN field is CHAR(15) on the screen. If the user enters non-numeric
data, `%DEC(%TRIM(VALUEIN) : 15 : 2)` will cause a runtime error. For a
production application you would add input validation before the conversion.
For this interview demo, pre-fill with a valid numeric default.

---

*End of DOC 5 — 5250 Display File Setup, Verification & Project Tie-Together*
*Companion to DOC 4. Use DOC 1 as the primary IBM i proof, DOC 4 as an optional enhancement.*
