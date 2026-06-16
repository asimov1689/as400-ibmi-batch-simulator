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
