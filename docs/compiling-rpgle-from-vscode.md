# Compiling RPGLE from VS Code for IBM i

Version: 1.00  
Source format: sanitized Markdown adaptation of a local Word document

## Purpose

This note records a practical milestone from my IBM i / AS400 learning workflow: I successfully compiled an RPGLE program with embedded Db2 for i SQL while working from a VS Code-oriented setup.

The original local document was created as personal learning evidence. This public Markdown version intentionally removes or masks private environment details before publication.

## What Was Proven

- Connected to an IBM i environment from a local development machine.
- Used a terminal-driven IBM i command flow compatible with a VS Code workflow.
- Compiled an RPGLE source member containing embedded SQL.
- Produced a successful program object from the source member.

## Redacted Compile Command Pattern

The original command used `CRTSQLRPGI`. Sensitive values have been replaced with placeholders:

```text
CRTSQLRPGI
  OBJ(<LIBRARY>/<PROGRAM>)
  SRCFILE(<LIBRARY>/<SOURCE_FILE>)
  SRCMBR(<SOURCE_MEMBER>)
  OBJTYPE(*PGM)
  COMMIT(*NONE)
  CLOSQLCSR(*ENDMOD)
```

## Redaction Notes

The public version masks:

- IBM i user profile names
- library names
- source file and source member names where they could identify a private setup
- host login prompts and terminal banners
- password prompts and authentication details
- local macOS username, machine prompt, and filesystem paths

No passwords, certificates, screenshots, connection profiles, private host configuration, or employer/client details are included in this repository.

## Outcome

The compile completed successfully for an RPGLE program with embedded Db2 for i SQL. This Markdown file is included as a public, recruiter-safe companion artifact showing the learning outcome without exposing operational details.

