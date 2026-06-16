package com.example.ibmi.service.ibmi;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommandExecutorService {

    private static final Logger log = LoggerFactory.getLogger(CommandExecutorService.class);

    private final AS400 as400;
    private final String library;

    public CommandExecutorService(AS400 as400, String ibmiLibrary) {
        this.as400 = as400;
        this.library = ibmiLibrary;
    }

    public boolean execute(String command) {
        try {
            CommandCall cmd = new CommandCall(as400);
            boolean success = cmd.run(command);

            if (!success) {
                for (AS400Message msg : cmd.getMessageList()) {
                    log.error("IBM i message [{}]: {}", msg.getID(), msg.getText());
                }
            }
            return success;
        } catch (Exception e) {
            log.error("CommandCall failed for command: {}", e.getMessage());
            return false;
        }
    }

    public boolean createPhysicalFile(String fileName, String text) {
        String cmd =
                String.format("CRTPF FILE(%s/%s) RCDLEN(200) TEXT('%s')", library, fileName, text);
        return execute(cmd);
    }
}
