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

PORTFID = 'PF005';
OWNER = 'Marvin';
STATUS = 'A';
CURRENCY = 'PLN';
VALUEIN = '175000.00';
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
