package com.example.ibmi.service.ibmi;

import com.ibm.as400.access.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
            AS400Text isinConverter = new AS400Text(12, 37, as400);
            AS400Text retCodeConverter = new AS400Text(2, 37, as400);

            ProgramParameter[] parms = new ProgramParameter[]{
                    new ProgramParameter(portfIdConverter.toBytes(
                            String.format("%-10s", portfolioId))),
                    new ProgramParameter(isinConverter.toBytes(
                            String.format("%-12s", isin))),
                    new ProgramParameter(2)
            };

            ProgramCall pgmCall = new ProgramCall(as400);
            pgmCall.setProgram(
                    String.format("/QSYS.LIB/%s.LIB/CPECHKR.PGM", library), parms);

            if (pgmCall.run()) {
                String retCode = (String) retCodeConverter.toObject(
                        parms[2].getOutputData());
                result.put("portfolioId", portfolioId);
                result.put("isin", isin);
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
