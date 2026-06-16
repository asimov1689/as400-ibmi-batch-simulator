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
