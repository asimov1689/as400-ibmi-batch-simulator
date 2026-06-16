# AS400 IBM i Batch Simulator

This repository is a learning project around IBM i / AS400 development workflows.

## Promoted IBM i Source

The IBM i components are promoted as source under `src/ibmi/`:

```text
src/ibmi/
|-- clle/
|   |-- ITEST01.clle
|   `-- ORDPROC.clle
|-- cobol/
|   `-- PORTFCBL.cbl
|-- include/
|   `-- PORTFSVCPR.rpgleinc
|-- rpgle/
|   |-- PORTFSVC.rpgle
|   |-- PORTFTEST.rpgle
|   `-- UTEST01.rpgle
|-- sql/
|   `-- CRTTABLES.sql
|-- sqlrpgle/
|   |-- ORDRBATCH.sqlrpgle
|   |-- PORTFINQ.sqlrpgle
|   `-- STEST01.sqlrpgle
`-- srvsrc/
    `-- PORTFSVC.bnd
```

These files are the repository source equivalents of the PUB400 source members
used in DOC1.

## Documentation Artifact

- [DOC1: IBM i Native Development - DB2 for i, RPGLE, COBOL/400, CL, ILE Service Programs, and Tests](docs/DOC1_Layer1_IBMi_Native_Development.md)
- [Compiling RPGLE from VS Code for IBM i](docs/compiling-rpgle-from-vscode.md)

The DOC1 guide is the main native IBM i build and verification walkthrough for
this project. It covers PUB400 setup, DB2 for i tables, RPGLE and COBOL/400
inquiry programs, a CL driver, an ILE service program, unit/integration/system
tests, and verification from both VS Code/PASE and native 5250.

The compiling note is a public companion to a local Word document I created
while learning the toolchain. It records that I successfully compiled an RPGLE
program from a VS Code-driven IBM i workflow.
