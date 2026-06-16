      *================================================================
      * PORTFCBL  - COBOL/400 Portfolio Inquiry
      * Purpose  : Read PORTFOLIO table by key using embedded SQL
      *            Demonstrates COBOL/400 on IBM i - syntax is
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

      * SQL Communication Area - same structure as z/OS
       EXEC SQL
           INCLUDE SQLCA
       END-EXEC.

      * Working variables for SQL output
       01  WS-OWNER         PIC X(40).
       01  WS-CURRENCY      PIC X(3).
       01  WS-TOTAL-VALUE   PIC S9(13)V99 COMP-3.

       LINKAGE SECTION.
      * Parameters - equivalent to RPG DCL-PI
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
      * Embedded SQL - IDENTICAL syntax to z/OS COBOL EXEC SQL
           EXEC SQL
               SELECT OWNER, TOTAL_VALUE, CURRENCY
               INTO   :WS-OWNER, :WS-TOTAL-VALUE, :WS-CURRENCY
               FROM   CODELIVER1.PORTFOLIO
               WHERE  PORTF_ID = :LK-PORTF-ID
               FETCH FIRST 1 ROW ONLY
           END-EXEC.

      * SQLCODE handling - IDENTICAL to z/OS
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
