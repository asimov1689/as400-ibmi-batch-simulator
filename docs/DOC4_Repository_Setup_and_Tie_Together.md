# DOC 4 — Repository Setup, Verification & Project Tie-Together
## How Everything Connects: IBM i Native + Java + 5250 UI + GitHub
### Platform: macOS · IDE: VS Code · Version Control: Git + GitHub

---

## Table of Contents

1. [The Big Picture — How All Five Layers Fit Together](#1-the-big-picture--how-all-five-layers-fit-together)
2. [Create the GitHub Repository](#2-create-the-github-repository)
3. [Initialise the Local Repository in VS Code](#3-initialise-the-local-repository-in-vs-code)
4. [.gitignore — What to Never Commit](#4-gitignore--what-to-never-commit)
5. [Complete Repository File Structure](#5-complete-repository-file-structure)
6. [README.md — The GitHub Public Face](#6-readmemd--the-github-public-face)
7. [Git Workflow in VS Code — Day-to-Day Commands](#7-git-workflow-in-vs-code--day-to-day-commands)
8. [VS Code Source Control Panel — Push, Pull, Commit](#8-vs-code-source-control-panel--push-pull-commit)
9. [Compiling DOC 3 (5250 UI) on PUB400](#9-compiling-doc-3-5250-ui-on-pub400)
10. [Running Everything Together — End-to-End Startup Sequence](#10-running-everything-together--end-to-end-startup-sequence)
11. [System Integration Test — Full Stack Verification](#11-system-integration-test--full-stack-verification)
12. [Running the Full Demo — Under 10 Minutes](#12-running-the-full-demo--under-10-minutes)
13. [What the Interviewer Will See on GitHub](#13-what-the-interviewer-will-see-on-github)
14. [Troubleshooting Common Issues](#14-troubleshooting-common-issues)

---

## 1. The Big Picture — How All Five Layers Fit Together

```
┌─────────────────────────────────────────────────────────────────┐
│  YOUR LOCAL macOS MACHINE                                       │
│                                                                  │
│  LAYER 4 — REST API (Spring Boot, port 8080)       [DOC 2]      │
│  PortfolioController → 8 REST endpoints                          │
│  Swagger UI → /swagger-ui.html                                   │
│  Caffeine cache → @Cacheable / @CacheEvict                       │
│      │                                                           │
│  LAYER 3 — IBM i Integration Services (JT400)      [DOC 2]      │
│  PortfolioService (cached) → Repository → DB2 for i             │
│  DataQueueService → *DTAQ   ProgramCallService → *PGM            │
│  CommandExecutorService → CL commands                            │
│      │                                                           │
└──────┼───────────────────────────────────────────────────────────┘
       │ JT400 / TCP port 8476 (DDM) + 449 (DDR)
       │
       │          Internet → PUB400.com (IBM i 7.5)
       │
┌──────▼───────────────────────────────────────────────────────────┐
│  IBM i (PUB400.COM)   Library: CODELIVER1                        │
│                                                                  │
│  LAYER 5 — 5250 Display File UI                    [DOC 3]      │
│  PORTFDSPL  *FILE DSPF  — green-screen layout                    │
│  PORTFUI    *PGM        — RPGLE workstation program              │
│      │                                                           │
│      └──── calls ────┐                                           │
│                      ▼                                           │
│  LAYER 2 — IBM i Programs (native RPG / COBOL / CL) [DOC 1]    │
│  PORTFSVC   *SRVPGM ←── shared business rules                   │
│  PORTFINQ   *PGM         PORTFCBL  *PGM                         │
│  ORDPROC    *PGM         PORTFTEST *PGM                          │
│  ORDRBATCH  *PGM                                                 │
│                                                                  │
│  LAYER 1 — DB2 for i (shared database)              [DOC 1]     │
│  CODELIVER1.PORTFOLIO         *FILE (PF)                        │
│  CODELIVER1.TRADE_ORDERS      *FILE (PF)                        │
│  CODELIVER1.ACTIVE_PORTFOLIOS  SQL View                         │
│                                                                  │
│  ORDERQ  *DTAQ — trade order queue                  [DOC 2]     │
└─────────────────────────────────────────────────────────────────┘

GitHub Repository: github.com/asimov1689/as400-ibmi-batch-simulator
  ↑ VS Code pushes here — interviewer reads source here
```

### Connection summary

| Component | Where it runs | How it connects to IBM i |
|-----------|--------------|--------------------------|
| Layer 1 (DB2) | PUB400.com | Native IBM i — always on |
| Layer 2 (Programs) | PUB400.com | Native IBM i — always on |
| Layer 3 (JT400 services) | Your Mac | TCP to pub400.com:449 (DDR) |
| Layer 4 (REST API) | Your Mac (port 8080) | Via Layer 3 |
| Layer 5 (5250 UI) | PUB400.com (ACS) | Native IBM i — 5250 terminal |
| VS Code IBM i panel | Your Mac | SSH to pub400.com:2222 |
| Tests | Your Mac | Layer 3 → PUB400 (integration/system) |

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

## 2. Create the GitHub Repository

### 2.1 Create the Repository on GitHub.com

1. Go to https://github.com/new
2. Fill in:
   - **Repository name:** `as400-ibmi-batch-simulator`
   - **Description:** `IBM i Portfolio Management System — Spring Boot + JT400 + native RPG/COBOL/CL on PUB400. Demonstrates IBM i concepts for banking/wealth management.`
   - **Visibility:** Public (so the interviewer can see it without logging in)
   - **Do NOT** tick "Add a README" (you'll add it locally)
   - **Do NOT** tick "Add .gitignore" (you'll add it locally)
3. Click **Create repository**
4. Copy the URL: `https://github.com/asimov1689/as400-ibmi-batch-simulator.git`

---

## 3. Initialise the Local Repository in VS Code

Open VS Code terminal:

```bash
cd ~/projects/as400-ibmi-batch-simulator

git init
git branch -M main

git config --global user.name  "Christian Oliver Jaramillo"
git config --global user.email "256548565+asimov1689@users.noreply.github.com"

git remote add origin git@github.com:asimov1689/as400-ibmi-batch-simulator.git

git remote -v
# Expected:
# origin  git@github.com:asimov1689/as400-ibmi-batch-simulator.git (fetch)
# origin  git@github.com:asimov1689/as400-ibmi-batch-simulator.git (push)
```

---

## 4. .gitignore — What to Never Commit

```gitignore
# Build output
target/

# IDE
.idea/
*.iml
.vscode/
.settings/
.project
.classpath
*.swp
*.swo
*~

# OS
.DS_Store
Thumbs.db

# Maven
*.jar
*.war
*.ear

# Logs
*.log
logs/

# Credentials & secrets — NEVER commit
.env
.env.*
*.pem
*.key
*.p12
*.jks
*.keystore
credentials.*
secrets.*
application-local.yml
application-local.properties

# Spring Boot
spring-boot-devtools.properties

# IBM i
*.sav
*.savf

# Test outputs
surefire-reports/
failsafe-reports/
```

---

## 5. Complete Repository File Structure

```
as400-ibmi-batch-simulator/
├── .gitignore
├── pom.xml
├── README.md
├── docs/
│   ├── DOC1_Layer1_IBMi_Native_Development.md
│   ├── DOC2_Layer3_Layer4_Java_SpringBoot_JT400.md
│   ├── DOC3_Layer5_IBMi_5250_Display_File_UI.md
│   ├── DOC4_Repository_Setup_and_Tie_Together.md      ← this doc
│   ├── assets/
│   │   └── compilation-success-redacted.png
│   └── compiling-rpgle-from-vscode.md
├── src/
│   ├── ibmi/                              ← IBM i native source
│   │   ├── clle/                          ← CL programs (DOC 1)
│   │   │   ├── ITEST01.clle
│   │   │   ├── LOGMSG.clle
│   │   │   └── ORDPROC.clle
│   │   ├── cobol/                         ← COBOL/400 (DOC 1)
│   │   │   └── PORTFCBL.cbl
│   │   ├── dds/                           ← DDS display files (DOC 3)
│   │   │   └── PORTFDSPL.dspf
│   │   ├── include/                       ← RPG copy members (DOC 1)
│   │   │   └── PORTFSVCPR.rpgleinc
│   │   ├── rpgle/                         ← RPGLE programs (DOC 1 + DOC 3)
│   │   │   ├── PORTFSVC.rpgle
│   │   │   ├── PORTFTEST.rpgle
│   │   │   ├── PORTFUI.rpgle
│   │   │   └── UTEST01.rpgle
│   │   ├── sql/                           ← SQL DDL (DOC 1)
│   │   │   └── CRTTABLES.sql
│   │   ├── sqlrpgle/                      ← SQLRPGLE programs (DOC 1)
│   │   │   ├── ORDRBATCH.sqlrpgle
│   │   │   ├── PORTFINQ.sqlrpgle
│   │   │   └── STEST01.sqlrpgle
│   │   └── srvsrc/                        ← Binder source (DOC 1)
│   │       └── PORTFSVC.bnd
│   ├── main/java/com/example/ibmi/        ← Java Spring Boot (DOC 2)
│   │   ├── IbmiApplication.java
│   │   ├── config/
│   │   │   ├── CacheConfig.java
│   │   │   ├── IbmiConnectionConfig.java
│   │   │   └── OpenApiConfig.java
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
│   └── test/
│       ├── java/com/example/ibmi/
│       │   ├── unit/                      ← H2 in-memory, mocked AS400
│       │   ├── integration/               ← live PUB400 DB2 + JT400
│       │   └── system/                    ← full HTTP round-trip to IBM i
│       ├── resources/
│       │   ├── application-test.yml
│       │   ├── schema-h2.sql
│       │   └── data-test.sql
│       └── http/
│           └── ibmi-tests.http
```

---

## 6. README.md — The GitHub Public Face

This is the most important file on GitHub. The interviewer will read this first.

The README should contain:

- **Business context** — the wealth management order processing narrative
- **Architecture diagram** — all 5 layers clearly described
- **IBM i concept table** — every concept with z/OS equivalent
- **Quick start** — can clone and run in 3 commands
- **Security note** — credentials from env vars only
- **API endpoints table** — with IBM i concept per endpoint
- **Documentation links** — DOC 1 through DOC 4

### Suggested README badges

```markdown
[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io)
[![IBM i](https://img.shields.io/badge/IBM%20i-7.5-red)](https://pub400.com)
[![JT400](https://img.shields.io/badge/JT400-20.0-orange)](https://jt400.sourceforge.net)
```

### IBM i Concepts Demonstrated

| IBM i Concept | Component | z/OS Equivalent |
|--------------|-----------|----------------|
| Physical File (PF) | `CODELIVER1.PORTFOLIO` | VSAM KSDS / DB2 z/OS table |
| SQL View = Logical File | `ACTIVE_PORTFOLIOS` | DB2 z/OS view |
| `**FREE` RPGLE | `PORTFINQ` | COBOL free-format |
| `DCL-PI` procedure interface | `PORTFINQ` | COBOL LINKAGE SECTION |
| COBOL/400 embedded SQL | `PORTFCBL` | z/OS COBOL EXEC SQL — identical syntax |
| `MONMSG` error handling | `ORDPROC` | JCL `COND=` |
| ILE `*SRVPGM` | `PORTFSVC` | IEWL link-edit / shared DLL |
| ILE `*MODULE` | `PORTFSVC` before build | Object file before link |
| Binding Directory `*BNDDIR` | `PORTFBNDD` | Dependency manager / library search path |
| Binder source | `srvsrc/PORTFSVC.bnd` | API contract / export control |
| Exported procedure | `VALIDATEPORTFOLIO`, etc. | Public method/function |
| SQL cursor + `COMMIT(*CHG)` | `ORDRBATCH` | COBOL DECLARE cursor + EXEC SQL COMMIT |
| `*DTAQ` Data Queue | `DataQueueService` | IBM MQ / CICS TSQ |
| `ProgramCall` + EBCDIC | `ProgramCallService` | EXEC CICS LINK |
| `AS400JDBCDriver` | `PortfolioRepository` | JDBC to DB2 z/OS |
| DDS display file | `PORTFDSPL` | BMS mapset (CICS) |
| Workstation file + EXFMT | `PORTFUI` | CICS SEND MAP + RECEIVE MAP |
| Function key handling (CA03) | `PORTFUI` | CICS AID byte check |

---

## 7. Git Workflow in VS Code — Day-to-Day Commands

### 7.1 Commit Message Convention

```text
<type>: <short description>

<body — optional, explain why not what>
```

Types:
- `feat:` — new feature or capability
- `fix:` — bug fix
- `test:` — adding or updating tests
- `docs:` — documentation changes
- `ibmi:` — IBM i native source changes (RPG/CL/COBOL on PUB400)
- `config:` — configuration changes

Examples:

```bash
git commit -m "ibmi: Add PORTFCBL COBOL/400 program"
git commit -m "feat: Add DataQueueService enqueue/dequeue for DTAQ messaging"
git commit -m "test: Add BatchSettlementSystemTest full stack coverage"
git commit -m "ibmi: Add PORTFDSPL display file and PORTFUI workstation program"
```

### 7.2 Branch and PR Workflow

```bash
# Create a feature branch
git checkout -b feature-name

# Work, commit, push
git add <files>
git commit -m "feat: description"
git push -u origin feature-name

# Create PR and merge
gh pr create --title "Title" --body "Description"
gh pr merge --merge --delete-branch
```

### 7.3 Code Formatting

Run Spotless before committing Java code:

```bash
mvn spotless:apply    # auto-format all Java source
mvn spotless:check    # verify formatting (fails build if dirty)
```

---

## 8. VS Code Source Control Panel — Push, Pull, Commit

### 8.1 Using the GUI (alternative to terminal)

1. Click the **Source Control** icon in the left sidebar (`Ctrl + Shift + G`)
2. Changed files appear under **Changes**
3. Click `+` next to a file to stage it
4. Type a commit message in the box at the top
5. Click the ✓ **Commit** button
6. Click **Sync Changes** to push to GitHub

### 8.2 Connect VS Code to GitHub (SSH)

```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "256548565+asimov1689@users.noreply.github.com" -f ~/.ssh/github_id_ed25519

# Add to ssh-agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/github_id_ed25519

# Copy public key and add to GitHub → Settings → SSH Keys
cat ~/.ssh/github_id_ed25519.pub

# Set remote to SSH
git remote set-url origin git@github.com:asimov1689/as400-ibmi-batch-simulator.git

# Test
ssh -T git@github.com
# Expected: Hi asimov1689! You've successfully authenticated
```

---

## 9. Compiling DOC 3 (5250 UI) on PUB400

DOC 3 adds two IBM i objects that must be compiled on PUB400.

### Prerequisites

```bash
ssh -p 2222 CODELIVER@pub400.com
system "CHGJOB CCSID(37)"

# Service program must exist and export 3 procedures
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
# Expected: VALIDATEPORTFOLIO, VALIDATECURRENCY, FORMATPORTFOLIOMSG

# Binding directory must contain PORTFSVC
system "DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)"
# Expected: PORTFSVC *SRVPGM
```

### Promoting source to PUB400

Upload the source files via VS Code IBM i panel:

1. Open `CODELIVER1-SRC` in the Object Browser
2. Right-click `QDDSSRC` → **New Member** → name `PORTFDSPL`, type `DSPF`
3. Paste the contents of `src/ibmi/dds/PORTFDSPL.dspf` and save
4. Right-click `QRPGLESRC` → **New Member** → name `PORTFUI`, type `RPGLE`
5. Paste the contents of `src/ibmi/rpgle/PORTFUI.rpgle` and save

### Compile the display file

```bash
system "CRTDSPF FILE(CODELIVER1/PORTFDSPL) SRCFILE(CODELIVER1/QDDSSRC) SRCMBR(PORTFDSPL)"
```

### Compile the workstation program

```bash
system "CRTBNDRPG PGM(CODELIVER1/PORTFUI) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFUI) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
```

### Verify

```bash
system "WRKOBJ OBJ(CODELIVER1/PORTFDSPL) OBJTYPE(*FILE)"
system "WRKOBJ OBJ(CODELIVER1/PORTFUI) OBJTYPE(*PGM)"
```

---

## 10. Running Everything Together — End-to-End Startup Sequence

### Step 1 — Set environment variables (if not in ~/.zshrc)

```bash
export IBMI_HOST=pub400.com
export IBMI_USER=CODELIVER
export IBMI_PASSWORD=yourpassword
export IBMI_LIBRARY=CODELIVER1
```

### Step 2 — Verify PUB400 is reachable

```bash
ssh -p 2222 CODELIVER@pub400.com
system "CHGJOB CCSID(37)"
system "WRKOBJ OBJ(CODELIVER1/*ALL) OBJTYPE(*ALL)"
# Expected: 15+ objects including PORTFOLIO, PORTFINQ, PORTFSVC, PORTFDSPL, PORTFUI
exit
```

### Step 3 — Verify IBM i Layer 1 (DB2) has data

```bash
ssh -p 2222 CODELIVER@pub400.com
system "STRSQL"
# SELECT * FROM CODELIVER1.PORTFOLIO    → 3 rows
# SELECT * FROM CODELIVER1.TRADE_ORDERS → 3 rows
# If STATUS is PROC, reset:
# UPDATE CODELIVER1.TRADE_ORDERS SET STATUS='PEND', PROCESS_DT=NULL
exit
```

### Step 4 — Build the Java project

```bash
cd ~/projects/as400-ibmi-batch-simulator
mvn clean install -DskipTests
# Expected: BUILD SUCCESS
```

### Step 5 — Run unit tests (no IBM i needed)

```bash
mvn test
# Expected: Tests run: 16, Failures: 0, Errors: 0
```

### Step 6 — Run integration and system tests (requires PUB400)

```bash
mvn test -Pintegration
# Expected: Tests run: 28, Failures: 0, Errors: 0
```

### Step 7 — Start the Spring Boot application

```bash
mvn spring-boot:run
# Expected: Tomcat started on port(s): 8080
```

Leave this running. Open a new terminal tab.

### Step 8 — Quick smoke test

```bash
curl -s http://localhost:8080/api/v1/system/ping | python3 -m json.tool
curl -s http://localhost:8080/api/v1/portfolios | python3 -m json.tool
```

### Step 9 — Open Swagger UI

```bash
open http://localhost:8080/swagger-ui.html
```

### Step 10 — Test the 5250 screen (ACS)

Open IBM ACS → 5250 Emulator → connect to `pub400.com` → sign on:

```cl
CALL PGM(CODELIVER1/PORTFUI)
```

- Press Enter → `PASS: PF001 | Richard Papen | USD 150000.00`
- Change Status to `I` → `FAIL: Portfolio status is inactive.`
- Press F3 to exit

### Step 11 — Use the .http file in VS Code

Open `src/test/http/ibmi-tests.http` in VS Code.
Click **Send Request** above each `###` block.

---

## 11. System Integration Test — Full Stack Verification

### 11.1 Layer 1 — DB2 for i (PUB400)

```bash
ssh -p 2222 CODELIVER@pub400.com
system "CHGJOB CCSID(37)"
system "STRSQL"
# SELECT COUNT(*) FROM CODELIVER1.PORTFOLIO          → 3
# SELECT COUNT(*) FROM CODELIVER1.ACTIVE_PORTFOLIOS  → 2 (PF003 is inactive)
# SELECT COUNT(*) FROM CODELIVER1.TRADE_ORDERS       → 3
```

### 11.2 Layer 2 — IBM i Programs (PUB400)

```bash
# Service program exports
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
# Expected: VALIDATEPORTFOLIO, VALIDATECURRENCY, FORMATPORTFOLIOMSG

# Batch test
system "CLRPFM FILE(CODELIVER1/PORTFOUT)"
system "CALL CODELIVER1/PORTFTEST"
system "DSPPFM FILE(CODELIVER1/PORTFOUT)"
# Expected: TEST1 PASS, TEST2 PASS, TEST3 MSG

# CL driver
system "CALL CODELIVER1/ORDPROC"
# Expected: RPGLE result: Richard Papen USD, COBOL result: Richard Papen USD
```

### 11.3 Layer 5 — 5250 Display File UI (ACS)

From a 5250 command line (not PASE):

```cl
CALL PGM(CODELIVER1/PORTFUI)
```

| Test | Input | Expected result |
|------|-------|----------------|
| PASS case | Status=A, Currency=USD, Value=150000.00 | `PASS: PF001 \| Richard Papen \| USD 150000.00` |
| FAIL — inactive | Status=I | `FAIL: Portfolio status is inactive.` |
| FAIL — zero value | Value=0 | `FAIL: Portfolio value is zero or negative.` |
| FAIL — bad currency | Currency=XYZ | `FAIL: Currency is not valid.` |
| Exit | Press F3 | Screen closes cleanly |

### 11.4 Layer 3 — JT400 Integration (via REST endpoints)

```bash
# Ping — tests CommandExecutorService
curl -s http://localhost:8080/api/v1/system/ping

# Job info — tests ProgramCallService (QUSRJOBI)
curl -s http://localhost:8080/api/v1/job-info

# Enqueue + dequeue — tests DataQueueService
curl -s -X POST http://localhost:8080/api/v1/orders/enqueue \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-VERIFY-001","portfId":"PF001","isin":"TSH000000001","quantity":1,"price":100}'
curl -s "http://localhost:8080/api/v1/orders/dequeue?waitSeconds=5"
```

### 11.5 Layer 4 — REST API (full CRUD)

```bash
curl -s http://localhost:8080/api/v1/portfolios | python3 -m json.tool
curl -s http://localhost:8080/api/v1/portfolios/PF001 | python3 -m json.tool
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/portfolios/XXXXX
# Expected: 404
curl -s -X PUT "http://localhost:8080/api/v1/portfolios/PF001/value?newValue=160000.00"
```

### 11.6 Run All Tests

```bash
mvn test                    # 16 unit tests (no IBM i)
mvn test -Pintegration      # 28 tests (requires live PUB400)
mvn spotless:check          # code formatting
```

### 11.7 All Objects on PUB400

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
PORTFDSPL     *FILE DSPF  Display file (DOC 3)
PORTFUI       *PGM        Workstation program (DOC 3)
```

---

## 12. Running the Full Demo — Under 10 Minutes

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

## 13. What the Interviewer Will See on GitHub

When the interviewer opens the repo, they see:

```text
README.md                              ← architecture, endpoints, how to run
docs/
  DOC1_...md                           ← IBM i native development guide
  DOC2_...md                           ← Java Spring Boot + JT400 guide
  DOC3_...md                           ← 5250 display file UI guide
  DOC4_...md                           ← this tie-together doc
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

### The `ibmiConcept` field in every API response

When the interviewer runs `curl http://localhost:8080/api/v1/portfolios/PF001`:

```json
{
  "data": {
    "portfId": "PF001",
    "owner": "Richard Papen",
    "currency": "USD",
    "totalValue": 150000.00,
    "status": "A"
  },
  "ibmiConcept": "DB2 for i JDBC keyed read — CHAIN opcode equivalent. JdbcTemplate.query() -> AS400JDBCDriver -> DB2 for i SELECT WHERE PORTF_ID=?"
}
```

Every response is self-documenting.

---

## 14. Troubleshooting Common Issues

### `AS400SecurityException: Password is incorrect`

```bash
echo $IBMI_PASSWORD
# Verify env var is set and matches your PUB400 password
```

### `ClassNotFoundException: com.ibm.as400.access.AS400JDBCDriver`

```bash
mvn clean install -DskipTests
# Rebuilds and downloads JT400 dependency
```

### `Connection refused` or `No route to host`

```bash
ping pub400.com
telnet pub400.com 449   # DRDA port for DB2 for i JDBC
```

### DB2 JDBC: `Table not found`

```bash
ssh -p 2222 CODELIVER@pub400.com
system "WRKOBJ OBJ(CODELIVER1/*ALL) OBJTYPE(*FILE)"
# If PORTFOLIO not listed, re-run CRTTABLES.sql (DOC 1 §5.3)
```

### CCSID errors — garbled characters

```bash
# Always run at session start:
system "CHGJOB CCSID(37)"
```

### `*DTAQ ORDERQ not found`

```bash
system "CRTDTAQ DTAQ(CODELIVER1/ORDERQ) MAXLEN(200) TEXT('Order Queue')"
```

### Unit tests fail with `H2 table not found`

```bash
mvn test -Dspring.profiles.active=test
# Ensure src/test/resources/schema-h2.sql and data-test.sql exist
```

### PORTFDSPL not found during PORTFUI compile

The display file must be compiled before the program:

```bash
system "CRTDSPF FILE(CODELIVER1/PORTFDSPL) SRCFILE(CODELIVER1/QDDSSRC) SRCMBR(PORTFDSPL)"
```

### PORTFUI binding errors

```bash
system "DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)"
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
# Exported names must match DCL-PR EXTPROC() names exactly
```

### 5250 screen does not display from VS Code terminal

That is expected. Display files require a real 5250 terminal session.
Use IBM ACS (5250 Emulator) or any TN5250 client.

### %DEC conversion error on VALUEIN

The VALUEIN field is CHAR(15). Non-numeric input causes a runtime error.
For demo purposes, always use the pre-filled numeric defaults.

---

## Summary: The Four-Document Build Order

| Order | Document | What You Build |
|-------|----------|---------------|
| **1st** | **DOC 1** | IBM i native — DB2 tables, RPG/COBOL/CL programs on PUB400 (Layers 1-2) |
| **2nd** | **DOC 2** | Java Spring Boot + JT400 — REST API connecting your Mac to PUB400 (Layers 3-4) |
| **3rd** | **DOC 3** | 5250 display file UI — native green-screen front end over PORTFSVC (Layer 5) |
| **4th** | **DOC 4** | Repository setup, verification, and project tie-together (this doc) |

The result is a single public GitHub repository that contains:
- Native IBM i source code (readable without a 5250 terminal)
- A live Spring Boot REST API with OpenAPI/Swagger UI
- A native 5250 green-screen validation UI
- Caffeine caching on portfolio and order reads
- Business context documentation explaining the wealth management domain
- Three tiers of tests (unit → integration → system) — 28 tests total
- An `ibmiConcept` field in every API response connecting the Java code to IBM i concepts
- Three front ends sharing one service program — ILE separation of concerns

---

*End of DOC 4 — Repository Setup, Verification & Project Tie-Together*
*All four documents together constitute the complete technical guide for the IBM i Portfolio Management System*
