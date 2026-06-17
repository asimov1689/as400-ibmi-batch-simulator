**FREE
// ============================================================
// PORTFTEST - Tests PORTFSVC and writes visible output to PORTFOUT
// IBM i    : DCL-PR prototype, binding directory, calling *SRVPGM
// z/OS eq  : COBOL CALL with type-checked USING clause
// ============================================================

CTL-OPT DFTACTGRP(*NO) ACTGRP('JRAMSRV');

DCL-F PORTFOUT DISK(100) USAGE(*OUTPUT);

// -- Prototypes for PORTFSVC procedures -----------------------
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

// -- Output record buffer for program-described file PORTFOUT -
DCL-DS outRec LEN(100);
  outText CHAR(100) POS(1);
END-DS;

// -- Test variables -------------------------------------------
DCL-S wRetCode   CHAR(2);
DCL-S wIsValid   IND;
DCL-S wMsg       VARCHAR(100);
DCL-S wStatus    CHAR(1)      INZ('A');
DCL-S wValue     PACKED(15:2) INZ(150000.00);
DCL-S wCurrency  CHAR(3)      INZ('USD');
DCL-S wPortfId   CHAR(10)     INZ('PF001     ');
DCL-S wOwner     CHAR(40)     INZ('Arthur Dent                             ');

// -- Test 1: validatePortfolio - active portfolio with value --
wRetCode = validatePortfolio(wStatus : wValue);
IF wRetCode = '00';
  outText = 'TEST1 PASS: Portfolio is valid (status=A, value>0)';
ELSE;
  outText = 'TEST1 FAIL: validatePortfolio returned ' + wRetCode;
ENDIF;
WRITE PORTFOUT outRec;

// -- Test 2: validateCurrency - USD is a valid currency -------
wIsValid = validateCurrency(wCurrency);
IF wIsValid = *ON;
  outText = 'TEST2 PASS: Currency USD is valid';
ELSE;
  outText = 'TEST2 FAIL: Currency USD rejected';
ENDIF;
WRITE PORTFOUT outRec;

// -- Test 3: formatPortfolioMsg - builds display string -------
wMsg = formatPortfolioMsg(wPortfId : wOwner : wValue : wCurrency);
outText = 'TEST3 MSG: ' + %TRIM(wMsg);
WRITE PORTFOUT outRec;

*INLR = *ON;
RETURN;
