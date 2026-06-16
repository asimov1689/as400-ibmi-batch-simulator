**FREE
// ============================================================
// PORTFINQ - Portfolio Inquiry (ILE RPG, SQLRPGLE)
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

// -- Main logic -----------------------------------------------
// EXEC SQL is identical in syntax to z/OS COBOL EXEC SQL
EXEC SQL
  SELECT OWNER, TOTAL_VALUE, CURRENCY
  INTO   :wOwner, :wValue, :wCurrency
  FROM   CODELIVER1.PORTFOLIO
  WHERE  PORTF_ID = :pPortfId
  FETCH FIRST 1 ROW ONLY;

// SQLCODE handling (z/OS: COBOL IF SQLCODE = 0 / 100 / other)
SELECT;
  WHEN SQLCODE = 0;     // Row found - populate outputs
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
