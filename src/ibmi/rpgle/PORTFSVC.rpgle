**FREE
// ============================================================
// PORTFSVC - ILE Service Program
// Purpose  : Shared library of 3 exported validation procs
// IBM i    : CTL-OPT NOMAIN, DCL-PROC EXPORT, *SRVPGM
// z/OS eq  : IEWL link-edit / shared DLL
// Build    : CRTRPGMOD then CRTSRVPGM (two-step ILE build)
// ============================================================

// NOMAIN = no standalone entry point - this is a *SRVPGM candidate
// z/OS: like a CSECT with no main() - only exported entry points
CTL-OPT NOMAIN;

// -- Procedure 1: validatePortfolio ---------------------------
// Checks that the portfolio has active status and positive value
// Returns: '00'=valid, '10'=inactive, '20'=zero/negative value
DCL-PROC validatePortfolio EXPORT;
  DCL-PI *N CHAR(2);
    pStatus     CHAR(1)      CONST;   // 'A'=Active, 'I'=Inactive
    pTotalValue PACKED(15:2) CONST;
  END-PI;

  IF pStatus <> 'A';
    RETURN '10';    // Portfolio is not active
  ENDIF;

  IF pTotalValue <= 0;
    RETURN '20';    // Value is zero or negative
  ENDIF;

  RETURN '00';      // All validations passed
END-PROC;

// -- Procedure 2: validateCurrency ----------------------------
// Checks that currency code is one of the 4 supported codes
// Returns: *ON (*TRUE) if valid, *OFF (*FALSE) if not
DCL-PROC validateCurrency EXPORT;
  DCL-PI *N IND;
    pCurrency CHAR(3) CONST;
  END-PI;

  SELECT;
    WHEN pCurrency = 'USD' OR pCurrency = 'EUR' OR
         pCurrency = 'CHF' OR pCurrency = 'GBP';
      RETURN *ON;
    OTHER;
      RETURN *OFF;
  ENDSL;
END-PROC;

// -- Procedure 3: formatPortfolioMsg --------------------------
// Builds a human-readable portfolio summary string
// Returns: VARCHAR(100) formatted message
DCL-PROC formatPortfolioMsg EXPORT;
  DCL-PI *N VARCHAR(100);
    pPortfId    CHAR(10)     CONST;
    pOwner      CHAR(40)     CONST;
    pTotalValue PACKED(15:2) CONST;
    pCurrency   CHAR(3)      CONST;
  END-PI;

  RETURN %TRIM(pPortfId) + ' | ' +
         %TRIM(pOwner)   + ' | ' +
         pCurrency       + ' '   +
         %CHAR(pTotalValue);
END-PROC;
