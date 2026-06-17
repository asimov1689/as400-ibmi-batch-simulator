package com.example.ibmi.service.ibmi;

import com.ibm.as400.access.*;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProgramCallService {

    private static final Logger log = LoggerFactory.getLogger(ProgramCallService.class);

    private final AS400 as400;
    private final String library;

    public ProgramCallService(AS400 as400, String ibmiLibrary) {
        this.as400 = as400;
        this.library = ibmiLibrary;
    }

    public Map<String, String> checkEligibility(String portfolioId, String isin) {
        Map<String, String> result = new HashMap<>();

        try {
            AS400Text portfIdConverter = new AS400Text(10, 37, as400);
            AS400Text ownerConverter = new AS400Text(40, 37, as400);
            AS400PackedDecimal totalValueConverter = new AS400PackedDecimal(15, 2);
            AS400Text currencyConverter = new AS400Text(3, 37, as400);
            AS400Text retCodeConverter = new AS400Text(2, 37, as400);

            ProgramParameter[] parms =
                    new ProgramParameter[] {
                        new ProgramParameter(
                                portfIdConverter.toBytes(String.format("%-10s", portfolioId))),
                        new ProgramParameter(40),
                        new ProgramParameter(totalValueConverter.getByteLength()),
                        new ProgramParameter(3),
                        new ProgramParameter(2)
                    };

            ProgramCall pgmCall = new ProgramCall(as400);
            pgmCall.setProgram(String.format("/QSYS.LIB/%s.LIB/PORTFINQ.PGM", library), parms);

            if (pgmCall.run()) {
                String owner = (String) ownerConverter.toObject(parms[1].getOutputData());
                Object totalValue = totalValueConverter.toObject(parms[2].getOutputData());
                String currency = (String) currencyConverter.toObject(parms[3].getOutputData());
                String retCode = (String) retCodeConverter.toObject(parms[4].getOutputData());
                result.put("portfolioId", portfolioId);
                result.put("isin", isin);
                result.put("owner", owner.trim());
                result.put("currency", currency.trim());
                result.put("totalValue", totalValue.toString());
                result.put("retCode", retCode.trim());
                result.put("eligible", "00".equals(retCode.trim()) ? "true" : "false");
            } else {
                result.put("error", "Program call failed");
                for (AS400Message msg : pgmCall.getMessageList()) {
                    log.error("IBM i message: {} — {}", msg.getID(), msg.getText());
                }
            }
        } catch (Exception e) {
            log.error("checkEligibility failed: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, String> getJobInfo() {
        Map<String, String> result = new HashMap<>();

        try {
            Job job = new Job(as400);
            result.put("jobName", job.getName());
            result.put("jobUser", job.getUser());
            result.put("jobNumber", job.getNumber());
            result.put("jobType", String.valueOf(job.getType()));
        } catch (Exception e) {
            log.error("getJobInfo failed: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }
}
