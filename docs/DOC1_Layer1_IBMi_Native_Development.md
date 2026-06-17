# DOC 1 — Layer 1 & Layer 2: IBM i Native Development
## DB2 for i Database + RPG / COBOL / CL Programs on PUB400
### Complete Step-by-Step Guide with Full Source Code
### Platform: macOS · IDE: VS Code · Terminal: VS Code Integrated Terminal

---

## Table of Contents

1. [What You Are Building](#1-what-you-are-building)
2. [macOS Prerequisites — What to Install](#2-macos-prerequisites--what-to-install)
3. [VS Code Setup for IBM i](#3-vs-code-setup-for-ibm-i)
4. [Connect to PUB400 from VS Code](#4-connect-to-pub400-from-vs-code)
5. [Layer 1 — DB2 for i: Create the Database](#5-layer-1--db2-for-i-create-the-database)
6. [Layer 2A — PORTFINQ: RPGLE Portfolio Inquiry](#6-layer-2a--portfinq-rpgle-portfolio-inquiry)
7. [Layer 2B — PORTFCBL: COBOL/400 Portfolio Inquiry](#7-layer-2b--portfcbl-cobol400-portfolio-inquiry)
8. [Layer 2C — ORDPROC: CL Driver (calls both RPGLE + COBOL)](#8-layer-2c--ordproc-cl-driver-calls-both-rpgle--cobol)
9. [Layer 2D — PORTFSVC: ILE Service Program](#9-layer-2d--portfsvc-ile-service-program)
10. [Layer 2E — PORTFTEST: Service Program Caller](#10-layer-2e--portftest-service-program-caller)
11. [Layer 2F — ORDRBATCH: Batch Order Processor](#11-layer-2f--ordrbatch-batch-order-processor)
12. [Unit, Integration, and System Tests (AAA Format)](#12-unit-integration-and-system-tests-aaa-format)
13. [Verification Checklist](#13-verification-checklist)
14. [Storing Source in the Git Repository](#14-storing-source-in-the-git-repository)

---

## 1. What You Are Building

Layer 1 and Layer 2 are the IBM i native core of the project. Everything runs directly on PUB400 — a free public IBM i 7.5 system. You write source in VS Code, upload it to PUB400, compile it with native IBM i compilers, and run the resulting objects on the IBM i OS.

```
LAYER 2 — IBM i Programs (native RPG / COBOL / CL)
  PORTFINQ   *PGM   — RPGLE portfolio inquiry
  PORTFCBL   *PGM   — COBOL/400 portfolio inquiry (same logic)
  ORDPROC    *PGM   — CL driver calling both above programs
  PORTFSVC   *SRVPGM— ILE service program (3 exported procedures)
  PORTFTEST  *PGM   — Caller that exercises PORTFSVC
  ORDRBATCH  *PGM   — Batch processor with set-based SQL update + COMMIT

LAYER 1 — DB2 for i (shared database)
  CODELIVER1.PORTFOLIO        — Physical file (master portfolio data)
  CODELIVER1.TRADE_ORDERS     — Physical file (trade orders)
  CODELIVER1.ACTIVE_PORTFOLIOS— SQL View (active portfolios only)
```

**Working library/schema:** `CODELIVER1` (substitute your own IBM i library if different)

---

## 2. macOS Prerequisites — What to Install

### 2.1 Homebrew (macOS package manager)

Open **Terminal** (or VS Code integrated terminal) and run:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Verify:

```bash
brew --version
# Expected: Homebrew 4.x.x
```

### 2.2 SSH (built-in on macOS — verify it works)

```bash
ssh -V
# Expected: OpenSSH_9.x
```

### 2.3 VS Code

Download from https://code.visualstudio.com and install the `.dmg`. Then add the `code` shell command:

1. Open VS Code
2. Press `Cmd + Shift + P`
3. Type `Shell Command: Install 'code' command in PATH` → press Enter

Verify:

```bash
code --version
# Expected: 1.9x.x
```

### 2.4 VS Code Extensions — Install These

Open VS Code, press `Cmd + Shift + X` (Extensions), search and install each:

| Extension | Publisher | Purpose |
|-----------|-----------|---------|
| **Code for IBM i** | Halcyon-Tech | Edit, compile, run RPG/CL/COBOL on IBM i from VS Code |
| **RPG & CL** | Halcyon-Tech | Syntax highlighting for RPGLE and CL source |
| **IBM i Languages** | Halcyon-Tech | COBOL/400 syntax support |
| **GitLens** | GitKraken | Enhanced Git integration |
| **YAML** | Red Hat | Syntax support for application.yml |
| **REST Client** | Huachao Mao | Run .http test files directly in VS Code |

Or install all at once from the terminal:

```bash
code --install-extension halcyontechltd.code-for-ibmi
code --install-extension halcyontechltd.vscode-rpgle
code --install-extension halcyontechltd.vscode-ibmi-languages
code --install-extension eamodio.gitlens
code --install-extension redhat.vscode-yaml
code --install-extension humao.rest-client
```

### 2.5 IBM i Access Client Solutions (ACS) — 5250 Green Screen Terminal

IBM i Access Client Solutions is the IBM-supported desktop tool used here for the
5250 green screen. You can do most coding from VS Code, but ACS is still useful
for signing on to PUB400, checking objects with `WRKOBJ`, browsing libraries with
`WRKLIB`, and using classic PDM/SEU screens when you want to verify native IBM i
behavior.

Download ACS from IBM:

https://www.ibm.com/support/pages/ibm-i-access-client-solutions

ACS is Java-based. Install Java 21 on macOS:

```bash
brew install --cask temurin@21
java -version
# Expected: openjdk version "21.x.x"
```

After downloading ACS, unzip the archive and keep the application files in a
stable folder, for example:

```bash
mkdir -p ~/tools/ibm-acs
# Copy or unzip the downloaded ACS package into ~/tools/ibm-acs
```

Launch ACS from Finder or from the terminal. The exact `.jar` name depends on the
download package, but it is commonly `acsbundle.jar`:

```bash
java -jar ~/tools/ibm-acs/acsbundle.jar
```

If your downloaded package has a different `.jar` name, locate it:

```bash
find ~/tools/ibm-acs -name "*.jar" -maxdepth 3
```

Then launch that jar with `java -jar`.

> **Tip:** Create a shell alias for convenience. In `~/.zshrc`:
> ```bash
> alias acs='java -jar ~/tools/ibm-acs/acsbundle.jar &'
> ```

#### Configure a PUB400 5250 Session

In ACS:

1. Open **System Configurations**.
2. Click **New**.
3. Set **System name** to `PUB400`.
4. Set **Host name or IP address** to `pub400.com`.
5. Click **OK**.
6. From the main ACS window, open **5250 Emulator**.
7. Select the `PUB400` system.
8. Start the session and sign on with your PUB400 user profile.

For this guide, the examples use:

| Field | Value |
|-------|-------|
| Host | `pub400.com` |
| User | `CODELIVER1` (replace with your PUB400 username) |
| Library | `CODELIVER1` |

After sign-on, you should see a green screen command line:

```text
===> 
```

Useful first checks:

```text
DSPLIBL
WRKLIB CODELIVER1
WRKOBJ CODELIVER1/*ALL
```

#### Optional: IBM i Access Application Package for ODBC

The ACS desktop tool gives you the 5250 terminal. IBM also provides a separate
macOS **IBM i Access Application Package** that installs command-line utilities
and the IBM i ODBC driver under `/Library/IBMiAccess`.

Install this package only if you need local ODBC access from tools such as Excel,
Python, or SQL clients. It is not required for compiling RPG, COBOL, CL, or SQL
objects in this Layer 1/Layer 2 guide.

If you install the Application Package, also install `unixODBC` and register the
driver:

```bash
brew install unixodbc
sudo installer -pkg /path/to/ibm-iaccess-1.1.0.29.pkg -target /
sudo /Library/IBMiAccess/register_driver
```

Verify the command-line tools and ODBC driver:

```bash
cwbping '-?'
odbcinst -q -d
# Expected: [IBM i Access ODBC Driver]
```

### 2.6 Git

```bash
brew install git
git --version
# Expected: git version 2.4x.x
```

---

## 3. VS Code Setup for IBM i

### 3.1 Open the Code for IBM i Connection Panel

In VS Code left sidebar, click the **IBM i** icon (plug/server icon added by the extension). Click **"Add Connection"**.

### 3.2 Create a Connection Profile

Fill in:

| Field | Value |
|-------|-------|
| Connection Name | `PUB400` |
| Host | `pub400.com` |
| Username | `CODELIVER` (your PUB400 sign-on profile; substitute yours if different) |
| Auth Method | Password |
| Password | (your PUB400 password — stored in VS Code keychain) |
| Default Library | `CODELIVER1` |

Click **Connect**. A green status bar at the bottom of VS Code confirms the connection.

### 3.3 Configure Default Settings (settings.json)

Press `Cmd + Shift + P` → `Open User Settings (JSON)` and add:

```json
{
  "code-for-ibmi.connectionSettings": {
    "defaultLibrary": "CODELIVER1",
    "sourceFileList": ["QRPGLESRC", "QCLSRC", "QCBLLESRC", "QSQLSRC", "QDDSSRC", "QSRVSRC"],
    "libraryList": ["CODELIVER1", "QGPL", "QTEMP"]
  },
  "ibmi.logLevel": "info"
}
```

### 3.4 Confirm the Active IBM i Profile and Object Browser Filters

Before creating, compiling, or testing any source, confirm that VS Code is using
the correct Code for IBM i profile. If the wrong profile is active, commands may
run against the wrong IBM i system, user, library list, or current library.

In the VS Code IBM i panel:

1. Open the Code for IBM i connection/profile selector.
2. Confirm the active profile is `PUB400`.
3. Confirm it is connected to `pub400.com`.
4. Confirm the current library is `CODELIVER1`.
5. Confirm the object browser shows filters for `CODELIVER1-SRC` and
   `CODELIVER1-OBJ`.

Use this mental model:

| Browser filter | Comparable local concept | Purpose |
|----------------|--------------------------|---------|
| `CODELIVER1-SRC` | Java/Kotlin/RPG source files | Browse source physical files and source members |
| `CODELIVER1-OBJ` | `.class`, `.jar`, executables | Browse compiled IBM i artifacts |

Create these two IBM i VS Code Object Browser filters for library `CODELIVER1`.

**Filter 1 — Source Members**

| Field | Value |
|-------|-------|
| Name | `CODELIVER1-SRC` |
| Libraries | `CODELIVER1` |
| Objects | `*` |
| Object Types | `*SRCPF` |
| Members | `*` |

Purpose: show source physical files and members such as `QRPGLESRC`, `QSQLSRC`,
`QCBLLESRC`, `QCLSRC`, `QDDSSRC`, `QSRVSRC`, and source members like
`PORTFINQ`, `PORTFSVC`, `PORTFSVCPR`, `PORTFTEST`, `CRTTABLES`, `ORDPROC`, and
`UTEST01`.

**Filter 2 — Compiled Objects**

| Field | Value |
|-------|-------|
| Name | `CODELIVER1-OBJ` |
| Libraries | `CODELIVER1` |
| Objects | `*` |
| Object Types | `*PGM,*MODULE,*SRVPGM,*BNDDIR` |

Purpose: show compiled IBM i objects such as programs, modules, service
programs, and binding directories. Examples: `PORTFINQ *PGM`,
`PORTFSVC *MODULE`, `PORTFSVC *SRVPGM`, and `PORTFBNDD *BNDDIR`.

Do this quick check before every compile or test run:

```bash
system "DSPJOB OPTION(*DFNA)"
system "DSPLIBL"
system "WRKOBJ OBJ(CODELIVER1/*ALL) OBJTYPE(*ALL)"
```

Expected:

- The SSH terminal is signed on to PUB400, not local macOS.
- The VS Code active profile is `PUB400`.
- The working library/schema for this guide is `CODELIVER1`.
- `CODELIVER1-SRC` is used when editing source members.
- `CODELIVER1-OBJ` is used when checking compiled outputs.

### 3.5 Verify the Connection

In the IBM i panel in VS Code:
- Expand `CODELIVER1` → you should see your library
- Right-click the library → **Run SQL** → type `SELECT * FROM CODELIVER1.PORTFOLIO` (after tables are created in step 5)

---

## 4. Connect to PUB400 from VS Code

### 4.1 SSH Connection (for bash terminal access)

Add to `~/.ssh/config`:

```
Host pub400
  HostName pub400.com
  User CODELIVER
  Port 2222
  ServerAliveInterval 60
```

Then connect:

```bash
ssh pub400
# Enter your PUB400 password when prompted
```

You are now in the IBM i PASE shell (a Unix-like environment on IBM i). The `system` command runs CL commands from bash.

For PASE bash, keep each `system "..."` command on one physical line. The `+`
continuation marker is for the 5250 command line / SEU style, not for bash.

If any of the bash examples using `system "..."` do not work, first make sure
you are actually inside the PUB400 PASE shell. From your local macOS terminal or
VS Code terminal, reconnect explicitly:

```bash
ssh -p 2222 CODELIVER@pub400.com
```

After signing in, rerun the `system "..."` command from that SSH session.

### 4.2 Set Up the Spooled File Browser in VS Code

On PUB400, each `system "..."` call from PASE bash can run in a separate IBM i
job. That means `DSPJOBLOG` and `CPYSPLF` may not show output from an earlier
`system "CALL ..."` command if that output belongs to a different job. The VS
Code Spooled File Browser avoids this by querying output queues directly.

Install and configure it once:

1. Open VS Code Extensions (`Ctrl+Shift+X` or `Cmd+Shift+X` on macOS).
2. Search for **Code for IBM i Spooled Files** by Matt Tyler and install it.
3. In the IBM i sidebar, scroll to **SPOOLED FILE BROWSER**.
4. Click the `+` icon.
5. When prompted for `Library/USERNAME`, enter your IBM i username, for example
   `CODELIVER`, and press Enter.

When submitting batch jobs, retain the job log as a spool file:

```bash
system "SBMJOB CMD(CALL PGM(CODELIVER1/PORTFTEST)) JOB(PORTFTEST) LOG(4 0 *SECLVL) LOGOUTPUT(*JOBEND)"
```

After the job completes, click the refresh icon in **SPOOLED FILE BROWSER**. The
latest `QPJOBLOG.splf` entry is the job log for the submitted run. Open it
inline in VS Code to review program messages and failures.

Use direct calls for quick interactive tests:

```bash
system "CALL CODELIVER1/PORTFTEST"
```

Use `SBMJOB ... LOG(4 0 *SECLVL) LOGOUTPUT(*JOBEND)` when you need a retained
job log that survives PASE job boundaries.

### 4.3 Set CCSID at Every Session Start

**Always run this first** in every SSH/PASE session to prevent EBCDIC encoding problems:

```bash
export QIBM_MULTI_THREADED=Y
system "CHGJOB CCSID(37)"
```

### 4.4 Open the 5250 Green Screen via ACS (for PDM/SEU verification)

1. Launch ACS
2. New connection → `pub400.com`
3. Start 5250 Session
4. Sign on: User `CODELIVER` / your password

The green screen `===>` command line is your native IBM i terminal.

---

## 5. Layer 1 — DB2 for i: Create the Database

### 5.1 Create Source Physical Files

In VS Code integrated terminal (SSH session to PUB400):

```bash
# Fix CCSID first
system "CHGJOB CCSID(37)"

# Create source physical files (containers for source members)
system "CRTSRCPF FILE(CODELIVER1/QRPGLESRC) RCDLEN(112) TEXT('RPG Source')"
system "CRTSRCPF FILE(CODELIVER1/QCLSRC)    RCDLEN(112) TEXT('CL Source')"
system "CRTSRCPF FILE(CODELIVER1/QCBLLESRC) RCDLEN(112) TEXT('COBOL Source')"
system "CRTSRCPF FILE(CODELIVER1/QSQLSRC)   RCDLEN(112) TEXT('SQL Source')"
system "CRTSRCPF FILE(CODELIVER1/QDDSSRC)   RCDLEN(112) TEXT('DDS Source')"
system "CRTSRCPF FILE(CODELIVER1/QSRVSRC)   RCDLEN(112) TEXT('Binder Source')"

# Verify
system "DSPLIB LIB(CODELIVER1)"
```

Expected output: 6 `*FILE` objects listed.

If SQL source is being truncated around column 80, `QSQLSRC` was probably
created with the default source-file length. IBM i source physical files reserve
12 bytes for sequence/date fields, so `RCDLEN(92)` leaves only 80 usable source
columns. Recreate `QSQLSRC` with `RCDLEN(112)` before pasting `CRTTABLES.SQL`.

Only run this reset if `QSQLSRC` contains no source members you need to keep:

```bash
system "DLTF FILE(CODELIVER1/QSQLSRC)"
system "CRTSRCPF FILE(CODELIVER1/QSQLSRC) RCDLEN(112) TEXT('SQL Source')"
```

After recreating `QSQLSRC`, reopen `CODELIVER1-SRC` in the VS Code IBM i object
browser and paste the full `CRTTABLES.SQL` source again.

### 5.2 Create the SQL Source Member: CRTTABLES

Create or update the IBM i source member in VS Code:

```text
CODELIVER1/QSQLSRC/CRTTABLES.SQL
```

In the Code for IBM i object browser:

1. Expand `CODELIVER1-SRC`.
2. Open `qsqlsrc` (`SQL Source Members`).
3. Open `crttables.sql`, or create a new member named `CRTTABLES` with type `SQL`.
4. Paste the SQL below and save the member.

```sql
-- ============================================================
-- CRTTABLES.sql
-- Creates PORTFOLIO and TRADE_ORDERS physical files
-- IBM i Concept: Physical File = DB2 for i base table
-- z/OS Equivalent: VSAM KSDS or DB2 z/OS base table
-- ============================================================

-- Rerunnable cleanup.
-- Drop dependent objects first, then parent objects.
DROP VIEW IF EXISTS CODELIVER1.ACTIVE_PORTFOLIOS;
DROP TABLE IF EXISTS CODELIVER1.TRADE_ORDERS;
DROP TABLE IF EXISTS CODELIVER1.PORTFOLIO;

-- PORTFOLIO: Master portfolio table
CREATE TABLE CODELIVER1.PORTFOLIO (
    PORTF_ID    CHAR(10)      NOT NULL,
    OWNER       CHAR(40)      NOT NULL,
    CURRENCY    CHAR(3)       NOT NULL DEFAULT 'USD',
    TOTAL_VALUE DECIMAL(15,2) NOT NULL DEFAULT 0,
    STATUS      CHAR(1)       NOT NULL DEFAULT 'A',
    LAST_UPD    DATE          NOT NULL DEFAULT CURRENT_DATE
);

ALTER TABLE CODELIVER1.PORTFOLIO
    ADD PRIMARY KEY (PORTF_ID);

-- TRADE_ORDERS: Order staging table
CREATE TABLE CODELIVER1.TRADE_ORDERS (
    ORDER_ID    CHAR(20)      NOT NULL,
    PORTF_ID    CHAR(10)      NOT NULL,
    ISIN        CHAR(12)      NOT NULL,
    QUANTITY    DECIMAL(15,4) NOT NULL DEFAULT 0,
    PRICE       DECIMAL(15,4) NOT NULL DEFAULT 0,
    ORDER_DT    DATE          NOT NULL DEFAULT CURRENT_DATE,
    PROCESS_DT  DATE,
    STATUS      CHAR(4)       NOT NULL DEFAULT 'PEND',
    PRIMARY KEY (ORDER_ID),
    FOREIGN KEY (PORTF_ID) REFERENCES CODELIVER1.PORTFOLIO(PORTF_ID)
);

-- ACTIVE_PORTFOLIOS: SQL View (Logical File equivalent)
CREATE OR REPLACE VIEW CODELIVER1.ACTIVE_PORTFOLIOS AS
    SELECT * FROM CODELIVER1.PORTFOLIO
    WHERE STATUS = 'A';

-- Seed data
-- Fictional test data only. Names are literary character references,
-- and ISIN-like values are dummy identifiers, not real securities.
INSERT INTO CODELIVER1.PORTFOLIO
    (PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD)
VALUES
    ('PF001', 'Richard Papen', 'USD', 150000.00, 'A', CURRENT_DATE);

INSERT INTO CODELIVER1.PORTFOLIO
    (PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD)
VALUES
    ('PF002', 'Henry Winter', 'CHF', 280000.00, 'A', CURRENT_DATE);

INSERT INTO CODELIVER1.PORTFOLIO
    (PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD)
VALUES
    ('PF003', 'Camilla Macaulay', 'EUR', 95000.00, 'I', CURRENT_DATE);

INSERT INTO CODELIVER1.TRADE_ORDERS
    (ORDER_ID, PORTF_ID, ISIN, QUANTITY, PRICE, ORDER_DT, PROCESS_DT, STATUS)
VALUES
    ('ORD-2026-001', 'PF001', 'TSH000000001', 100, 182.50,
     CURRENT_DATE, NULL, 'PEND');

INSERT INTO CODELIVER1.TRADE_ORDERS
    (ORDER_ID, PORTF_ID, ISIN, QUANTITY, PRICE, ORDER_DT, PROCESS_DT, STATUS)
VALUES
    ('ORD-2026-002', 'PF001', 'TSH000000002', 50, 312.00,
     CURRENT_DATE, NULL, 'PEND');

INSERT INTO CODELIVER1.TRADE_ORDERS
    (ORDER_ID, PORTF_ID, ISIN, QUANTITY, PRICE, ORDER_DT, PROCESS_DT, STATUS)
VALUES
    ('ORD-2026-003', 'PF002', 'TSH000000003', 200, 45.75,
     CURRENT_DATE, NULL, 'PEND');
```

### 5.3 Run the SQL Source Member

The SQL already lives in the IBM i source member:

```text
CODELIVER1/QSQLSRC/CRTTABLES.SQL
```

Run from SSH terminal:

```bash
ssh -p 2222 CODELIVER@pub400.com
system "CHGJOB CCSID(37)"
system "RUNSQLSTM SRCFILE(CODELIVER1/QSQLSRC) SRCMBR(CRTTABLES) COMMIT(*NONE) NAMING(*SQL) DECMPT(*PERIOD)"
```

Important:
- Run `system "..."` only after SSH signs you into PUB400. It will not work from a local macOS shell.
- `CODELIVER` is the PUB400 sign-on user. `CODELIVER1` is the IBM i library/schema used by the source files and tables.
- Use the exact `DROP ... IF EXISTS` statements from section 5.2 for rerunnable cleanup. Do not use broken drop lines such as `--OP TABLE ...`; that is only a damaged comment, not a valid `DROP TABLE`.
- Use one `INSERT` statement per row in IBM i source members. Avoid multi-row `VALUES (...), (...), (...)` here; it can fail under `RUNSQLSTM` and is harder to diagnose when source lines wrap.

If your IBM i level does not accept `DROP ... IF EXISTS`, remove those three
drop lines from `CRTTABLES.SQL` and reset manually before reruns:

```bash
system "RUNSQL SQL('DROP VIEW CODELIVER1.ACTIVE_PORTFOLIOS') COMMIT(*NONE)"
system "RUNSQL SQL('DROP TABLE CODELIVER1.TRADE_ORDERS') COMMIT(*NONE)"
system "RUNSQL SQL('DROP TABLE CODELIVER1.PORTFOLIO') COMMIT(*NONE)"
```

Verify:

```bash
system "STRSQL"
# In the interactive SQL screen:
# SELECT * FROM CODELIVER1.PORTFOLIO
# Expected: 3 rows — PF001, PF002, PF003
```

Or verify from VS Code IBM i panel → right-click `CODELIVER1` → **Run SQL**:

```sql
SELECT PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS
FROM CODELIVER1.PORTFOLIO;
```

---

## 6. Layer 2A — PORTFINQ: RPGLE Portfolio Inquiry

### 6.1 What It Demonstrates

| IBM i Concept | Detail |
|---------------|--------|
| `**FREE` fully-free format | No column restrictions — code starts at column 1 |
| `CTL-OPT` | Control options — sets activation group, debug options |
| `DCL-PI` | Procedure Interface — equivalent to COBOL LINKAGE SECTION |
| `EXEC SQL` embedded SQL | SQLRPGLE — SQL inside RPG |
| `SQLCODE` handling | 0=found, 100=not found, negative=error |
| `*INLR = *ON` | Clean program exit — equivalent to COBOL STOP RUN |

### 6.2 Create the Source Member in VS Code

VS Code IBM i panel → right-click `CODELIVER1/QRPGLESRC` → **New Member** → name `PORTFINQ`, type `RPGLE`.

Paste this source code:

```rpgle
**FREE
// ============================================================
// PORTFINQ — Portfolio Inquiry (ILE RPG, SQLRPGLE)
// Purpose : Given a portfolio ID, return owner, value,
//           currency via DB2 for i embedded SQL
// IBM i   : **FREE format, DCL-PI, SQLRPGLE, SQLCODE, *INLR
// z/OS eq : COBOL LINKAGE SECTION + EXEC SQL SELECT INTO
// ============================================================

CTL-OPT DFTACTGRP(*NO)
        ACTGRP('JRAMSRV')
        OPTION(*SRCSTMT:*NODEBUGIO);

// DCL-PI = Procedure Interface (z/OS: COBOL LINKAGE SECTION)
DCL-PI PORTFINQ;
  pPortfId    CHAR(10);      // Input:  portfolio ID
  pOwner      CHAR(40);      // Output: owner name
  pTotalValue PACKED(15:2);  // Output: total portfolio value
  pCurrency   CHAR(3);       // Output: currency code
  pRetCode    CHAR(2);       // Output: 00=OK, 10=NotFound, 99=Error
END-PI;

// Working variables (not passed through parameter interface)
DCL-S wOwner    VARCHAR(40);
DCL-S wValue    PACKED(15:2);
DCL-S wCurrency CHAR(3);

// ── Main logic ───────────────────────────────────────────────
// EXEC SQL is identical in syntax to z/OS COBOL EXEC SQL
EXEC SQL
  SELECT OWNER, TOTAL_VALUE, CURRENCY
  INTO   :wOwner, :wValue, :wCurrency
  FROM   CODELIVER1.PORTFOLIO
  WHERE  PORTF_ID = :pPortfId
  FETCH FIRST 1 ROW ONLY;

// SQLCODE handling (z/OS: COBOL IF SQLCODE = 0 / 100 / other)
SELECT;
  WHEN SQLCODE = 0;     // Row found — populate outputs
    pOwner      = wOwner;
    pTotalValue = wValue;
    pCurrency   = wCurrency;
    pRetCode    = '00';
  WHEN SQLCODE = 100;   // Not found (z/OS: same SQLCODE 100)
    pOwner      = *BLANKS;
    pTotalValue = 0;
    pCurrency   = *BLANKS;
    pRetCode    = '10';
  OTHER;                // Error (negative SQLCODE)
    pRetCode    = '99';
ENDSL;

// *INLR = *ON: signals clean program exit (z/OS: COBOL STOP RUN)
*INLR = *ON;
RETURN;
```

### 6.3 Compile PORTFINQ

**Option A — from VS Code IBM i panel:**
Right-click the `PORTFINQ` member → **Compile** (or press `F1` → `IBM i: Compile`).

**Option B — from VS Code SSH terminal / IBM i PASE bash:**

```bash
system "CRTSQLRPGI OBJ(CODELIVER1/PORTFINQ) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFINQ) OBJTYPE(*PGM) COMMIT(*NONE) CLOSQLCSR(*ENDMOD)"
```

**Option C — from 5250 / SEU command line:**

```cl
CRTSQLRPGI OBJ(CODELIVER1/PORTFINQ) +
  SRCFILE(CODELIVER1/QRPGLESRC) +
  SRCMBR(PORTFINQ) +
  OBJTYPE(*PGM) +
  COMMIT(*NONE) +
  CLOSQLCSR(*ENDMOD)
```

Expected output:
```
Program PORTFINQ created in library CODELIVER1.
```

If errors, view the compile listing:

```bash
system "WRKSPLF"
# In green screen: type 5 next to the PORTFINQ spool file, F18 for errors
```

### 6.4 Run PORTFINQ

```bash
# Call with padded parameters — CHAR fields must be full length
system "CALL CODELIVER1/PORTFINQ PARM('PF001     ' '                                        ' 0 '   ' '  ')"
system "DSPJOBLOG"
```

---

## 7. Layer 2B — PORTFCBL: COBOL/400 Portfolio Inquiry

### 7.1 What It Demonstrates

PORTFCBL is the COBOL/400 version of PORTFINQ. It reads the **same DB2 table**, uses the **same embedded SQL**, and returns the **same outputs** — but in full z/OS COBOL syntax. This is your bridge credential: proving that z/OS COBOL expertise transfers directly to IBM i.

| COBOL/400 | z/OS COBOL | Notes |
|-----------|------------|-------|
| `IDENTIFICATION DIVISION` | Same | Identical |
| `WORKING-STORAGE SECTION` | Same | Identical |
| `LINKAGE SECTION` | Same | Identical |
| `EXEC SQL SELECT INTO` | Same | DB2 for i and DB2 for z/OS share the same SQL dialect |
| `EVALUATE SQLCODE` | `IF SQLCODE = 0` | Same logic, EVALUATE is cleaner |
| `STOP RUN` | Same | Identical |
| `CRTSQLCBLI` | `IGYCRCTL` | Different compile command, same concept |
| `PIC S9(13)V99 COMP-3` | Same | Packed decimal — identical |
| `EXEC SQL INCLUDE SQLCA` | Same | SQLCA structure — identical |

### 7.2 Create the Source Member in VS Code

VS Code IBM i panel → right-click `CODELIVER1/QCBLLESRC` → **New Member** → name `PORTFCBL`, type `CBLLE`.

Paste this source code:

```cobol
      *================================================================
      * PORTFCBL  — COBOL/400 Portfolio Inquiry
      * Purpose  : Read PORTFOLIO table by key using embedded SQL
      *            Demonstrates COBOL/400 on IBM i — syntax is
      *            IDENTICAL to z/OS COBOL for EXEC SQL sections
      * IBM i eq : PORTFINQ (RPGLE version of the same logic)
      * z/OS eq  : Standard COBOL batch with EXEC SQL SELECT INTO
      *================================================================
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PORTFCBL.
       AUTHOR. CODELIVER1.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-AS400.
       OBJECT-COMPUTER. IBM-AS400.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

      * SQL Communication Area — same structure as z/OS
       EXEC SQL
           INCLUDE SQLCA
       END-EXEC.

      * Working variables for SQL output
       01  WS-OWNER         PIC X(40).
       01  WS-CURRENCY      PIC X(3).
       01  WS-TOTAL-VALUE   PIC S9(13)V99 COMP-3.

       LINKAGE SECTION.
      * Parameters — equivalent to RPG DCL-PI
       01  LK-PORTF-ID      PIC X(10).
       01  LK-OWNER         PIC X(40).
       01  LK-TOTAL-VALUE   PIC S9(13)V99 COMP-3.
       01  LK-CURRENCY      PIC X(3).
       01  LK-RET-CODE      PIC X(2).

       PROCEDURE DIVISION USING
           LK-PORTF-ID
           LK-OWNER
           LK-TOTAL-VALUE
           LK-CURRENCY
           LK-RET-CODE.

       MAIN-PARA.
      * Embedded SQL — IDENTICAL syntax to z/OS COBOL EXEC SQL
           EXEC SQL
               SELECT OWNER, TOTAL_VALUE, CURRENCY
               INTO   :WS-OWNER, :WS-TOTAL-VALUE, :WS-CURRENCY
               FROM   CODELIVER1.PORTFOLIO
               WHERE  PORTF_ID = :LK-PORTF-ID
               FETCH FIRST 1 ROW ONLY
           END-EXEC.

      * SQLCODE handling — IDENTICAL to z/OS
           EVALUATE SQLCODE
               WHEN 0
                   MOVE WS-OWNER       TO LK-OWNER
                   MOVE WS-TOTAL-VALUE TO LK-TOTAL-VALUE
                   MOVE WS-CURRENCY    TO LK-CURRENCY
                   MOVE '00'           TO LK-RET-CODE
               WHEN 100
                   MOVE SPACES         TO LK-OWNER
                   MOVE ZEROS          TO LK-TOTAL-VALUE
                   MOVE SPACES         TO LK-CURRENCY
                   MOVE '10'           TO LK-RET-CODE
               WHEN OTHER
                   MOVE '99'           TO LK-RET-CODE
           END-EVALUATE.

           STOP RUN.
```

> **Column note:** COBOL/400 uses fixed-format columns:
> - Area A (cols 8–11): `DIVISION`, `SECTION`, paragraph names
> - Area B (cols 12–72): all statements
> - Comment lines: `*` in column 7

### 7.3 Compile PORTFCBL

**Option A — VS Code IBM i panel:**
Right-click `PORTFCBL` member → **Compile**.

**Option B — SSH terminal / IBM i PASE bash:**

```bash
system "CRTSQLCBLI OBJ(CODELIVER1/PORTFCBL) SRCFILE(CODELIVER1/QCBLLESRC) SRCMBR(PORTFCBL) OBJTYPE(*PGM) COMMIT(*NONE)"
```

**Option C — from 5250 / SEU command line:**

```cl
CRTSQLCBLI OBJ(CODELIVER1/PORTFCBL) +
  SRCFILE(CODELIVER1/QCBLLESRC) +
  SRCMBR(PORTFCBL) +
  OBJTYPE(*PGM) +
  COMMIT(*NONE)
```

Expected:
```
Program PORTFCBL created in library CODELIVER1.
```

### 7.4 Run PORTFCBL

```bash
system "CALL CODELIVER1/PORTFCBL PARM('PF001     ' '                                        ' 0 '   ' '  ')"
system "DSPJOBLOG"
# Expected: No errors. Return code in 5th parameter = '00'
```

---

## 8. Layer 2C — ORDPROC: CL Driver (calls both RPGLE + COBOL)

### 8.1 What It Demonstrates

ORDPROC is the CL (Control Language) driver. CL is IBM i's equivalent of JCL — but it is a real programming language with variables, conditional logic, and error handling.

| CL Concept | z/OS Equivalent |
|------------|-----------------|
| `PGM / ENDPGM` | `//EXEC PGM=` block in JCL |
| `DCL VAR(&X) TYPE(*CHAR)` | JCL symbolic `&X` or COBOL WORKING-STORAGE |
| `MONMSG MSGID(CPF0000)` | JCL `COND=` parameter or COBOL `ON EXCEPTION` |
| `CALL PGM(LIB/PGM) PARM(...)` | `EXEC PGM=` with PARM or COBOL CALL |
| `SNDPGMMSG MSG(...)` | `DSPLY` in COBOL or write to SYSOUT |
| `GOTO CMDLBL(label)` | COBOL `GO TO paragraph` |

### 8.2 Create the Source Member in VS Code

VS Code IBM i panel → right-click `CODELIVER1/QCLSRC` → **New Member** → name `ORDPROC`, type `CLLE`.

```cl
/* ======================================================== */
/* ORDPROC  — CL Driver Program                             */
/* Purpose  : Calls PORTFINQ (RPGLE) then PORTFCBL (COBOL)  */
/*            Demonstrates CL as a programming language     */
/* IBM i    : DCL vars, MONMSG, CALL PARM, IF/DO, GOTO     */
/* z/OS eq  : JCL EXEC PGM= + COND= + COBOL CALL           */
/* ======================================================== */
PGM

/* Variable declarations — z/OS: JCL symbolics / COBOL WS */
DCL VAR(&PORTFID)  TYPE(*CHAR) LEN(10) VALUE('PF001     ')
DCL VAR(&OWNER)    TYPE(*CHAR) LEN(40)
DCL VAR(&TOTVAL)   TYPE(*DEC)  LEN(15 2) VALUE(0)
DCL VAR(&CURRENCY) TYPE(*CHAR) LEN(3)
DCL VAR(&RETCODE)  TYPE(*CHAR) LEN(2)
DCL VAR(&MSG)      TYPE(*CHAR) LEN(100)

/* Global error handler — z/OS: JCL COND= or COBOL ON EXCEPTION */
MONMSG MSGID(CPF0000) EXEC(GOTO CMDLBL(ERRHANDLER))

/* ── CALL 1: RPGLE Program ─────────────────────────────── */
SNDPGMMSG MSG('--- Calling PORTFINQ (RPGLE) ---')
CALL PGM(CODELIVER1/PORTFINQ) +
     PARM(&PORTFID &OWNER &TOTVAL &CURRENCY &RETCODE)

IF COND(&RETCODE *EQ '00') THEN(DO)
  CHGVAR VAR(&MSG) VALUE('RPGLE result: ' *CAT +
                          %TRIM(&OWNER) *BCAT &CURRENCY)
  SNDPGMMSG MSG(&MSG)
ENDDO

/* Reset output variables before second call */
CHGVAR VAR(&OWNER)   VALUE('                                        ')
CHGVAR VAR(&RETCODE) VALUE('  ')

/* ── CALL 2: COBOL/400 Program ─────────────────────────── */
SNDPGMMSG MSG('--- Calling PORTFCBL (COBOL/400) ---')
CALL PGM(CODELIVER1/PORTFCBL) +
     PARM(&PORTFID &OWNER &TOTVAL &CURRENCY &RETCODE)

IF COND(&RETCODE *EQ '00') THEN(DO)
  CHGVAR VAR(&MSG) VALUE('COBOL result: ' *CAT +
                          %TRIM(&OWNER) *BCAT &CURRENCY)
  SNDPGMMSG MSG(&MSG)
ENDDO

GOTO CMDLBL(ENDPGM)

ERRHANDLER:
  SNDPGMMSG MSGID(CPF9898) MSGF(QCPFMSG) +
            MSGDTA('ORDPROC failed — check job log') +
            MSGTYPE(*ESCAPE)

ENDPGM:
ENDPGM
```

### 8.3 Compile ORDPROC

**Option A — VS Code IBM i panel:**
Right-click `ORDPROC` → **Compile**.

**Option B — SSH terminal / IBM i PASE bash:**

```bash
system "CRTBNDCL PGM(CODELIVER1/ORDPROC) SRCFILE(CODELIVER1/QCLSRC) SRCMBR(ORDPROC)"
```

**Option C — from 5250 / SEU command line:**

```cl
CRTBNDCL PGM(CODELIVER1/ORDPROC) +
  SRCFILE(CODELIVER1/QCLSRC) +
  SRCMBR(ORDPROC)
```

### 8.4 Run ORDPROC

```bash
system "CALL CODELIVER1/ORDPROC"
system "DSPJOBLOG"
```

Expected job log output:
```
--- Calling PORTFINQ (RPGLE) ---
RPGLE result: Richard Papen USD
--- Calling PORTFCBL (COBOL/400) ---
COBOL result: Richard Papen USD
```

---

## 9. Layer 2D — PORTFSVC: ILE Service Program

### 9.1 What It Demonstrates

A `*SRVPGM` (Service Program) is the most technically impressive IBM i concept in the project. It is a shared library of reusable exported procedures — closer to a DLL than a regular program because it stays resident in memory across calls.

Conceptual mapping:

| IBM i ILE concept | Modern equivalent |
|-------------------|-------------------|
| `*MODULE` | `.o` object file before linking |
| `*SRVPGM` | Shared library / Java or Kotlin JAR-style utility library |
| Exported procedure | Public method or function |
| `*BNDDIR` | Dependency management / library search path |
| Binder source | API contract and versioning metadata |
| `*PGM` | Executable application |
| `CRTRPGMOD` | Compile source to module |
| `CRTSRVPGM` | Link/build shared library |
| `CRTBNDRPG` | Compile and link executable RPG program |

`PORTFSVC` exports reusable procedures:

```text
VALIDATEPORTFOLIO
VALIDATECURRENCY
FORMATPORTFOLIOMSG
```

A caller does not execute every procedure in a service program. A service
program behaves like a utility library: callers bind to the library and invoke
only the exported procedures they need.

```text
PORTFTEST
   |
   +--> validatePortfolio(...)
   +--> validateCurrency(...)
   +--> formatPortfolioMsg(...)
```

The two-step ILE build:

```
Source (.RPGLE)
    │
    ▼ CRTRPGMOD
*MODULE  (compiled but not yet runnable)
    │
    ▼ CRTSRVPGM
*SRVPGM  (shareable library of exported procedures)
```

z/OS equivalent: `IGYCRCTL` compile → `IEWL` link-edit → shared load module.

### 9.2 Create the Source Member in VS Code

VS Code IBM i panel → right-click `CODELIVER1/QRPGLESRC` → **New Member** → name `PORTFSVC`, type `RPGLE`.

```rpgle
**FREE
// ============================================================
// PORTFSVC — ILE Service Program
// Purpose  : Shared library of 3 exported validation procs
// IBM i    : CTL-OPT NOMAIN, DCL-PROC EXPORT, *SRVPGM
// z/OS eq  : IEWL link-edit / shared DLL
// Build    : CRTRPGMOD then CRTSRVPGM (two-step ILE build)
// ============================================================

// NOMAIN = no standalone entry point — this is a *SRVPGM candidate
// z/OS: like a CSECT with no main() — only exported entry points
CTL-OPT NOMAIN;

// ── Procedure 1: validatePortfolio ───────────────────────────
// Checks that the portfolio has active status and positive value
// Returns: '00'=valid, '10'=inactive, '20'=zero/negative value
DCL-PROC validatePortfolio EXPORT;
  DCL-PI *N CHAR(2);
    pStatus     CHAR(1)      CONST;   // 'A'=Active, 'I'=Inactive
    pTotalValue PACKED(15:2) CONST;
  END-PI;

  IF pStatus <> 'A';
    RETURN '10';    // Portfolio is not active
  ENDIF;

  IF pTotalValue <= 0;
    RETURN '20';    // Value is zero or negative
  ENDIF;

  RETURN '00';      // All validations passed
END-PROC;

// ── Procedure 2: validateCurrency ────────────────────────────
// Checks that currency code is one of the 4 supported codes
// Returns: *ON (*TRUE) if valid, *OFF (*FALSE) if not
DCL-PROC validateCurrency EXPORT;
  DCL-PI *N IND;
    pCurrency CHAR(3) CONST;
  END-PI;

  SELECT;
    WHEN pCurrency = 'USD' OR pCurrency = 'EUR' OR
         pCurrency = 'CHF' OR pCurrency = 'GBP';
      RETURN *ON;
    OTHER;
      RETURN *OFF;
  ENDSL;
END-PROC;

// ── Procedure 3: formatPortfolioMsg ──────────────────────────
// Builds a human-readable portfolio summary string
// Returns: VARCHAR(100) formatted message
DCL-PROC formatPortfolioMsg EXPORT;
  DCL-PI *N VARCHAR(100);
    pPortfId    CHAR(10)     CONST;
    pOwner      CHAR(40)     CONST;
    pTotalValue PACKED(15:2) CONST;
    pCurrency   CHAR(3)      CONST;
  END-PI;

  RETURN %TRIM(pPortfId) + ' | ' +
         %TRIM(pOwner)   + ' | ' +
         pCurrency       + ' '   +
         %CHAR(pTotalValue);
END-PROC;
```

### 9.3 Compile PORTFSVC — Two-Step ILE Build

**Step 1 — Compile source to *MODULE:**

```bash
system "CRTRPGMOD MODULE(CODELIVER1/PORTFSVC) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFSVC)"
# Expected: Module PORTFSVC created in library CODELIVER1.
```

5250 / SEU command-line form:

```cl
CRTRPGMOD MODULE(CODELIVER1/PORTFSVC) +
  SRCFILE(CODELIVER1/QRPGLESRC) +
  SRCMBR(PORTFSVC)
```

**Step 2 — Bind *MODULE into *SRVPGM:**

```bash
system "CRTSRVPGM SRVPGM(CODELIVER1/PORTFSVC) MODULE(CODELIVER1/PORTFSVC) EXPORT(*ALL)"
# Expected: Service program PORTFSVC created in library CODELIVER1.
```

5250 / SEU command-line form:

```cl
CRTSRVPGM SRVPGM(CODELIVER1/PORTFSVC) +
  MODULE(CODELIVER1/PORTFSVC) +
  EXPORT(*ALL)
```

`CRTSRVPGM` does not use `ACTGRP()` here. Keep the named activation group on the
bound caller programs, such as `PORTFTEST` and `UTEST01`, where the examples use
`CTL-OPT DFTACTGRP(*NO) ACTGRP('JRAMSRV');`.

**Verify exported procedures:**

```bash
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC)"
# Press F11 in the green screen — you should see 3 exports:
#   validatePortfolio
#   validateCurrency
#   formatPortfolioMsg
```

Use the procedure export detail view when diagnosing binding problems:

```bash
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
# Expected exports may display in uppercase:
#   FORMATPORTFOLIOMSG
#   VALIDATECURRENCY
#   VALIDATEPORTFOLIO
```

### 9.4 Enterprise Patterns for Service Programs

The examples above use `EXPORT(*ALL)` because it is quick for learning. Large
IBM i shops, especially in banking, normally make service-program interfaces
more explicit.

**Pattern 1 — Service program plus shared prototype member**

Production teams avoid copying the same `DCL-PR` prototypes into every caller.
Instead they create one shared prototype member:

```text
QRPGLESRC
├── PORTFSVC      source for the service program
├── PORTFSVCPR    shared procedure prototypes
└── PORTFTEST     consumer program
```

`PORTFSVCPR` contains the procedure declarations:

```rpgle
DCL-PR validatePortfolio CHAR(2) EXTPROC('VALIDATEPORTFOLIO');
  pStatus     CHAR(1)      CONST;
  pTotalValue PACKED(15:2) CONST;
END-PR;

DCL-PR validateCurrency IND EXTPROC('VALIDATECURRENCY');
  pCurrency CHAR(3) CONST;
END-PR;

DCL-PR formatPortfolioMsg VARCHAR(100) EXTPROC('FORMATPORTFOLIOMSG');
  pPortfId    CHAR(10)     CONST;
  pOwner      CHAR(40)     CONST;
  pTotalValue PACKED(15:2) CONST;
  pCurrency   CHAR(3)      CONST;
END-PR;
```

Consumer programs include it:

```rpgle
/COPY QRPGLESRC,PORTFSVCPR
```

or:

```rpgle
/INCLUDE QRPGLESRC,PORTFSVCPR
```

Benefits: one source of truth, less prototype drift, and easier maintenance.

**Pattern 2 — Binder source**

Instead of `EXPORT(*ALL)`, production environments often use binder source to
define the public API of the service program.

Create member `PORTFSVC` in `CODELIVER1/QSRVSRC`:

```text
STRPGMEXP PGMLVL(*CURRENT)
  EXPORT SYMBOL('VALIDATEPORTFOLIO')
  EXPORT SYMBOL('VALIDATECURRENCY')
  EXPORT SYMBOL('FORMATPORTFOLIOMSG')
ENDPGMEXP
```

Build with binder source:

```bash
system "CRTSRVPGM SRVPGM(CODELIVER1/PORTFSVC) MODULE(CODELIVER1/PORTFSVC) EXPORT(*SRCFILE) SRCFILE(CODELIVER1/QSRVSRC) SRCMBR(PORTFSVC)"
```

5250 / SEU form:

```cl
CRTSRVPGM SRVPGM(CODELIVER1/PORTFSVC) +
  MODULE(CODELIVER1/PORTFSVC) +
  EXPORT(*SRCFILE) +
  SRCFILE(CODELIVER1/QSRVSRC) +
  SRCMBR(PORTFSVC)
```

Benefits: stable interface contracts, versioning support, backward
compatibility, and API-style evolution.

**Pattern 3 — Binding directories**

Large IBM i environments use binding directories to centralize service-program
dependencies:

```bash
system "CRTBNDDIR BNDDIR(CODELIVER1/PORTFBNDD) TEXT('Portfolio service binding directory')"
system "ADDBNDDIRE BNDDIR(CODELIVER1/PORTFBNDD) OBJ((CODELIVER1/PORTFSVC *SRVPGM))"
system "DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)"
```

If `CRTBNDDIR` reports `CPF5D10` or `CPF5D0B`, the binding directory already
exists. Verify it with `DSPBNDDIR` and continue. If `ADDBNDDIRE` reports that
the entry already exists, continue to the program compile.

Consumer programs can specify the binding directory in source:

```rpgle
CTL-OPT DFTACTGRP(*NO) ACTGRP('JRAMSRV') BNDDIR('PORTFBNDD');
```

or at compile time with `BNDDIR(CODELIVER1/PORTFBNDD)`.

Recommended production flow:

```text
CRTRPGMOD PORTFSVC
    ↓
CRTSRVPGM PORTFSVC
    ↓
ADDBNDDIRE PORTFBNDD
    ↓
CRTBNDRPG PORTFTEST
```

---

## 10. Layer 2E — PORTFTEST: Service Program Caller

### 10.1 What It Demonstrates

PORTFTEST calls all three PORTFSVC procedures using `DCL-PR` procedure
prototypes. The prototype tells the compiler the exact signature of the
external procedure before the call, providing compile-time type checking. This
learning version keeps the prototypes in `PORTFTEST`; the enterprise pattern is
to move them into shared member `PORTFSVCPR` and include it with `/COPY` or
`/INCLUDE`.

### 10.2 Create the PORTFOUT Output File

Create one simple physical file to hold the `PORTFTEST` results. This avoids
transient display messages and gives you persistent output you can inspect with
native IBM i command `DSPPFM`.

Run this once:

```bash
system "CRTPF FILE(CODELIVER1/PORTFOUT) RCDLEN(100) TEXT('PORTFTEST visible output')"
```

5250 command-line form:

```cl
CRTPF FILE(CODELIVER1/PORTFOUT) RCDLEN(100) TEXT('PORTFTEST visible output')
```

If `CRTPF` says `PORTFOUT` already exists, continue.

Verify:

```bash
system "WRKOBJ OBJ(CODELIVER1/PORTFOUT) OBJTYPE(*FILE)"
```

### 10.3 Create the PORTFTEST Source Member in VS Code

VS Code IBM i panel → right-click `CODELIVER1/QRPGLESRC` → **New Member** → name `PORTFTEST`, type `RPGLE`.

```rpgle
**FREE
// ============================================================
// PORTFTEST - Tests PORTFSVC and writes visible output to PORTFOUT
// IBM i    : DCL-PR prototype, binding directory, calling *SRVPGM
// z/OS eq  : COBOL CALL with type-checked USING clause
// ============================================================

CTL-OPT DFTACTGRP(*NO) ACTGRP('JRAMSRV');

DCL-F PORTFOUT DISK(100) USAGE(*OUTPUT);

// ── Prototypes for PORTFSVC procedures ───────────────────────
// DCL-PR = prototype declaration (tells compiler the signature)
// EXTPROC('...') = match the exported procedure name in PORTFSVC
// z/OS: COBOL CALL literal with type-checked USING clause

DCL-PR validatePortfolio CHAR(2) EXTPROC('VALIDATEPORTFOLIO');
  pStatus     CHAR(1)      CONST;
  pTotalValue PACKED(15:2) CONST;
END-PR;

DCL-PR validateCurrency IND EXTPROC('VALIDATECURRENCY');
  pCurrency CHAR(3) CONST;
END-PR;

DCL-PR formatPortfolioMsg VARCHAR(100) EXTPROC('FORMATPORTFOLIOMSG');
  pPortfId    CHAR(10)     CONST;
  pOwner      CHAR(40)     CONST;
  pTotalValue PACKED(15:2) CONST;
  pCurrency   CHAR(3)      CONST;
END-PR;

// ── Output record buffer for program-described file PORTFOUT ─
DCL-DS outRec LEN(100);
  outText CHAR(100) POS(1);
END-DS;

// ── Test variables ───────────────────────────────────────────
DCL-S wRetCode   CHAR(2);
DCL-S wIsValid   IND;
DCL-S wMsg       VARCHAR(100);
DCL-S wStatus    CHAR(1)      INZ('A');
DCL-S wValue     PACKED(15:2) INZ(150000.00);
DCL-S wCurrency  CHAR(3)      INZ('USD');
DCL-S wPortfId   CHAR(10)     INZ('PF001     ');
DCL-S wOwner     CHAR(40)     INZ('Richard Papen                           ');

// ── Test 1: validatePortfolio — active portfolio with value ──
wRetCode = validatePortfolio(wStatus : wValue);
IF wRetCode = '00';
  outText = 'TEST1 PASS: Portfolio is valid (status=A, value>0)';
ELSE;
  outText = 'TEST1 FAIL: validatePortfolio returned ' + wRetCode;
ENDIF;
WRITE PORTFOUT outRec;

// ── Test 2: validateCurrency — USD is a valid currency ───────
wIsValid = validateCurrency(wCurrency);
IF wIsValid = *ON;
  outText = 'TEST2 PASS: Currency USD is valid';
ELSE;
  outText = 'TEST2 FAIL: Currency USD rejected';
ENDIF;
WRITE PORTFOUT outRec;

// ── Test 3: formatPortfolioMsg — builds display string ───────
wMsg = formatPortfolioMsg(wPortfId : wOwner : wValue : wCurrency);
outText = 'TEST3 MSG: ' + %TRIM(wMsg);
WRITE PORTFOUT outRec;

*INLR = *ON;
RETURN;
```

### 10.4 Compile PORTFTEST

Create a binding directory once and add the service program to it:

```bash
system "CRTBNDDIR BNDDIR(CODELIVER1/PORTFBNDD) TEXT('Portfolio service binding directory')"
system "ADDBNDDIRE BNDDIR(CODELIVER1/PORTFBNDD) OBJ((CODELIVER1/PORTFSVC *SRVPGM))"
```

If `CRTBNDDIR` says the binding directory already exists, continue with
`ADDBNDDIRE`. If `ADDBNDDIRE` says the entry already exists, continue with the
compile.

Compile `PORTFTEST` with `CRTBNDRPG`. Use `CRTBNDRPG` because this member is
plain RPGLE and contains no embedded SQL:

```bash
system "CRTBNDRPG PGM(CODELIVER1/PORTFTEST) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFTEST) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
```

5250 / SEU command-line form:

```cl
CRTBNDDIR BNDDIR(CODELIVER1/PORTFBNDD) +
  TEXT('Portfolio service binding directory')

ADDBNDDIRE BNDDIR(CODELIVER1/PORTFBNDD) +
  OBJ((CODELIVER1/PORTFSVC *SRVPGM))

CRTBNDRPG PGM(CODELIVER1/PORTFTEST) +
  SRCFILE(CODELIVER1/QRPGLESRC) +
  SRCMBR(PORTFTEST) +
  DFTACTGRP(*NO) +
  ACTGRP(JRAMSRV) +
  BNDDIR(CODELIVER1/PORTFBNDD)
```

`CRTSQLRPGI` is for RPG members that contain embedded SQL. If you use it on a
non-SQL member, it can emit `SQL0053 - No SQL statements found`. For non-SQL
RPGLE callers like `PORTFTEST`, use `CRTBNDRPG`. For SQL RPGLE programs like
`PORTFINQ` and `ORDRBATCH`, keep using `CRTSQLRPGI`.

### 10.5 Run PORTFTEST and View the Output

Clear old rows, call the test program, then display the output file:

```bash
system "CLRPFM FILE(CODELIVER1/PORTFOUT)"
system "CALL CODELIVER1/PORTFTEST"
system "DSPPFM FILE(CODELIVER1/PORTFOUT)"
```

5250 command-line form:

```cl
CLRPFM FILE(CODELIVER1/PORTFOUT)
CALL PGM(CODELIVER1/PORTFTEST)
DSPPFM FILE(CODELIVER1/PORTFOUT)
```

Expected records in `PORTFOUT`:

```text
TEST1 PASS: Portfolio is valid (status=A, value>0)
TEST2 PASS: Currency USD is valid
TEST3 MSG: PF001 | Richard Papen | USD 150000.00
```

`DSPPFM` keeps the output on screen, so this is the preferred native IBM i
verification method for this exercise.

### 10.6 Optional: QPRINT Spool Output for VS Code

Use this optional version when you want `PORTFTEST` to create a real spool file
that appears in the VS Code **SPOOLED FILE BROWSER**. This version writes to
IBM i standard printer file `QPRINT`, so you do not need to create DDS or a
custom printer file.

Replace `CODELIVER1/QRPGLESRC(PORTFTEST)` with this source only when you want
spool output instead of `PORTFOUT` file output:

```rpgle
**FREE
// ============================================================
// PORTFTEST - Tests PORTFSVC and writes visible output to QPRINT
// IBM i    : DCL-PR prototype, binding directory, QPRINT spool
// z/OS eq  : COBOL CALL with SYSOUT-style test output
// ============================================================

CTL-OPT DFTACTGRP(*NO) ACTGRP('JRAMSRV');

DCL-F QPRINT PRINTER(132) USAGE(*OUTPUT);

// ── Prototypes for PORTFSVC procedures ───────────────────────
DCL-PR validatePortfolio CHAR(2) EXTPROC('VALIDATEPORTFOLIO');
  pStatus     CHAR(1)      CONST;
  pTotalValue PACKED(15:2) CONST;
END-PR;

DCL-PR validateCurrency IND EXTPROC('VALIDATECURRENCY');
  pCurrency CHAR(3) CONST;
END-PR;

DCL-PR formatPortfolioMsg VARCHAR(100) EXTPROC('FORMATPORTFOLIOMSG');
  pPortfId    CHAR(10)     CONST;
  pOwner      CHAR(40)     CONST;
  pTotalValue PACKED(15:2) CONST;
  pCurrency   CHAR(3)      CONST;
END-PR;

// ── Output record buffer for QPRINT ──────────────────────────
DCL-DS printRec LEN(132);
  printText CHAR(132) POS(1);
END-DS;

// ── Test variables ───────────────────────────────────────────
DCL-S wRetCode   CHAR(2);
DCL-S wIsValid   IND;
DCL-S wMsg       VARCHAR(100);
DCL-S wStatus    CHAR(1)      INZ('A');
DCL-S wValue     PACKED(15:2) INZ(150000.00);
DCL-S wCurrency  CHAR(3)      INZ('USD');
DCL-S wPortfId   CHAR(10)     INZ('PF001     ');
DCL-S wOwner     CHAR(40)     INZ('Richard Papen                           ');

// ── Test 1: validatePortfolio — active portfolio with value ──
wRetCode = validatePortfolio(wStatus : wValue);
IF wRetCode = '00';
  printText = 'TEST1 PASS: Portfolio is valid (status=A, value>0)';
ELSE;
  printText = 'TEST1 FAIL: validatePortfolio returned ' + wRetCode;
ENDIF;
WRITE QPRINT printRec;

// ── Test 2: validateCurrency — USD is a valid currency ───────
wIsValid = validateCurrency(wCurrency);
IF wIsValid = *ON;
  printText = 'TEST2 PASS: Currency USD is valid';
ELSE;
  printText = 'TEST2 FAIL: Currency USD rejected';
ENDIF;
WRITE QPRINT printRec;

// ── Test 3: formatPortfolioMsg — builds display string ───────
wMsg = formatPortfolioMsg(wPortfId : wOwner : wValue : wCurrency);
printText = 'TEST3 MSG: ' + %TRIM(wMsg);
WRITE QPRINT printRec;

*INLR = *ON;
RETURN;
```

Compile the QPRINT version:

```bash
system "CRTBNDRPG PGM(CODELIVER1/PORTFTEST) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFTEST) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
```

Run it interactively from a 5250 command line:

```cl
CALL PGM(CODELIVER1/PORTFTEST)
```

Then view the spool from native IBM i:

```cl
WRKSPLF SELECT(CODELIVER)
```

Look for the newest spool file named `QPRINT`, then use option `5` to display
it.

To view the same spool from VS Code:

1. Open the IBM i **SPOOLED FILE BROWSER**.
2. Expand your user, for example `CODELIVER`.
3. Refresh the browser.
4. Open the newest `QPRINT.splf`.
5. Search for:

```text
TEST1 PASS
TEST2 PASS
TEST3 MSG
```

If you only see `QPJOBLOG.splf`, you opened the job log, not the test output.
Refresh and look specifically for `QPRINT.splf`.

### 10.7 Troubleshooting PORTFTEST Compile Failures

If `PORTFTEST` fails with this compile-listing message:

```text
RNF5191: The Result-Field is not a data structure when Factor 2 is a file name.
```

then `outRec` was declared as a scalar field instead of a data structure. In
section 10.3, make sure this block appears before the working variables:

```rpgle
DCL-DS outRec LEN(100);
  outText CHAR(100) POS(1);
END-DS;
```

Assignments must use `outText`, and writes must use `WRITE PORTFOUT outRec;`.

For the optional `QPRINT` version, the equivalent required block is:

```rpgle
DCL-DS printRec LEN(132);
  printText CHAR(132) POS(1);
END-DS;
```

Assignments must use `printText`, and writes must use `WRITE QPRINT printRec;`.

Verify both objects:

```bash
system "WRKOBJ OBJ(CODELIVER1/PORTFOUT) OBJTYPE(*FILE)"
system "WRKOBJ OBJ(CODELIVER1/PORTFTEST) OBJTYPE(*PGM)"
```

If the compile listing also shows these entries under statically bound
procedures, binding is working:

```text
validateCurrency
validatePortfolio
formatPortfolioMsg
```

That confirms `PORTFBNDD` was found, `PORTFSVC` was found, and the exported
procedure names matched the caller prototypes. A service program (`*SRVPGM`) is
similar to a shared library or DLL: callers bind to exported procedures and call
only the procedures they need.

If the compile listing shows zero RPG errors but ends with:

```text
Errors were found during the binding step.
Program PORTFTEST in library CODELIVER1 not created.
```

then the source compiled, but the executable program was not linked. Compilation
success does not guarantee successful binding. Immediately inspect the job log:

```bash
system "DSPJOBLOG"
```

Then verify both sides of the bind:

```bash
system "DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)"
system "DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)"
```

Typical causes are a missing binding-directory entry, a stale service program,
or prototype `EXTPROC()` names that do not match the exported procedure names.

---

## 11. Layer 2F — ORDRBATCH: Batch Order Processor

### 11.1 What It Demonstrates

ORDRBATCH is the central batch program for the banking interview narrative. It models the nightly settlement run: find pending trade orders, mark them processed under commitment control, commit the unit of work, and raise an escape message if DB2 rejects the update or commit.

The first learning version used an SQL cursor. The promoted version below is the verified production-style version: one set-based SQL update moves all `PEND` rows to `PROC`, then one commit completes the batch. This avoided the PUB400 cursor/update wait path and is easier to reason about in an operations handover.

| IBM i Concept | z/OS Equivalent |
|---------------|-----------------|
| `CRTSQLRPGI ... COMMIT(*CHG)` | COBOL/DB2 program running under commit control |
| Set-based `UPDATE ... WHERE STATUS = 'PEND'` | Searched SQL update in a batch step |
| `GET DIAGNOSTICS ... ROW_COUNT` | SQL row-count check after DML |
| `EXEC SQL COMMIT` | COBOL `EXEC SQL COMMIT` |
| `EXEC SQL ROLLBACK` | COBOL `EXEC SQL ROLLBACK` |
| `CPF9898` escape on fatal failure | Abend/return-code path for operations |

### 11.2 Create the Source Member in VS Code

VS Code IBM i panel -> right-click `CODELIVER1/QRPGLESRC` -> **New Member** -> name `ORDRBATCH`, type `SQLRPGLE`.

If the member already exists with the wrong source type, change it:

```cl
CHGPFM FILE(CODELIVER1/QRPGLESRC) MBR(ORDRBATCH) SRCTYPE(SQLRPGLE)
```

```rpgle
**FREE
// ============================================================
// ORDRBATCH - Batch Order Processor (SQLRPGLE)
// Purpose  : Process all PEND orders with one set-based update,
//            commit once, rollback on failure
//            write production-style diagnostics to the job log
// IBM i    : CRTSQLRPGI COMMIT(*CHG)
// ============================================================

CTL-OPT DFTACTGRP(*NO) ACTGRP(*NEW);

// IBM i job-log API. CPF9898 lets the program send plain text messages.
DCL-PR QMHSNDPM EXTPGM('QMHSNDPM');
  pMsgId       CHAR(7)   CONST;
  pMsgFile     CHAR(20)  CONST;
  pMsgData     CHAR(512) CONST OPTIONS(*VARSIZE);
  pMsgDataLen  INT(10)   CONST;
  pMsgType     CHAR(10)  CONST;
  pCallStack   CHAR(10)  CONST;
  pCallStackCtr INT(10)  CONST;
  pMsgKey      CHAR(4);
  pErrorCode   CHAR(8)   CONST;
END-PR;

DCL-PR LogInfo;
  pText VARCHAR(512) CONST;
END-PR;

DCL-PR LogFatal;
  pText VARCHAR(512) CONST;
END-PR;

DCL-PR SqlText VARCHAR(256);
  pStep VARCHAR(80) CONST;
END-PR;

// Working variables
DCL-S wOrderId   CHAR(20);
DCL-S wPortfId   CHAR(10);
DCL-S wIsin      CHAR(12);
DCL-S wQty       PACKED(15:4);
DCL-S wPrice     PACKED(15:4);
DCL-S wCount     INT(10) INZ(0);
DCL-S wCommitted INT(10) INZ(0);
DCL-S wRows      INT(10) INZ(0);

LogInfo('ORDRBATCH started');

// Process all currently pending rows as one batch unit of work.
EXEC SQL
  UPDATE CODELIVER1.TRADE_ORDERS
     SET STATUS     = 'PROC',
         PROCESS_DT = CURRENT_DATE
   WHERE STATUS     = 'PEND';

IF SQLCODE < 0;
  EXEC SQL
    ROLLBACK;
  LogFatal(SqlText('UPDATE TRADE_ORDERS failed'));
  *INLR = *ON;
  RETURN;
ENDIF;

EXEC SQL
  GET DIAGNOSTICS :wRows = ROW_COUNT;

wCount = wRows;

EXEC SQL
  COMMIT;

IF SQLCODE < 0;
  EXEC SQL
    ROLLBACK;
  LogFatal(SqlText('COMMIT failed'));
ELSE;
  wCommitted = wCount;
ENDIF;

LogInfo('ORDRBATCH ended. Processed rows=' + %CHAR(wCount)
        + ', committed rows=' + %CHAR(wCommitted));

*INLR = *ON;
RETURN;

DCL-PROC LogInfo;
  DCL-PI *N;
    pText VARCHAR(512) CONST;
  END-PI;

  // Informational logging is intentionally quiet in batch/PASE runs.
END-PROC;

DCL-PROC LogFatal;
  DCL-PI *N;
    pText VARCHAR(512) CONST;
  END-PI;

  DCL-S msgText CHAR(512);
  DCL-S msgKey  CHAR(4);
  DCL-S apiErr  CHAR(8) INZ(X'0000000000000000');

  msgText = %SUBST(pText:1:%MIN(%LEN(pText):512));

  QMHSNDPM('CPF9898'
          :'QCPFMSG   *LIBL     '
          :msgText
          :%MIN(%LEN(%TRIMR(msgText)):512)
          :'*ESCAPE   '
          :'*EXT      '
          :0
          :msgKey
          :apiErr);
END-PROC;

DCL-PROC SqlText;
  DCL-PI *N VARCHAR(256);
    pStep VARCHAR(80) CONST;
  END-PI;

  RETURN pStep
       + '. SQLCODE=' + %CHAR(SQLCODE)
       + ', SQLSTATE=' + SQLSTATE;
END-PROC;
```

### 11.3 Compile ORDRBATCH

```bash
system "CRTSQLRPGI OBJ(CODELIVER1/ORDRBATCH) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(ORDRBATCH) OBJTYPE(*PGM) COMMIT(*CHG) CLOSQLCSR(*ENDMOD)"
```

5250 / SEU command-line form:

```cl
CRTSQLRPGI OBJ(CODELIVER1/ORDRBATCH) +
  SRCFILE(CODELIVER1/QRPGLESRC) +
  SRCMBR(ORDRBATCH) +
  OBJTYPE(*PGM) +
  COMMIT(*CHG) +
  CLOSQLCSR(*ENDMOD)
```

### 11.4 Run ORDRBATCH

Important: `RUNSQLSTM` recreates `TRADE_ORDERS`. Because `ORDRBATCH` is compiled with `COMMIT(*CHG)`, restart journaling after every table reset before calling `ORDRBATCH`.

```bash
system "RUNSQLSTM SRCFILE(CODELIVER1/QSQLSRC) SRCMBR(CRTTABLES) COMMIT(*NONE) NAMING(*SQL) DECMPT(*PERIOD)"
system "STRJRNPF FILE(CODELIVER1/TRADE00001) JRN(CODELIVER1/ORDJRN)"
system -v "CALL PGM(CODELIVER1/ORDRBATCH)"
echo ORDRBATCH_RC:$?
```

Export the table to a readable UTF-8 file from VS Code / SSH / PASE:

```bash
system "CPYTOIMPF FROMFILE(CODELIVER1/TRADE00001) TOSTMF('/home/CODELIVER/trade_orders_vfy.csv') MBROPT(*REPLACE) STMFCCSID(1208) RCDDLM(*CRLF) DTAFMT(*DLM)"
cat /home/CODELIVER/trade_orders_vfy.csv
```

Expected successful result:

```text
ORDRBATCH_RC:0
TRADE_ORDERS: every seeded row has STATUS="PROC" and PROCESS_DT populated
```

`ORDRBATCH` is intentionally quiet on success in batch/PASE paths. On failure it raises `CPF9898` with `SQLCODE` and `SQLSTATE`, so the job ends non-zero and the native job log contains the failing DB2 operation.

### 11.5 Troubleshooting SQL7008 During ORDRBATCH

If `ORDRBATCH` compiles but the rows stay `PEND`, check for SQL7008. This means DB2 rejected the update under commitment control:

```text
SQL7008
TRADE00001 in CODELIVER1 not valid for operation.
Reason code 3: file is not journaled, no authority to the journal, or the journal is not usable.
```

Root cause: `CRTSQLRPGI ... COMMIT(*CHG)` runs SQL updates under commitment control. On IBM i, a physical file updated under commitment control must be journaled. The SQL table `CODELIVER1.TRADE_ORDERS` is stored as the native physical file `CODELIVER1/TRADE00001`.

One-time journal setup:

```bash
system "CRTJRNRCV JRNRCV(CODELIVER1/ORDRCV0001)"
system "CRTJRN JRN(CODELIVER1/ORDJRN) JRNRCV(CODELIVER1/ORDRCV0001)"
```

After every `CRTTABLES` reset, restart journaling on the recreated physical file:

```bash
system "STRJRNPF FILE(CODELIVER1/TRADE00001) JRN(CODELIVER1/ORDJRN)"
```

Then rerun `ORDRBATCH` and verify the exported table shows `PROC` rows.

### 11.6 Convert QPJOBLOG Spool to Readable Text in VS Code

Sometimes VS Code opens a `.splf` job log as raw spool data, which appears as
unreadable control characters. Convert the spool file to a normal UTF-8 IFS text
file before opening it in VS Code.

First identify the job and spool number in the VS Code Spooled File Browser or
with `WRKSPLF`. Example:

```text
File        : QPJOBLOG
Job         : 706057/CODELIVER/QPADEV0003
Spool number: 3
```

Create a temporary physical file for the spool text:

```bash
system "DLTF FILE(CODELIVER1/SPLTXT)"
system "CRTPF FILE(CODELIVER1/SPLTXT) RCDLEN(200)"
```

If `DLTF` says the file does not exist, continue.

Copy the spooled job log into the physical file. Replace the job number/name and
`SPLNBR` with the values from your spool entry:

```bash
system "CPYSPLF FILE(QPJOBLOG) TOFILE(CODELIVER1/SPLTXT) JOB(706057/CODELIVER/QPADEV0003) SPLNBR(3) MBROPT(*REPLACE) CTLCHAR(*NONE)"
```

Export the physical file to a UTF-8 text file in the IFS:

```bash
system "CPYTOIMPF FROMFILE(CODELIVER1/SPLTXT) TOSTMF('/home/CODELIVER/ordrbatch_joblog.txt') MBROPT(*REPLACE) STMFCCSID(1208) RCDDLM(*CRLF)"
```

Open this file from the VS Code IFS Browser:

```text
/home/CODELIVER/ordrbatch_joblog.txt
```

Then search for:

```text
ORDRBATCH
SQLCODE
SQLSTATE
CPF9898
ROLLBACK
```

To reset and re-run the batch:

```bash
system "STRSQL"
# UPDATE CODELIVER1.TRADE_ORDERS SET STATUS='PEND', PROCESS_DT=NULL
```

---

## 12. Unit, Integration, and System Tests (AAA Format)

The tests in this section follow **Arrange → Act → Assert** (AAA):

- `UTEST01` is an RPGLE unit test for the `PORTFSVC` service program.
- `ITEST01` is a CLLE integration test for `PORTFINQ`, `PORTFCBL`, and DB2 for i.
- `STEST01` is an SQLRPGLE system test for the end-to-end batch settlement flow.

Each test writes PASS / FAIL messages to the job log. If any assertion fails,
the test sends an escape message with `CPF9898`, so the failure is visible in
`DSPJOBLOG` instead of being hidden as ordinary diagnostic text.

Before running the system test, remember that `ORDRBATCH` was compiled with
`COMMIT(*CHG)` in section 11.3. On IBM i, that means `CODELIVER1.TRADE_ORDERS`
must be journaled before `ORDRBATCH` can update it under commitment control. If
you have not already done the section 11.5 journaling setup, do it before
running `STEST01`.

### 12.1 Unit Test: UTEST01 — Validate PORTFSVC Validation Logic

**Scope:** Tests the three `PORTFSVC` procedures in isolation: `validatePortfolio`, `validateCurrency`, and `formatPortfolioMsg`.

The promoted unit test no longer uses `DSPLY`. It calls a small CLLE logger (`LOGMSG`) for visible informational messages and raises `CPF9898` only if an assertion fails. This is closer to production behavior: quiet success is acceptable, but failure must be obvious in the job log and return path.

Create member `LOGMSG` in `QCLSRC`, type `CLLE`:

```cl
/* ============================================================ */
/* LOGMSG - Visible test/batch message logger                   */
/* Purpose: Send one informational message to the external queue */
/* Output : Visible from VS Code SSH and native DSPJOBLOG paths  */
/* ============================================================ */
PGM PARM(&MSG)

DCL VAR(&MSG) TYPE(*CHAR) LEN(256)

SNDPGMMSG MSG(&MSG) TOPGMQ(*EXT) MSGTYPE(*INFO)

ENDPGM
```

Create member `UTEST01` in `QRPGLESRC`, type `RPGLE`:

```rpgle
**FREE
// ============================================================
// UTEST01 - Unit Test: PORTFSVC validation procedures
// Pattern : Arrange -> Act -> Assert (AAA)
// Scope   : Unit - tests PORTFSVC in isolation, no DB access
// ============================================================
CTL-OPT DFTACTGRP(*NO) ACTGRP('JRAMSRV');

// PORTFSVC prototypes
DCL-PR validatePortfolio CHAR(2) EXTPROC('VALIDATEPORTFOLIO');
  pStatus     CHAR(1)      CONST;
  pTotalValue PACKED(15:2) CONST;
END-PR;

DCL-PR validateCurrency IND EXTPROC('VALIDATECURRENCY');
  pCurrency CHAR(3) CONST;
END-PR;

DCL-PR formatPortfolioMsg VARCHAR(100) EXTPROC('FORMATPORTFOLIOMSG');
  pPortfId    CHAR(10)     CONST;
  pOwner      CHAR(40)     CONST;
  pTotalValue PACKED(15:2) CONST;
  pCurrency   CHAR(3)      CONST;
END-PR;

// Job-log API for final failing escape message
DCL-PR QMHSNDPM EXTPGM('QMHSNDPM');
  pMsgId        CHAR(7)   CONST;
  pMsgFile      CHAR(20)  CONST;
  pMsgData      CHAR(256) CONST OPTIONS(*VARSIZE);
  pMsgDataLen   INT(10)   CONST;
  pMsgType      CHAR(10)  CONST;
  pCallStack    CHAR(10)  CONST;
  pCallStackCtr INT(10)   CONST;
  pMsgKey       CHAR(4);
  pErrorCode    CHAR(8)   CONST;
END-PR;

DCL-PR LogInfo;
  pText VARCHAR(256) CONST;
END-PR;

DCL-PR LOGMSG EXTPGM('LOGMSG');
  pText CHAR(256) CONST;
END-PR;

DCL-S wResult   CHAR(2);
DCL-S wIsValid  IND;
DCL-S wMsg      VARCHAR(100);
DCL-S wSummary  CHAR(256);
DCL-S wPassed   INT(10) INZ(0);
DCL-S wFailed   INT(10) INZ(0);
DCL-S wFailText CHAR(256);
DCL-S msgKey    CHAR(4);
DCL-S apiErr    CHAR(8) INZ(X'0000000000000000');

DCL-S uStatus   CHAR(1)      INZ('A');
DCL-S uValue    PACKED(15:2) INZ(150000.00);
DCL-S uCurrency CHAR(3)      INZ('USD');
DCL-S uPortfId  CHAR(10)     INZ('PF001     ');
DCL-S uOwner    CHAR(40)     INZ('Richard Papen');

// TC-U-01: Active portfolio with positive value -> '00'
// Arrange
uStatus = 'A';
uValue  = 150000.00;

// Act
wResult = validatePortfolio(uStatus : uValue);

// Assert
IF wResult = '00';
  LogInfo('TC-U-01 PASS');
  wPassed += 1;
ELSE;
  LogInfo('TC-U-01 FAIL');
  wFailed += 1;
ENDIF;

// TC-U-02: Inactive portfolio -> '10'
// Arrange
uStatus = 'I';
uValue  = 150000.00;

// Act
wResult = validatePortfolio(uStatus : uValue);

// Assert
IF wResult = '10';
  LogInfo('TC-U-02 PASS');
  wPassed += 1;
ELSE;
  LogInfo('TC-U-02 FAIL');
  wFailed += 1;
ENDIF;

// TC-U-03: Zero value -> '20'
// Arrange
uStatus = 'A';
uValue  = 0;

// Act
wResult = validatePortfolio(uStatus : uValue);

// Assert
IF wResult = '20';
  LogInfo('TC-U-03 PASS');
  wPassed += 1;
ELSE;
  LogInfo('TC-U-03 FAIL');
  wFailed += 1;
ENDIF;

// TC-U-04: Valid currency USD -> *ON
// Arrange
uCurrency = 'USD';

// Act
wIsValid = validateCurrency(uCurrency);

// Assert
IF wIsValid = *ON;
  LogInfo('TC-U-04 PASS');
  wPassed += 1;
ELSE;
  LogInfo('TC-U-04 FAIL');
  wFailed += 1;
ENDIF;

// TC-U-05: Invalid currency XXX -> *OFF
// Arrange
uCurrency = 'XXX';

// Act
wIsValid = validateCurrency(uCurrency);

// Assert
IF wIsValid = *OFF;
  LogInfo('TC-U-05 PASS');
  wPassed += 1;
ELSE;
  LogInfo('TC-U-05 FAIL');
  wFailed += 1;
ENDIF;

// TC-U-06: Format message contains key business fields
// Arrange
uPortfId  = 'PF001     ';
uOwner    = 'Richard Papen';
uValue    = 150000.00;
uCurrency = 'USD';

// Act
wMsg = formatPortfolioMsg(uPortfId : uOwner : uValue : uCurrency);

// Assert
IF %SCAN('PF001' : wMsg) > 0 AND
   %SCAN('Richard Papen' : wMsg) > 0 AND
   %SCAN('USD' : wMsg) > 0;
  LogInfo('TC-U-06 PASS');
  wPassed += 1;
ELSE;
  LogInfo('TC-U-06 FAIL');
  wFailed += 1;
ENDIF;

// Summary
wSummary = 'UTEST01 SUMMARY: PASS=' + %CHAR(wPassed)
         + ' FAIL=' + %CHAR(wFailed);
LogInfo(%TRIMR(wSummary));

IF wFailed > 0;
  wFailText = 'UTEST01 FAILED: ' + %CHAR(wFailed) + ' failed';
  QMHSNDPM('CPF9898'
          :'QCPFMSG   *LIBL     '
          :wFailText
          :%LEN(%TRIMR(wFailText))
          :'*ESCAPE   '
          :'*EXT      '
          :1
          :msgKey
          :apiErr);
ENDIF;

*INLR = *ON;
RETURN;

DCL-PROC LogInfo;
  DCL-PI *N;
    pText VARCHAR(256) CONST;
  END-PI;

  DCL-S msgText CHAR(256);
  msgText = %SUBST(pText : 1 : %MIN(%LEN(pText) : 256));

  LOGMSG(msgText);
END-PROC;
```

Compile and run from VS Code / SSH / PASE:

```bash
system "CRTBNDCL PGM(CODELIVER1/LOGMSG) SRCFILE(CODELIVER1/QCLSRC) SRCMBR(LOGMSG)"
system "CRTBNDRPG PGM(CODELIVER1/UTEST01) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(UTEST01) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
system -v "CALL PGM(CODELIVER1/UTEST01)"
echo UTEST01_RC:$?
```

Expected successful result:

```text
UTEST01_RC:0
```

5250 / SEU compile commands:

```cl
CRTBNDCL PGM(CODELIVER1/LOGMSG) +
  SRCFILE(CODELIVER1/QCLSRC) +
  SRCMBR(LOGMSG)

CRTBNDRPG PGM(CODELIVER1/UTEST01) +
  SRCFILE(CODELIVER1/QRPGLESRC) +
  SRCMBR(UTEST01) +
  DFTACTGRP(*NO) +
  ACTGRP(JRAMSRV) +
  BNDDIR(CODELIVER1/PORTFBNDD)
```

### 12.2 Integration Test: ITEST01 — PORTFINQ + DB2 Round-Trip

**Scope:** Tests the real DB2 table plus both inquiry programs:

- `PORTFINQ` RPGLE inquiry against `CODELIVER1.PORTFOLIO`
- `PORTFCBL` COBOL/400 inquiry against the same table
- Quiet success handling plus failure / negative-path job-log evidence

Create member `ITEST01` in `QCLSRC`, type `CLLE`. Paste the source so `PGM`,
`DCL`, `CHGVAR`, `CALL`, and `ENDPGM` start in column 1 of the source member:

```cl
/* ======================================================== */
/* ITEST01 - Integration Test: PORTFINQ + PORTFCBL + DB2    */
/* Pattern : Arrange -> Act -> Assert (AAA)                 */
/* Output  : Production-style job-log evidence              */
/* ======================================================== */
PGM

DCL VAR(&PORTFID)  TYPE(*CHAR) LEN(10)
DCL VAR(&OWNER)    TYPE(*CHAR) LEN(40)
DCL VAR(&TOTVAL)   TYPE(*DEC)  LEN(15 2) VALUE(0)
DCL VAR(&CURRENCY) TYPE(*CHAR) LEN(3)
DCL VAR(&RETCODE)  TYPE(*CHAR) LEN(2)
DCL VAR(&MSG)      TYPE(*CHAR) LEN(100)
DCL VAR(&PASSED)   TYPE(*DEC)  LEN(3 0) VALUE(0)
DCL VAR(&FAILED)   TYPE(*DEC)  LEN(3 0) VALUE(0)

/* TC-I-01: PORTFINQ known PF001 */

/* Arrange */
CHGVAR VAR(&PORTFID)  VALUE('PF001     ')
CHGVAR VAR(&OWNER)    VALUE('                                        ')
CHGVAR VAR(&TOTVAL)   VALUE(0)
CHGVAR VAR(&CURRENCY) VALUE('   ')
CHGVAR VAR(&RETCODE)  VALUE('  ')

/* Act */
CALL PGM(CODELIVER1/PORTFINQ) +
     PARM(&PORTFID &OWNER &TOTVAL &CURRENCY &RETCODE)

/* Assert */
IF COND(&RETCODE *EQ '00') THEN(DO)
  CHGVAR VAR(&PASSED) VALUE(&PASSED + 1)
ENDDO
ELSE CMD(DO)
  SNDPGMMSG MSG('TC-I-01 FAIL: PORTFINQ PF001')
  CHGVAR VAR(&MSG) VALUE('RET=' *CAT &RETCODE)
  CHGVAR VAR(&MSG) VALUE(&MSG *BCAT 'OWNER=' *CAT &OWNER)
  SNDPGMMSG MSG(&MSG)
  CHGVAR VAR(&FAILED) VALUE(&FAILED + 1)
ENDDO

/* TC-I-02: PORTFINQ unknown XXXXX */

/* Arrange */
CHGVAR VAR(&PORTFID)  VALUE('XXXXX     ')
CHGVAR VAR(&OWNER)    VALUE('                                        ')
CHGVAR VAR(&TOTVAL)   VALUE(999999.99)
CHGVAR VAR(&CURRENCY) VALUE('USD')
CHGVAR VAR(&RETCODE)  VALUE('  ')

/* Act */
CALL PGM(CODELIVER1/PORTFINQ) +
     PARM(&PORTFID &OWNER &TOTVAL &CURRENCY &RETCODE)

/* Assert */
IF COND(&RETCODE *EQ '10') THEN(DO)
  SNDPGMMSG MSG('TC-I-02 NEGATIVE PATH: XXXXX not found')
  CHGVAR VAR(&PASSED) VALUE(&PASSED + 1)
ENDDO
ELSE CMD(DO)
  SNDPGMMSG MSG('TC-I-02 FAIL: expected not-found retcode 10')
  CHGVAR VAR(&MSG) VALUE('RET=' *CAT &RETCODE)
  CHGVAR VAR(&MSG) VALUE(&MSG *BCAT 'OWNER=' *CAT &OWNER)
  SNDPGMMSG MSG(&MSG)
  CHGVAR VAR(&FAILED) VALUE(&FAILED + 1)
ENDDO

/* TC-I-03: PORTFCBL known PF002 */

/* Arrange */
CHGVAR VAR(&PORTFID)  VALUE('PF002     ')
CHGVAR VAR(&OWNER)    VALUE('                                        ')
CHGVAR VAR(&TOTVAL)   VALUE(0)
CHGVAR VAR(&CURRENCY) VALUE('   ')
CHGVAR VAR(&RETCODE)  VALUE('  ')

/* Act */
CALL PGM(CODELIVER1/PORTFCBL) +
     PARM(&PORTFID &OWNER &TOTVAL &CURRENCY &RETCODE)

/* Assert */
IF COND(&RETCODE *EQ '00') THEN(DO)
  CHGVAR VAR(&PASSED) VALUE(&PASSED + 1)
ENDDO
ELSE CMD(DO)
  SNDPGMMSG MSG('TC-I-03 FAIL: PORTFCBL PF002')
  CHGVAR VAR(&MSG) VALUE('RET=' *CAT &RETCODE)
  CHGVAR VAR(&MSG) VALUE(&MSG *BCAT 'OWNER=' *CAT &OWNER)
  SNDPGMMSG MSG(&MSG)
  CHGVAR VAR(&FAILED) VALUE(&FAILED + 1)
ENDDO

/* Summary */
CHGVAR VAR(&MSG) VALUE('ITEST01 PASS=' *CAT %CHAR(&PASSED))
SNDPGMMSG MSG(&MSG)
CHGVAR VAR(&MSG) VALUE('ITEST01 FAIL=' *CAT %CHAR(&FAILED))
SNDPGMMSG MSG(&MSG)

ENDPGM
```

Compile and run from VS Code / SSH / PASE:

```bash
system "CRTBNDCL PGM(CODELIVER1/ITEST01) SRCFILE(CODELIVER1/QCLSRC) SRCMBR(ITEST01)"
system -v "CALL PGM(CODELIVER1/ITEST01)"
echo ITEST01_RC:$?
```

Expected successful result:

```text
ITEST01 PASS=3
ITEST01 FAIL=0
ITEST01_RC:0
```

5250 / SEU compile command:

```cl
CRTBNDCL PGM(CODELIVER1/ITEST01) +
  SRCFILE(CODELIVER1/QCLSRC) +
  SRCMBR(ITEST01)
```

### 12.3 System Test: STEST01 — End-to-End Batch Settlement

**Scope:** Tests the full business flow automatically:

- reset all orders to `PEND`
- run `ORDRBATCH`
- assert that no `PEND` orders remain
- assert that every processed order has `PROCESS_DT` populated

This is a system test because it exercises DB2 for i plus the batch RPGLE program from end to end. The promoted version calls `ORDRBATCH` directly with an external program prototype. Do not use `QCMDEXC` here; the direct call path is the version that compiled and completed during final verification.

For `STEST01`, AAA is applied at the business-scenario level:

- Arrange: reset `TRADE_ORDERS` to a known `PEND` state.
- Act: call `CODELIVER1/ORDRBATCH`.
- Assert: verify no `PEND` rows remain and every order has `PROCESS_DT`.

Create member `STEST01` in `QRPGLESRC`, type `SQLRPGLE`:

```rpgle
**FREE
// ============================================================
// STEST01 - System Test: End-to-End Batch Settlement
// Pattern : Arrange -> Act -> Assert (AAA)
// Scope   : System - exercises DB2 + ORDRBATCH together
// Business: Models the nightly settlement run in Olympic
// ============================================================
CTL-OPT DFTACTGRP(*NO) ACTGRP(*NEW);

EXEC SQL SET OPTION COMMIT = *NONE, NAMING = *SQL, CLOSQLCSR = *ENDMOD;

// Direct call to the batch program under test.
DCL-PR ORDRBATCH EXTPGM('ORDRBATCH');
END-PR;

// IBM i job-log API. CPF9898 lets the test emit visible PASS / FAIL text.
DCL-PR QMHSNDPM EXTPGM('QMHSNDPM');
  pMsgId        CHAR(7)   CONST;
  pMsgFile      CHAR(20)  CONST;
  pMsgData      CHAR(512) CONST OPTIONS(*VARSIZE);
  pMsgDataLen   INT(10)   CONST;
  pMsgType      CHAR(10)  CONST;
  pCallStack    CHAR(10)  CONST;
  pCallStackCtr INT(10)   CONST;
  pMsgKey       CHAR(4);
  pErrorCode    CHAR(8)   CONST;
END-PR;

DCL-PR LogInfo;
  pText VARCHAR(512) CONST;
END-PR;

DCL-PR RecordFail;
  pText VARCHAR(512) CONST;
END-PR;

DCL-S wRows         INT(10) INZ(0);
DCL-S wPendingBefore INT(10) INZ(0);
DCL-S wPendingAfter  INT(10) INZ(0);
DCL-S wBadRows       INT(10) INZ(0);
DCL-S wPassed        INT(10) INZ(0);
DCL-S wFailed        INT(10) INZ(0);
DCL-S wSummary       CHAR(512);
DCL-S msgKey         CHAR(4);
DCL-S apiErr         CHAR(8) INZ(X'0000000000000000');

// -- TC-S-01: Reset all orders to PEND -----------------------
// Arrange - put the database in a known state.
LogInfo('TC-S-01 ARRANGE: resetting TRADE_ORDERS to PEND');

EXEC SQL
  UPDATE CODELIVER1.TRADE_ORDERS
     SET STATUS = 'PEND',
         PROCESS_DT = NULL;

IF SQLCODE < 0;
  RecordFail('TC-S-01 FAIL: reset update failed. SQLCODE=' + %CHAR(SQLCODE));
ELSE;
  EXEC SQL GET DIAGNOSTICS :wRows = ROW_COUNT;

  // Assert - confirm the arrange step produced rows for ORDRBATCH.
  EXEC SQL
    SELECT COUNT(*)
      INTO :wPendingBefore
      FROM CODELIVER1.TRADE_ORDERS
     WHERE STATUS = 'PEND';

  IF SQLCODE = 0 AND wPendingBefore > 0;
    LogInfo('TC-S-01 PASS: pending rows before batch='
            + %CHAR(wPendingBefore));
    wPassed += 1;
  ELSE;
    RecordFail('TC-S-01 FAIL: no PEND rows available for batch test');
  ENDIF;
ENDIF;

// -- TC-S-02: Run the batch processor ------------------------
// Act
IF wFailed = 0;
  LogInfo('TC-S-02 ACT: calling CODELIVER1/ORDRBATCH');

  MONITOR;
    ORDRBATCH();
    LogInfo('TC-S-02 PASS: ORDRBATCH completed without escape message');
    wPassed += 1;
  ON-ERROR;
    RecordFail('TC-S-02 FAIL: ORDRBATCH raised an escape message');
  ENDMON;
ENDIF;

// -- TC-S-03: Verify all orders are now PROC -----------------
// Assert - check the result in DB2.
IF wFailed = 0;
  EXEC SQL
    SELECT COUNT(*)
      INTO :wPendingAfter
      FROM CODELIVER1.TRADE_ORDERS
     WHERE STATUS = 'PEND';

  IF SQLCODE < 0;
    RecordFail('TC-S-03 FAIL: pending-count query failed. SQLCODE='
               + %CHAR(SQLCODE));
  ELSEIF wPendingAfter = 0;
    LogInfo('TC-S-03 PASS: zero PEND rows remain');
    wPassed += 1;
  ELSE;
    RecordFail('TC-S-03 FAIL: PEND rows remain=' + %CHAR(wPendingAfter));
  ENDIF;
ENDIF;

// -- TC-S-04: Verify processed rows have process dates -------
// Assert - all completed rows must have the batch processing date.
IF wFailed = 0;
  EXEC SQL
    SELECT COUNT(*)
      INTO :wBadRows
      FROM CODELIVER1.TRADE_ORDERS
     WHERE STATUS <> 'PROC'
        OR PROCESS_DT IS NULL;

  IF SQLCODE < 0;
    RecordFail('TC-S-04 FAIL: final status query failed. SQLCODE='
               + %CHAR(SQLCODE));
  ELSEIF wBadRows = 0;
    LogInfo('TC-S-04 PASS: all orders are PROC with PROCESS_DT populated');
    wPassed += 1;
  ELSE;
    RecordFail('TC-S-04 FAIL: non-PROC or missing-date rows='
               + %CHAR(wBadRows));
  ENDIF;
ENDIF;

// -- Summary -------------------------------------------------
wSummary = 'SYSTEM TEST SUMMARY: '
         + %CHAR(wPassed) + ' passed, '
         + %CHAR(wFailed) + ' failed';
LogInfo(%TRIMR(wSummary));

IF wFailed > 0;
  QMHSNDPM('CPF9898'
          :'QCPFMSG   *LIBL     '
          :wSummary
          :%LEN(%TRIMR(wSummary))
          :'*ESCAPE   '
          :'*EXT      '
          :0
          :msgKey
          :apiErr);
ENDIF;

*INLR = *ON;
RETURN;

DCL-PROC LogInfo;
  DCL-PI *N;
    pText VARCHAR(512) CONST;
  END-PI;

  // Informational logging is intentionally quiet in batch/PASE runs.
END-PROC;

DCL-PROC RecordFail;
  DCL-PI *N;
    pText VARCHAR(512) CONST;
  END-PI;

  LogInfo(pText);
  wFailed += 1;
END-PROC;
```

Compile and run from VS Code / SSH / PASE. If you reset tables with `CRTTABLES`, restart journaling before this call:

```bash
system "CRTSQLRPGI OBJ(CODELIVER1/STEST01) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(STEST01) OBJTYPE(*PGM) COMMIT(*NONE) CLOSQLCSR(*ENDMOD)"
system "RUNSQLSTM SRCFILE(CODELIVER1/QSQLSRC) SRCMBR(CRTTABLES) COMMIT(*NONE) NAMING(*SQL) DECMPT(*PERIOD)"
system "STRJRNPF FILE(CODELIVER1/TRADE00001) JRN(CODELIVER1/ORDJRN)"
system -v "CALL PGM(CODELIVER1/STEST01)"
echo STEST01_RC:$?
```

Expected successful result:

```text
STEST01_RC:0
```

5250 / SEU compile command:

```cl
CRTSQLRPGI OBJ(CODELIVER1/STEST01) +
  SRCFILE(CODELIVER1/QRPGLESRC) +
  SRCMBR(STEST01) +
  OBJTYPE(*PGM) +
  COMMIT(*NONE) +
  CLOSQLCSR(*ENDMOD)
```

---

## 13. Verification Checklist

Use VS Code / SSH / PASE for compile, run, return-code capture, and UTF-8 table exports. Use the native 5250 screen for optional spool/job-log inspection. Do not rely on PASE `system "DSPJOBLOG"` after a separate `system "CALL ..."`; those commands can run under different IBM i jobs, so the job log may not be the program run you intended to inspect.

Also avoid opening `.splf` files directly in VS Code when they display raw control data. For VS Code verification, prefer `system -v` return codes plus `CPYTOIMPF` table exports. For spool evidence, use native 5250 `WRKSPLF` and option `5`.

### 13.1 Compile from VS Code / SSH / PASE

Run these after SSH login to PUB400:

```bash
system "RUNSQLSTM SRCFILE(CODELIVER1/QSQLSRC) SRCMBR(CRTTABLES) COMMIT(*NONE) NAMING(*SQL) DECMPT(*PERIOD)"
system "CRTJRNRCV JRNRCV(CODELIVER1/ORDRCV0001)"
system "CRTJRN JRN(CODELIVER1/ORDJRN) JRNRCV(CODELIVER1/ORDRCV0001)"
system "STRJRNPF FILE(CODELIVER1/TRADE00001) JRN(CODELIVER1/ORDJRN)"

system "CRTSQLRPGI OBJ(CODELIVER1/PORTFINQ) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFINQ) OBJTYPE(*PGM) COMMIT(*NONE) CLOSQLCSR(*ENDMOD)"
system "CRTBNDCBL PGM(CODELIVER1/PORTFCBL) SRCFILE(CODELIVER1/QCBLLESRC) SRCMBR(PORTFCBL)"
system "CRTBNDCL PGM(CODELIVER1/ORDPROC) SRCFILE(CODELIVER1/QCLSRC) SRCMBR(ORDPROC)"
system "CRTRPGMOD MODULE(CODELIVER1/PORTFSVC) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFSVC)"
system "CRTSRVPGM SRVPGM(CODELIVER1/PORTFSVC) MODULE(CODELIVER1/PORTFSVC) EXPORT(*SRCFILE) SRCFILE(CODELIVER1/QSRVSRC) SRCMBR(PORTFSVC)"
system "CRTBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)"
system "ADDBNDDIRE BNDDIR(CODELIVER1/PORTFBNDD) OBJ((CODELIVER1/PORTFSVC *SRVPGM))"
system "CRTBNDRPG PGM(CODELIVER1/PORTFTEST) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFTEST) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
system "CRTSQLRPGI OBJ(CODELIVER1/ORDRBATCH) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(ORDRBATCH) OBJTYPE(*PGM) COMMIT(*CHG) CLOSQLCSR(*ENDMOD)"
system "CRTBNDCL PGM(CODELIVER1/LOGMSG) SRCFILE(CODELIVER1/QCLSRC) SRCMBR(LOGMSG)"
system "CRTBNDRPG PGM(CODELIVER1/UTEST01) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(UTEST01) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
system "CRTBNDCL PGM(CODELIVER1/ITEST01) SRCFILE(CODELIVER1/QCLSRC) SRCMBR(ITEST01)"
system "CRTSQLRPGI OBJ(CODELIVER1/STEST01) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(STEST01) OBJTYPE(*PGM) COMMIT(*NONE) CLOSQLCSR(*ENDMOD)"
```

If `CRTJRNRCV` or `CRTJRN` says the object already exists, continue. If `RUNSQLSTM` is rerun later, run `STRJRNPF` again because the table physical file was recreated.

### 13.2 Run and Verify from VS Code / SSH / PASE

Use `system -v` so the shell receives the IBM i command return status:

```bash
system -v "CALL PGM(CODELIVER1/UTEST01)"
echo UTEST01_RC:$?

system -v "CALL PGM(CODELIVER1/ITEST01)"
echo ITEST01_RC:$?

system -v "CALL PGM(CODELIVER1/STEST01)"
echo STEST01_RC:$?
```

Export the final table state to readable UTF-8 text:

```bash
system "CPYTOIMPF FROMFILE(CODELIVER1/TRADE00001) TOSTMF('/home/CODELIVER/final_trade_orders_vfy.csv') MBROPT(*REPLACE) STMFCCSID(1208) RCDDLM(*CRLF) DTAFMT(*DLM)"
cat /home/CODELIVER/final_trade_orders_vfy.csv
```

Final verified evidence from PUB400 on 2026-06-17:

```text
UTEST01_RC:0
ITEST01 PASS=3
ITEST01 FAIL=0
ITEST01_RC:0
STEST01_RC:0
TRADE_ORDERS: all 3 seeded rows had STATUS="PROC" and PROCESS_DT="2026-06-17"
```

The final exported rows were:

```text
"ORD-2026-001        ","PF001     ","TSH000000001",100.0000,182.5000,"2026-06-17","2026-06-17","PROC"
"ORD-2026-002        ","PF001     ","TSH000000002",50.0000,312.0000,"2026-06-17","2026-06-17","PROC"
"ORD-2026-003        ","PF002     ","TSH000000003",200.0000,45.7500,"2026-06-17","2026-06-17","PROC"
```

### 13.3 Native 5250 Verification

Use native 5250 for commands that are awkward or blocked in PASE, such as `WRKOBJ`, `WRKSPLF`, and interactive job-log display.

Object checks:

```cl
WRKOBJ OBJ(CODELIVER1/PORTFINQ) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/PORTFCBL) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/ORDPROC) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/PORTFSVC) OBJTYPE(*MODULE *SRVPGM)
WRKOBJ OBJ(CODELIVER1/PORTFTEST) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/ORDRBATCH) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/LOGMSG) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/UTEST01) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/ITEST01) OBJTYPE(*PGM)
WRKOBJ OBJ(CODELIVER1/STEST01) OBJTYPE(*PGM)
DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)
DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)
```

Native test run and inspection:

```cl
CALL PGM(CODELIVER1/UTEST01)
DSPJOBLOG

CALL PGM(CODELIVER1/ITEST01)
DSPJOBLOG

CALL PGM(CODELIVER1/STEST01)
DSPJOBLOG
```

For retained spool files:

```cl
WRKSPLF SELECT(CODELIVER)
```

Look for the newest `QPJOBLOG` spool files for `UTEST01`, `ITEST01`, and `STEST01`, then use option `5` to display them. If the VS Code Spooled File Browser opens unreadable control data, switch to native 5250 or export with `CPYSPLF` followed by `CPYTOIMPF` as shown in section 11.6.

### 13.4 Expected Object List

| Object | Type | Description |
|--------|------|-------------|
| `PORTFOLIO` | `*FILE (PF)` | Master portfolio physical file |
| `TRADE_ORDERS` | `*FILE (PF)` | Trade orders physical file |
| `PORTFINQ` | `*PGM` | RPGLE portfolio inquiry |
| `PORTFCBL` | `*PGM` | COBOL/400 portfolio inquiry |
| `ORDPROC` | `*PGM` | CL driver |
| `PORTFOUT` | `*FILE (PF)` | Persistent visible output for PORTFTEST |
| `PORTFSVC` | `*MODULE` | Compiled module used to create service program |
| `PORTFSVC` | `*SRVPGM` | ILE service program |
| `PORTFBNDD` | `*BNDDIR` | Binding directory containing PORTFSVC |
| `PORTFTEST` | `*PGM` | Service program caller |
| `ORDRBATCH` | `*PGM` | Batch order processor |
| `LOGMSG` | `*PGM` | CLLE informational logger used by UTEST01 |
| `UTEST01` | `*PGM` | Unit test - PORTFSVC validation |
| `ITEST01` | `*PGM` | Integration test - DB2 round-trip |
| `STEST01` | `*PGM` | System test - batch settlement |

---

## 14. Storing Source in the Git Repository

IBM i source members live on PUB400, but their promoted plain-text equivalents now live in this repo under `src/ibmi/`. Treat these as source code, not detached documentation snippets: edits should be committed, pushed, compiled on PUB400, and verified through the section 13 flow.

### 14.1 Directory Layout in the Repo

```text
as400-ibmi-batch-simulator/
└── src/
    └── ibmi/
        ├── sql/
        │   ├── CRTTABLES.sql
        │   └── SELECT01.sql
        ├── sqlrpgle/
        │   ├── PORTFINQ.sqlrpgle
        │   ├── ORDRBATCH.sqlrpgle
        │   └── STEST01.sqlrpgle
        ├── rpgle/
        │   ├── PORTFSVC.rpgle
        │   ├── PORTFTEST.rpgle
        │   └── UTEST01.rpgle
        ├── include/
        │   └── PORTFSVCPR.rpgleinc
        ├── srvsrc/
        │   └── PORTFSVC.bnd
        ├── cobol/
        │   └── PORTFCBL.cbl
        └── clle/
            ├── ORDPROC.clle
            ├── LOGMSG.clle
            └── ITEST01.clle
```

### 14.2 Save and Push from VS Code

In the local repo:

```bash
cd "/Users/oliverjaramillo/Local Documents/Asimov1689/as400-ibmi-batch-simulator"
git status --short
git add src/ibmi docs/DOC1_Layer1_IBMi_Native_Development.md README.md
git commit -m "Promote IBM i source and verification docs"
git push origin main
```

Each source file should keep a short header explaining what it is, the IBM i concept it demonstrates, and the z/OS equivalent where relevant. The authoritative source path for promoted IBM i components is `src/ibmi/`.

---

*End of DOC 1 — Layer 1 & Layer 2: IBM i Native Development*  
*Next: DOC 2 — Layer 3 & Layer 4: Java Spring Boot + JT400 REST API*
