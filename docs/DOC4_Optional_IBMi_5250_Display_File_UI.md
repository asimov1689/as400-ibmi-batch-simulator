# DOC 4 — Optional IBM i 5250 Display File UI
## Native Green-Screen Screen for PORTFSVC Validation
### Optional time-boxed extension after DOC 1
### Platform: PUB400 · IDE: VS Code + ACS 5250 · Language: DDS + RPGLE

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [What You Are Building](#2-what-you-are-building)
3. [When To Use This DOC](#3-when-to-use-this-doc)
4. [Create the Display File Source Member](#4-create-the-display-file-source-member)
5. [Compile the Display File](#5-compile-the-display-file)
6. [Create the PORTFUI RPGLE Program](#6-create-the-portfui-rpgle-program)
7. [Compile PORTFUI](#7-compile-portfui)
8. [Run the Native IBM i Screen](#8-run-the-native-ibm-i-screen)
9. [Push Source to GitHub](#9-push-source-to-github)
10. [How to Demonstrate This Skill](#10-how-to-demonstrate-this-skill)
11. [Interview Explanation](#11-interview-explanation)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Purpose

DOC 1 proves the native IBM i service program flow:

```text
PORTFTEST -> PORTFSVC -> exported validation procedures
```

It uses `PORTFOUT` and optional `QPRINT` because those give stable evidence you
can inspect with `DSPPFM`, native spool commands, and the VS Code Spooled File
Browser.

This DOC 4 is optional. It adds a small 5250 green-screen UI so you can also
show native IBM i display-file development if there is time.

---

## 2. What You Are Building

```text
PORTFDSPL  *FILE DSPF   Display file with one validation screen
PORTFUI    *PGM         RPGLE workstation program using PORTFDSPL
PORTFSVC   *SRVPGM      Existing service program from DOC 1
```

The screen accepts:

```text
Portfolio ID
Owner
Status
Currency
Total value
```

Then `PORTFUI` calls the same `PORTFSVC` procedures used by `PORTFTEST`:

```text
VALIDATEPORTFOLIO
VALIDATECURRENCY
FORMATPORTFOLIOMSG
```

The result is displayed on the 5250 screen.

---

## 3. When To Use This DOC

Use DOC 4 only after DOC 1 is working.

Minimum prerequisites:

```text
CODELIVER1/PORTFSVC   *SRVPGM exists
CODELIVER1/PORTFBNDD  *BNDDIR contains PORTFSVC
DOC 1 PORTFTEST works with PORTFOUT or QPRINT
```

Do not use this DOC to debug `PORTFSVC`. First prove `PORTFSVC` with DOC 1.

---

## 4. Create the Display File Source Member

In VS Code IBM i panel:

```text
CODELIVER1/QDDSSRC -> New Member
Name: PORTFDSPL
Type: DSPF
```

Paste:

```dds
     A                                      DSPSIZ(24 80 *DS3)
     A                                      CA03(03 'Exit')
     A          R MAIN
     A                                  1  2'Portfolio Validation'
     A                                  2  2'F3=Exit'
     A                                  4  2'Portfolio ID . . . . . .'
     A            PORTFID       10A  B  4 30
     A                                  5  2'Owner . . . . . . . . . .'
     A            OWNER         40A  B  5 30
     A                                  6  2'Status A/I . . . . . . .'
     A            STATUS         1A  B  6 30
     A                                  7  2'Currency . . . . . . . .'
     A            CURRENCY       3A  B  7 30
     A                                  8  2'Total Value . . . . . .'
     A            VALUEIN       15A  B  8 30
     A                                 10  2'Validation Result'
     A            RESULT        70A  O 11  2
```

Notes:

- `B` fields are both input and output.
- `O` field is output only.
- `CA03` lets the user exit with F3.

---

## 5. Compile the Display File

From VS Code terminal:

```bash
system "CRTDSPF FILE(CODELIVER1/PORTFDSPL) SRCFILE(CODELIVER1/QDDSSRC) SRCMBR(PORTFDSPL)"
```

5250 command-line form:

```cl
CRTDSPF FILE(CODELIVER1/PORTFDSPL) +
  SRCFILE(CODELIVER1/QDDSSRC) +
  SRCMBR(PORTFDSPL)
```

Verify:

```bash
system "WRKOBJ OBJ(CODELIVER1/PORTFDSPL) OBJTYPE(*FILE)"
```

Expected:

```text
PORTFDSPL   *FILE
```

---

## 6. Create the PORTFUI RPGLE Program

In VS Code IBM i panel:

```text
CODELIVER1/QRPGLESRC -> New Member
Name: PORTFUI
Type: RPGLE
```

Paste:

```rpgle
**FREE
// ============================================================
// PORTFUI - Optional 5250 display-file UI for PORTFSVC
// IBM i    : WORKSTN file, EXFMT, service-program call
// z/OS eq  : CICS-like screen flow + business service call
// ============================================================

CTL-OPT DFTACTGRP(*NO) ACTGRP('JRAMSRV');

DCL-F PORTFDSPL WORKSTN;

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

DCL-S retCode CHAR(2);
DCL-S currOk  IND;
DCL-S msg     VARCHAR(100);
DCL-S totalValue PACKED(15:2);

PORTFID = 'PF001';
OWNER = 'Richard Papen';
STATUS = 'A';
CURRENCY = 'USD';
VALUEIN = '150000.00';
RESULT = 'Press Enter to validate, or F3 to exit.';

DOW *IN03 = *OFF;
  EXFMT MAIN;

  IF *IN03 = *ON;
    LEAVE;
  ENDIF;

  totalValue = %DEC(%TRIM(VALUEIN) : 15 : 2);

  retCode = validatePortfolio(STATUS : totalValue);
  currOk = validateCurrency(CURRENCY);

  IF retCode <> '00';
    SELECT;
      WHEN retCode = '10';
        RESULT = 'FAIL: Portfolio status is inactive.';
      WHEN retCode = '20';
        RESULT = 'FAIL: Portfolio value is zero or negative.';
      OTHER;
        RESULT = 'FAIL: Unexpected portfolio return code ' + retCode;
    ENDSL;
  ELSEIF currOk <> *ON;
    RESULT = 'FAIL: Currency is not valid.';
  ELSE;
    msg = formatPortfolioMsg(PORTFID : OWNER : totalValue : CURRENCY);
    RESULT = 'PASS: ' + %TRIM(msg);
  ENDIF;
ENDDO;

*INLR = *ON;
RETURN;
```

---

## 7. Compile PORTFUI

From VS Code terminal:

```bash
system "CRTBNDRPG PGM(CODELIVER1/PORTFUI) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFUI) DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)"
```

5250 command-line form:

```cl
CRTBNDRPG PGM(CODELIVER1/PORTFUI) +
  SRCFILE(CODELIVER1/QRPGLESRC) +
  SRCMBR(PORTFUI) +
  DFTACTGRP(*NO) +
  ACTGRP(JRAMSRV) +
  BNDDIR(CODELIVER1/PORTFBNDD)
```

Verify:

```bash
system "WRKOBJ OBJ(CODELIVER1/PORTFUI) OBJTYPE(*PGM)"
```

---

## 8. Run the Native IBM i Screen

Use a 5250 green-screen session. Do not run this from PASE bash.

```cl
CALL PGM(CODELIVER1/PORTFUI)
```

Expected flow:

1. The `Portfolio Validation` screen opens.
2. Default test values are pre-filled.
3. Press `Enter`.
4. The result line updates with a `PASS:` or `FAIL:` message.
5. Press `F3` to exit.

Expected successful result:

```text
PASS: PF001 | Richard Papen | USD 150000.00
```

---

## 9. Push Source to GitHub

IBM i source members live on the server. To push them to GitHub you copy the
content locally, commit, and push. Do this after PORTFDSPL and PORTFUI compile
successfully on PUB400.

### 9.1 Create the repository on GitHub

Go to github.com → **New repository**.

```text
Repository name : ibmi-5250-portfolio-screen-validator
Description     : Native IBM i (AS/400) 5250 green-screen portfolio validator
                  built with DDS display files and free-format RPGLE, calling a
                  reusable service program (PORTFSVC) for business-rule validation.
Visibility      : Public
Initialize      : Add a README (check the box)
```

Clone it locally:

```bash
git clone https://github.com/<your-username>/ibmi-5250-portfolio-screen-validator.git
cd ibmi-5250-portfolio-screen-validator
```

### 9.2 Local folder structure

Mirror the IBM i library/source-file layout:

```text
ibmi-5250-portfolio-screen-validator/
├── README.md
├── src/
│   ├── QDDSSRC/
│   │   └── PORTFDSPL.dspf      ← display file DDS
│   └── QRPGLESRC/
│       ├── PORTFUI.rpgle        ← workstation program
│       ├── PORTFSVC.rpgle       ← service program procedures
│       └── PORTFSVCB.rpgle      ← binding source (if applicable)
```

Create the folders:

```bash
mkdir -p src/QDDSSRC src/QRPGLESRC
```

### 9.3 Copy source from VS Code IBM i panel

In VS Code, open the IBM i Source Browser:

```text
CODELIVER1 / QDDSSRC / PORTFDSPL  → right-click → Download Member
  Save to: src/QDDSSRC/PORTFDSPL.dspf

CODELIVER1 / QRPGLESRC / PORTFUI  → right-click → Download Member
  Save to: src/QRPGLESRC/PORTFUI.rpgle
```

Alternatively, open each member, select all (`Cmd+A`), copy, paste into a new
local file saved at the paths above.

### 9.4 Add a README

Replace the generated README.md with content that explains what the repo
demonstrates:

```markdown
# ibmi-5250-portfolio-screen-validator

A native IBM i (AS/400) portfolio validation application built with:

- **DDS** — defines the 5250 green-screen display file (`PORTFDSPL`)
- **Free-format RPGLE** — workstation program (`PORTFUI`) drives the screen
- **RPGLE Service Program** — `PORTFSVC` contains the reusable business rules
  (`VALIDATEPORTFOLIO`, `VALIDATECURRENCY`, `FORMATPORTFOLIOMSG`)

## What it does

Presents a 5250 screen where an operator enters portfolio data (ID, owner,
status, currency, total value). On Enter, the program calls the `PORTFSVC`
service program to validate the data and displays a PASS or FAIL result on
the same screen. F3 exits.

## Tech stack

| Layer | Technology |
|-------|-----------|
| Platform | IBM i (AS/400) on PUB400.com |
| UI | 5250 green screen — DDS display file |
| Program | Free-format ILE RPGLE (DFTACTGRP *NO) |
| Business rules | ILE Service Program (PORTFSVC) |
| IDE | VS Code + IBM i extension (Code for IBM i) |
| Terminal | IBM ACS 5250 emulator |

## Structure

\`\`\`
src/QDDSSRC/PORTFDSPL.dspf    ← display file — screen layout and field definitions
src/QRPGLESRC/PORTFUI.rpgle   ← workstation program — EXFMT loop, calls PORTFSVC
\`\`\`

## Running on IBM i

Compile and call from a 5250 session (ACS):

\`\`\`cl
CRTDSPF FILE(CODELIVER1/PORTFDSPL) SRCFILE(CODELIVER1/QDDSSRC) SRCMBR(PORTFDSPL)
CRTBNDRPG PGM(CODELIVER1/PORTFUI) SRCFILE(CODELIVER1/QRPGLESRC) SRCMBR(PORTFUI) +
  DFTACTGRP(*NO) ACTGRP(JRAMSRV) BNDDIR(CODELIVER1/PORTFBNDD)
CALL PGM(CODELIVER1/PORTFUI)
\`\`\`
```

### 9.5 Commit and push

```bash
git add src/ README.md
git commit -m "Add PORTFDSPL display file and PORTFUI workstation program

Native IBM i 5250 portfolio validation screen.
DDS display file + free-format RPGLE calling PORTFSVC service program."

git push origin main
```

Your live repo URL to share: `https://github.com/<your-username>/ibmi-5250-portfolio-screen-validator`

---

## 10. How to Demonstrate This Skill

Use this sequence in an interview or screen-share. Keep it under 8 minutes.

### Step 1 — Open the GitHub repo (1 min)

Open `https://github.com/<your-username>/ibmi-5250-portfolio-screen-validator`
in a browser tab.

Say:

> "I'll start with the repo so you can see the source structure before we run it.
> The DDS display file defines the screen layout — field positions, input/output
> types, and the F3 key. The RPGLE program owns the screen loop and delegates
> all business logic to the PORTFSVC service program."

Point to `src/QDDSSRC/PORTFDSPL.dspf` — show the `CA03`, `B` fields, and `O`
field. Point to `src/QRPGLESRC/PORTFUI.rpgle` — show the `EXFMT MAIN` loop and
the three `DCL-PR` prototypes.

### Step 2 — Open VS Code + IBM i panel (2 min)

Open VS Code and show the IBM i Source Browser:

```text
CODELIVER1 / QDDSSRC  / PORTFDSPL
CODELIVER1 / QRPGLESRC / PORTFUI
```

Say:

> "These are the live members on PUB400, the public IBM i server I use for
> practice. I compile directly from VS Code using the integrated terminal."

Show the compile command in the VS Code terminal:

```bash
system "WRKOBJ OBJ(CODELIVER1/PORTFUI) OBJTYPE(*PGM)"
```

### Step 3 — Run the screen in ACS (3 min)

Switch to an open ACS 5250 session (have it ready before the call).

```cl
CALL PGM(CODELIVER1/PORTFUI)
```

Walk through the screen:

1. Default values are pre-filled — point them out.
2. Press `Enter` — show the `PASS:` result line.
3. Change `STATUS` to `I`, press `Enter` — show the `FAIL: Portfolio status is inactive.` message.
4. Change `CURRENCY` to `XYZ`, press `Enter` — show the currency fail.
5. Press `F3` to exit cleanly.

Say:

> "The screen itself never knows the rules. It just calls `VALIDATEPORTFOLIO`
> and `VALIDATECURRENCY` from the service program. That separation is exactly
> what you'd see with a CICS screen calling a COBOL business service on z/OS."

### Step 4 — One-line architecture summary (1 min)

Write or share this diagram:

```text
PORTFUI (RPGLE workstation program)
  └── EXFMT MAIN       — shows/reads the 5250 screen (PORTFDSPL)
  └── VALIDATEPORTFOLIO  ─┐
  └── VALIDATECURRENCY    ├─ PORTFSVC *SRVPGM (business rules)
  └── FORMATPORTFOLIOMSG ─┘
```

Say:

> "Three layers: the display file owns screen layout, the RPGLE program owns
> the screen loop, and the service program owns every business rule. Adding a
> new front end — say, a REST API or a batch job — reuses PORTFSVC unchanged."

### Things to have open before the call

```text
Browser    : GitHub repo tab open to PORTFDSPL.dspf
VS Code    : IBM i Source Browser showing CODELIVER1 members
ACS        : 5250 session signed in, command line ready
Terminal   : VS Code terminal ready for system "WRKOBJ ..." commands
```

---

## 11. Interview Explanation

Use this explanation if asked why DOC 4 exists:

> "DOC 1 proves the service-program call chain with persistent, inspectable
> output. DOC 4 adds a native 5250 front end over the exact same service
> program — no business logic is duplicated. That separation mirrors how a
> CICS screen on z/OS delegates to a COBOL business module: the presentation
> tier is swappable, the rules tier is reused."

### Concept map — what each piece proves to an interviewer

| IBM i concept | How you show it | z/OS / mainframe analogy |
|---------------|-----------------|--------------------------|
| DDS display file | `PORTFDSPL` — field positions, B/O attributes, CA03 key | BMS mapset (CICS) |
| Workstation file | `DCL-F PORTFDSPL WORKSTN` | CICS EXEC RECEIVE MAP |
| EXFMT (write + read) | `EXFMT MAIN` inside DOW loop | CICS SEND MAP + RECEIVE MAP |
| Function key handling | `CA03` in DDS, `*IN03` in RPG | CICS AID byte check |
| Service-program binding | `BNDDIR(CODELIVER1/PORTFBNDD)` at compile | COBOL CALL to a load module |
| Separation of concerns | UI in `PORTFUI`, rules in `PORTFSVC` | CICS screen ↔ COBOL service |
| ILE activation group | `ACTGRP(JRAMSRV)` shared with PORTFSVC | JVM class loader scope |

### Likely follow-up questions and answers

**"Why use DDS instead of just printing to a spool file?"**
> "Spool output is one-way. A display file gives interactive input — the
> operator can enter data, correct it, and resubmit. That is required for any
> real operator workflow on IBM i."

**"What does EXFMT actually do?"**
> "It is a combined write-then-read. It sends the record format to the screen
> and immediately waits for the user to press Enter or a function key. The
> screen fields are populated from the RPG data structure on the way out and
> read back into it on the way in."

**"How would you add a second screen — say, a confirmation page?"**
> "Add a second record format to the DDS (e.g., `R CONFIRM`), compile the
> display file, then add a second `EXFMT CONFIRM` after the validation logic
> in the RPG loop. No change to PORTFSVC."

**"Could this same PORTFSVC service program feed a REST API?"**
> "Yes. You would write an ILE RPGLE program that accepts HTTP requests via
> IBM i's integrated web services or QHTTPSVR, call the same PORTFSVC
> procedures, and return JSON. The service program never changes."

---

## 12. Troubleshooting

### `PORTFDSPL` not found

Compile the display file first:

```cl
CRTDSPF FILE(CODELIVER1/PORTFDSPL) SRCFILE(CODELIVER1/QDDSSRC) SRCMBR(PORTFDSPL)
```

### `PORTFUI` does not bind

Check that `PORTFBNDD` contains `PORTFSVC`:

```cl
DSPBNDDIR BNDDIR(CODELIVER1/PORTFBNDD)
```

Check exported procedure names:

```cl
DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP)
```

Expected exported procedures:

```text
VALIDATEPORTFOLIO
VALIDATECURRENCY
FORMATPORTFOLIOMSG
```

### Screen does not show in VS Code terminal

That is expected. Display files are native 5250 workstation screens. Run:

```cl
CALL PGM(CODELIVER1/PORTFUI)
```

from ACS 5250 or another real 5250 terminal session.

### F3 does not exit

Confirm the DDS has:

```dds
     A                                      CA03(03 'Exit')
```

and the RPG loop checks:

```rpgle
IF *IN03 = *ON;
  LEAVE;
ENDIF;
```

---

## Final Checklist

### IBM i objects (PUB400)

```text
CODELIVER1/PORTFDSPL  *FILE DSPF    — display file compiled from QDDSSRC/PORTFDSPL
CODELIVER1/PORTFUI    *PGM          — workstation program compiled from QRPGLESRC/PORTFUI
CODELIVER1/PORTFSVC   *SRVPGM       — service program from DOC 1
CODELIVER1/PORTFBNDD  *BNDDIR       — binding directory containing PORTFSVC
```

### GitHub

```text
Repo     : github.com/<your-username>/ibmi-5250-portfolio-screen-validator
README   : explains platform, tech stack, and what the app validates
src/QDDSSRC/PORTFDSPL.dspf          — display file DDS source
src/QRPGLESRC/PORTFUI.rpgle         — workstation program source
```

### Demo readiness

```text
[ ] GitHub repo tab open to PORTFDSPL.dspf
[ ] VS Code open, IBM i Source Browser showing CODELIVER1 members
[ ] ACS 5250 session signed in and at command line
[ ] CALL PGM(CODELIVER1/PORTFUI) typed and ready to run
[ ] Know PASS case: Status=A, Value=150000.00, Currency=USD
[ ] Know FAIL cases: Status=I (inactive), Currency=XYZ (invalid)
```

This DOC is optional. If time is short, keep DOC 1 as the main proof, share the
GitHub repo link, and mention DOC 4 as a live native IBM i UI enhancement you
can walk through on request.
