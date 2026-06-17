# DOC 3 — Optional: IBM i 5250 Display File UI (v2.00)
## Native Green-Screen Validation for PORTFSVC
### Platform: PUB400 (IBM i 7.5) · IDE: VS Code + ACS 5250 · Language: DDS + RPGLE

---

This optional extension adds a native IBM i 5250 display file (green-screen UI) to exercise the same PORTFSVC service program built in DOC 1 — demonstrating end-to-end DDS display file development, a foundational AS400 skill.

All test data in this project is fictional. Portfolio owners (Arthur Dent, Ford Prefect, Trillian, Zaphod Beeblebrox, Marvin) are characters from Douglas Adams' *The Hitchhiker's Guide to the Galaxy* and have no relation to real individuals.

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [What You Are Building](#2-what-you-are-building)
3. [Prerequisites](#3-prerequisites)
4. [Display File — PORTFDSPL](#4-display-file--portfdspl)
5. [Workstation Program — PORTFUI](#5-workstation-program--portfui)
6. [Compile and Run](#6-compile-and-run)
7. [Running and Verifying](#7-running-and-verifying)
8. [Technical Summary](#8-technical-summary)
9. [Troubleshooting](#9-troubleshooting)
10. [Final Checklist](#10-final-checklist)

---

## 1. Purpose

DOC 1 exercises PORTFSVC through file-based output (PORTFOUT, QPRINT) — stable, inspectable evidence of correct program execution. This DOC adds an interactive 5250 green-screen front end that calls the same service program without duplicating any business logic.

The goal is to practise native IBM i display-file development: DDS field definitions, workstation file handling, the EXFMT read-write cycle, and function-key routing — all core competencies for an AS400 engineer working on production green-screen applications.

---

## 2. What You Are Building

Three objects work together:

    PORTFDSPL  *FILE DSPF   — Display file defining the screen layout
    PORTFUI    *PGM         — RPGLE workstation program driving the screen
    PORTFSVC   *SRVPGM      — Existing service program from DOC 1

The screen accepts five input fields (Portfolio ID, Owner, Status, Currency, Total Value), calls the PORTFSVC validation procedures, and displays a PASS or FAIL result — all on a single 5250 screen.

---

## 3. Prerequisites

This DOC should only be attempted after DOC 1 is fully working:

    CODELIVER1/PORTFSVC   *SRVPGM exists and exports three procedures
    CODELIVER1/PORTFBD    *BNDDIR contains PORTFSVC
    DOC 1 PORTFTEST works and produces correct output

Do not use this DOC to debug PORTFSVC. Prove the service program with DOC 1 first.

---

## 4. Display File — PORTFDSPL

A DDS display file defines the 5250 screen layout — field positions, data types, and function keys. PORTFDSPL defines a single record format called MAIN.

**Pseudocode — what the DDS defines:**

    Screen size: 24 rows x 80 columns (standard 5250)
    Function key: F3 mapped to indicator 03 (exit)
    
    Record format MAIN:
      Row 1, Col 2   — Title text: "Portfolio Validation"
      Row 2, Col 2   — Help text: "F3=Exit"
      Row 4, Col 30  — PORTFID    CHAR(10)  input/output field
      Row 5, Col 30  — OWNER      CHAR(40)  input/output field
      Row 6, Col 30  — STATUS     CHAR(1)   input/output field
      Row 7, Col 30  — CURRENCY   CHAR(3)   input/output field
      Row 8, Col 30  — VALUEIN    CHAR(15)  input/output field
      Row 11, Col 2  — RESULT     CHAR(70)  output-only field

Key DDS concepts:

- **B (Both)** fields accept operator input and can be pre-filled by the program.
- **O (Output)** fields are display-only — the program writes to them but the operator cannot type into them.
- **CA03** maps the F3 key to RPG indicator 03, giving the program a clean exit path.

**Compile command:**

    CRTDSPF FILE(CODELIVER1/PORTFDSPL)
            SRCFILE(CODELIVER1/QDDSSRC)
            SRCMBR(PORTFDSPL)

---

## 5. Workstation Program — PORTFUI

PORTFUI is an RPGLE program that declares PORTFDSPL as a WORKSTN file, drives an EXFMT loop, and delegates all business logic to PORTFSVC.

**Pseudocode — program flow:**

    Declare PORTFDSPL as workstation file
    Prototype three PORTFSVC procedures:
        VALIDATEPORTFOLIO  — returns '00' (valid), '10' (inactive), '20' (bad value)
        VALIDATECURRENCY   — returns true/false
        FORMATPORTFOLIOMSG — returns formatted display string
    
    Set default screen values:
        Portfolio ID = 'PF005'
        Owner        = 'Marvin'
        Status       = 'A'
        Currency     = 'PLN'
        Total Value  = '175000.00'
        Result       = 'Press Enter to validate, or F3 to exit.'
    
    LOOP while F3 is not pressed:
        EXFMT MAIN                          -- write screen, wait for input
        
        IF F3 pressed THEN exit loop
        
        Convert VALUEIN string to packed decimal
        
        Call VALIDATEPORTFOLIO(status, value) -> retCode
        Call VALIDATECURRENCY(currency)       -> currOk
        
        IF retCode = '10' THEN
            RESULT = 'FAIL: Portfolio status is inactive.'
        ELSE IF retCode = '20' THEN
            RESULT = 'FAIL: Portfolio value is zero or negative.'
        ELSE IF currency invalid THEN
            RESULT = 'FAIL: Currency is not valid.'
        ELSE
            Call FORMATPORTFOLIOMSG -> formatted message
            RESULT = 'PASS: ' + formatted message
        END IF
    END LOOP
    
    Set last-record indicator on, return

Key RPGLE concepts:

- **DCL-F ... WORKSTN** declares a display file for interactive I/O.
- **EXFMT** (Execute Format) is a combined write-then-read: it sends the record format to the screen and immediately waits for the operator to press Enter or a function key.
- **DCL-PR ... EXTPROC(...)** prototypes an external procedure from the bound service program.
- **DFTACTGRP(*NO) / ACTGRP** places the program in an ILE activation group so it can bind to PORTFSVC.

**Compile command:**

    CRTBNDRPG PGM(CODELIVER1/PORTFUI)
              SRCFILE(CODELIVER1/QRPGLESRC)
              SRCMBR(PORTFUI)
              DFTACTGRP(*NO) ACTGRP(JRAMSRV)
              BNDDIR(CODELIVER1/PORTFBD)

---

## 6. Compile and Run

Compile the display file first, then the program. Run from a 5250 session (ACS or Tn5250j) — display files do not render in a PASE terminal.

    Step 1:  CRTDSPF   for PORTFDSPL
    Step 2:  CRTBNDRPG for PORTFUI with BNDDIR pointing to PORTFSVC
    Step 3:  CALL PGM(CODELIVER1/PORTFUI) from a 5250 session

Expected result when pressing Enter with default values:

    PASS: PF005 | Marvin | PLN 175000.00

---

## 7. Running and Verifying

Use the following sequence to verify the screen works end to end:

**Step 1 — Happy path:**
Run `CALL PGM(CODELIVER1/PORTFUI)`. Press Enter with the pre-filled default values. Confirm the result line shows `PASS: PF005 | Marvin | PLN 175000.00`.

**Step 2 — Inactive portfolio:**
Change Status to `I`, press Enter. Confirm: `FAIL: Portfolio status is inactive.`

**Step 3 — Invalid currency:**
Change Currency to `XYZ`, press Enter. Confirm: `FAIL: Currency is not valid.`

**Step 4 — Zero value:**
Change Total Value to `0`, press Enter. Confirm: `FAIL: Portfolio value is zero or negative.`

**Step 5 — Clean exit:**
Press F3. The program exits and returns to the command line.

The architecture diagram:

    PORTFUI (RPGLE workstation program)
      +-- EXFMT MAIN             shows/reads the 5250 screen (PORTFDSPL)
      +-- VALIDATEPORTFOLIO  --+
      +-- VALIDATECURRENCY   --+-- PORTFSVC *SRVPGM (business rules)
      +-- FORMATPORTFOLIOMSG --+

Three layers: the display file owns screen layout, the RPGLE program owns the screen loop, and the service program owns every business rule. Adding a new front end — a REST API, a batch job, or another screen — reuses PORTFSVC unchanged.

---

## 8. Technical Summary

### What this component demonstrates

| IBM i Concept | How It Appears | z/OS / Mainframe Analogy |
|---------------|----------------|--------------------------|
| DDS display file | PORTFDSPL — field positions, B/O attributes, CA03 key | BMS mapset (CICS) |
| Workstation file | DCL-F PORTFDSPL WORKSTN | CICS EXEC RECEIVE MAP |
| EXFMT (write + read) | EXFMT MAIN inside DOW loop | CICS SEND MAP + RECEIVE MAP |
| Function key handling | CA03 in DDS, *IN03 in RPG | CICS AID byte check |
| Service-program binding | BNDDIR at compile time | COBOL CALL to a load module |
| Separation of concerns | UI in PORTFUI, rules in PORTFSVC | CICS screen vs COBOL service |
| ILE activation group | ACTGRP(JRAMSRV) shared with PORTFSVC | Shared runtime scope |

### Common questions and answers

**Why use a display file instead of spool output?**
Spool output is one-way. A display file provides interactive input — the operator can enter data, correct it, and resubmit. This is required for any real operator workflow on IBM i.

**What does EXFMT actually do?**
It is a combined write-then-read. It sends the record format to the screen and immediately waits for the user to press Enter or a function key. Screen fields are populated from RPG variables on the way out and read back in on the way in.

**How would you add a second screen — e.g. a confirmation page?**
Add a second record format to the DDS (e.g. R CONFIRM), recompile the display file, then add a second EXFMT CONFIRM after the validation logic. No change to PORTFSVC.

**Could this same PORTFSVC feed a REST API?**
Yes. An ILE RPGLE program could accept HTTP requests via IBM i integrated web services, call the same PORTFSVC procedures, and return JSON. The service program never changes.

---

## 9. Troubleshooting

**PORTFDSPL not found:**
Compile the display file first. Verify with WRKOBJ OBJ(CODELIVER1/PORTFDSPL) OBJTYPE(*FILE).

**PORTFUI does not bind:**
Check that the binding directory contains PORTFSVC. Verify with DSPBNDDIR BNDDIR(CODELIVER1/PORTFBD). Check exported procedure names with DSPSRVPGM SRVPGM(CODELIVER1/PORTFSVC) DETAIL(*PROCEXP). Expected exports: VALIDATEPORTFOLIO, VALIDATECURRENCY, FORMATPORTFOLIOMSG.

**Screen does not render in VS Code terminal:**
This is expected. Display files are native 5250 workstation screens. Run CALL PGM(CODELIVER1/PORTFUI) from ACS, Tn5250j, or another real 5250 terminal session.

**F3 does not exit:**
Confirm the DDS includes CA03(03 'Exit') and the RPG loop checks *IN03 = *ON before calling LEAVE.

---

## 10. Final Checklist

### IBM i objects (PUB400)

    CODELIVER1/PORTFDSPL  *FILE DSPF  — display file
    CODELIVER1/PORTFUI    *PGM        — workstation program
    CODELIVER1/PORTFSVC   *SRVPGM     — service program (from DOC 1)
    CODELIVER1/PORTFBD    *BNDDIR     — binding directory containing PORTFSVC

### Verification

    [ ] PORTFDSPL compiles without errors
    [ ] PORTFUI compiles with 00 highest severity
    [ ] PASS case: Status=A, Value=175000.00, Currency=PLN -> PASS: PF005 | Marvin | PLN 175000.00
    [ ] FAIL cases: Status=I (inactive), Currency=XYZ (invalid), Value=0 (zero)
    [ ] F3 exits cleanly

This DOC is optional. If time is limited, DOC 1 alone provides full proof of the RPGLE service-program call chain. This extension adds native 5250 UI development as an additional skill demonstration.
