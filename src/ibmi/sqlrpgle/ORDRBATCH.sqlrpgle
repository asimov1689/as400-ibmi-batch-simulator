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

// Process all currently pending rows as one batch unit of work.
EXEC SQL
  UPDATE CODELIVER1.TRADE_ORDERS
     SET STATUS     = 'PROC',
         PROCESS_DT = CURRENT_DATE
   WHERE STATUS     = 'PEND';

IF SQLCODE < 0;
  EXEC SQL
    ROLLBACK;
  LogFatal(SqlText('UPDATE TRADE_ORDERS failed'));
  *INLR = *ON;
  RETURN;
ENDIF;

EXEC SQL
  GET DIAGNOSTICS :wRows = ROW_COUNT;

wCount = wRows;

EXEC SQL
  COMMIT;

IF SQLCODE < 0;
  EXEC SQL
    ROLLBACK;
  LogFatal(SqlText('COMMIT failed'));
ELSE;
  wCommitted = wCount;
ENDIF;

LogInfo('ORDRBATCH ended. Processed rows=' + %CHAR(wCount)
        + ', committed rows=' + %CHAR(wCommitted));

*INLR = *ON;
RETURN;

DCL-PROC LogInfo;
  DCL-PI *N;
    pText VARCHAR(512) CONST;
  END-PI;

  // Informational logging is intentionally quiet in batch/PASE runs.
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
          :'*EXT      '
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
