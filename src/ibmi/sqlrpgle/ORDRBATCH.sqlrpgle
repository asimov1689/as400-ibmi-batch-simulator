**FREE
// ============================================================
// ORDRBATCH - Batch Order Processor (SQLRPGLE)
// Purpose  : Open SQL cursor over PEND orders, process each,
//            commit every 10 rows, rollback on failure
//            write production-style diagnostics to the job log
// IBM i    : CRTSQLRPGI COMMIT(*CHG)
// ============================================================

CTL-OPT DFTACTGRP(*NO) ACTGRP(*NEW);

// IBM i job-log API. CPF9898 lets the program send plain text messages.
DCL-PR QMHSNDPM EXTPGM('QMHSNDPM');
  pMsgId       CHAR(7)   CONST;
  pMsgFile     CHAR(20)  CONST;
  pMsgData     CHAR(512) CONST OPTIONS(*VARSIZE);
  pMsgDataLen  INT(10)   CONST;
  pMsgType     CHAR(10)  CONST;
  pCallStack   CHAR(10)  CONST;
  pCallStackCtr INT(10)  CONST;
  pMsgKey      CHAR(4);
  pErrorCode   CHAR(8)   CONST;
END-PR;

DCL-PR LogInfo;
  pText VARCHAR(512) CONST;
END-PR;

DCL-PR LogFatal;
  pText VARCHAR(512) CONST;
END-PR;

DCL-PR SqlText VARCHAR(256);
  pStep VARCHAR(80) CONST;
END-PR;

// Working variables
DCL-S wOrderId   CHAR(20);
DCL-S wPortfId   CHAR(10);
DCL-S wIsin      CHAR(12);
DCL-S wQty       PACKED(15:4);
DCL-S wPrice     PACKED(15:4);
DCL-S wCount     INT(10) INZ(0);
DCL-S wCommitted INT(10) INZ(0);
DCL-S wRows      INT(10) INZ(0);

LogInfo('ORDRBATCH started');

// Declare ordered cursor.
// Do not use WHERE CURRENT OF with this cursor because ORDER BY
// can make the cursor non-updatable.
EXEC SQL
  DECLARE C_ORDERS CURSOR FOR
    SELECT ORDER_ID, PORTF_ID, ISIN, QUANTITY, PRICE
      FROM CODELIVER1.TRADE_ORDERS
     WHERE STATUS = 'PEND'
     ORDER BY ORDER_DT, ORDER_ID;

// Open cursor
EXEC SQL
  OPEN C_ORDERS;

IF SQLCODE < 0;
  LogFatal(SqlText('OPEN C_ORDERS failed'));
  *INLR = *ON;
  RETURN;
ENDIF;

// Fetch loop
DOW *ON;

  EXEC SQL
    FETCH C_ORDERS
      INTO :wOrderId, :wPortfId, :wIsin, :wQty, :wPrice;

  IF SQLCODE = 100;
    LEAVE;
  ENDIF;

  IF SQLCODE < 0;
    LogInfo(SqlText('FETCH C_ORDERS failed'));
    EXEC SQL
      ROLLBACK;
    LogFatal('ORDRBATCH rolled back after fetch failure');
    LEAVE;
  ENDIF;

  // Process the order by key
  EXEC SQL
    UPDATE CODELIVER1.TRADE_ORDERS
       SET STATUS     = 'PROC',
           PROCESS_DT = CURRENT_DATE
     WHERE ORDER_ID   = :wOrderId
       AND STATUS     = 'PEND';

  IF SQLCODE < 0;
    LogInfo(SqlText('UPDATE TRADE_ORDERS failed for order '
                    + %TRIM(wOrderId)));
    EXEC SQL
      ROLLBACK;
    LogFatal('ORDRBATCH rolled back after update failure');
    LEAVE;
  ENDIF;

  EXEC SQL
    GET DIAGNOSTICS :wRows = ROW_COUNT;

  IF wRows = 0;
    LogInfo('ORDRBATCH skipped order ' + %TRIM(wOrderId)
            + ' because no PEND row was updated');
  ENDIF;

  IF wRows > 0;
    wCount += 1;

    // Commit every 10 processed records
    IF %REM(wCount : 10) = 0;
      EXEC SQL
        COMMIT;

      IF SQLCODE < 0;
        LogInfo(SqlText('COMMIT failed after order '
                        + %TRIM(wOrderId)));
        EXEC SQL
          ROLLBACK;
        LogFatal('ORDRBATCH rolled back after commit failure');
        LEAVE;
      ENDIF;

      wCommitted = wCount;
      LogInfo('ORDRBATCH committed ' + %CHAR(wCommitted)
              + ' processed rows');
    ENDIF;
  ENDIF;

ENDDO;

// Final commit for remaining rows
IF wCount > wCommitted;
  EXEC SQL
    COMMIT;

  IF SQLCODE < 0;
    LogInfo(SqlText('Final COMMIT failed'));
    EXEC SQL
      ROLLBACK;
    LogFatal('ORDRBATCH rolled back after final commit failure');
  ELSE;
    wCommitted = wCount;
    LogInfo('ORDRBATCH final commit completed for '
            + %CHAR(wCommitted) + ' processed rows');
  ENDIF;
ENDIF;

// Close cursor
EXEC SQL
  CLOSE C_ORDERS;

IF SQLCODE < 0;
  LogFatal(SqlText('CLOSE C_ORDERS failed'));
ENDIF;

LogInfo('ORDRBATCH ended. Processed rows=' + %CHAR(wCount)
        + ', committed rows=' + %CHAR(wCommitted));

*INLR = *ON;
RETURN;

DCL-PROC LogInfo;
  DCL-PI *N;
    pText VARCHAR(512) CONST;
  END-PI;

  DCL-S msgText CHAR(512);
  DCL-S msgKey  CHAR(4);
  DCL-S apiErr  CHAR(8) INZ(X'0000000000000000');

  msgText = %SUBST(pText:1:%MIN(%LEN(pText):512));

  QMHSNDPM('CPF9898'
          :'QCPFMSG   *LIBL     '
          :msgText
          :%MIN(%LEN(%TRIMR(msgText)):512)
          :'*INFO     '
          :'*         '
          :0
          :msgKey
          :apiErr);
END-PROC;

DCL-PROC LogFatal;
  DCL-PI *N;
    pText VARCHAR(512) CONST;
  END-PI;

  DCL-S msgText CHAR(512);
  DCL-S msgKey  CHAR(4);
  DCL-S apiErr  CHAR(8) INZ(X'0000000000000000');

  msgText = %SUBST(pText:1:%MIN(%LEN(pText):512));

  QMHSNDPM('CPF9898'
          :'QCPFMSG   *LIBL     '
          :msgText
          :%MIN(%LEN(%TRIMR(msgText)):512)
          :'*ESCAPE   '
          :'*         '
          :0
          :msgKey
          :apiErr);
END-PROC;

DCL-PROC SqlText;
  DCL-PI *N VARCHAR(256);
    pStep VARCHAR(80) CONST;
  END-PI;

  RETURN pStep
       + '. SQLCODE=' + %CHAR(SQLCODE)
       + ', SQLSTATE=' + SQLSTATE;
END-PROC;
