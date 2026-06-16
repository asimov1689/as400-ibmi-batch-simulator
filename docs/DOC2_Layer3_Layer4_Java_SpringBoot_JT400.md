# DOC 2 — Layer 3 & Layer 4: Java Spring Boot + JT400 REST API
## IBM i Integration Services + REST API Layer
### Complete Step-by-Step Guide with Full Source Code
### Platform: macOS · IDE: VS Code · Language: Java · Build: Maven

---

## Table of Contents

1. [What You Are Building](#1-what-you-are-building)
2. [macOS Prerequisites — Java Toolchain](#2-macos-prerequisites--java-toolchain)
3. [VS Code Setup for Java](#3-vs-code-setup-for-java)
4. [Project Scaffold — Maven Structure](#4-project-scaffold--maven-structure)
5. [pom.xml — All Dependencies](#5-pomxml--all-dependencies)
6. [application.yml — Configuration](#6-applicationyml--configuration)
7. [Layer 3A — IbmiConnectionConfig.java](#7-layer-3a--ibmiconnectionconfigjava)
8. [Layer 3B — CommandExecutorService.java](#8-layer-3b--commandexecutorservicejava)
9. [Layer 3C — DataQueueService.java](#9-layer-3c--dataqueueservicejava)
10. [Layer 3D — ProgramCallService.java](#10-layer-3d--programcallservicejava)
11. [Layer 3E — PortfolioRepository.java](#11-layer-3e--portfoliorepositoryjava)
12. [Layer 4 — PortfolioController.java (REST API)](#12-layer-4--portfoliocontrollerjava-rest-api)
13. [Domain Models](#13-domain-models)
14. [Unit, Integration, and System Tests (AAA Format)](#14-unit-integration-and-system-tests-aaa-format)
15. [Running the Application Locally](#15-running-the-application-locally)
16. [Manual API Tests with REST Client (.http file)](#16-manual-api-tests-with-rest-client-http-file)

---

## 1. What You Are Building

Layer 3 and Layer 4 are the Java side of the project. A Spring Boot application runs on your **local macOS machine** and connects to the live IBM i (PUB400) using the JT400 library (IBM Toolbox for Java).

```
LAYER 4 — REST API (Spring Boot, Java)
  PortfolioController    → 8 REST endpoints
      GET  /portfolios
      GET  /portfolios/{id}
      PUT  /portfolios/{id}/value
      GET  /eligibility
      GET  /job-info
      POST /orders/enqueue
      GET  /orders/dequeue
      GET  /system/ping

LAYER 3 — IBM i Integration Services (JT400)
  IbmiConnectionConfig   → AS400 connection bean
  CommandExecutorService → CL command runner (QCMDEXC)
  DataQueueService       → *DTAQ write/read (SNDDTAQ/RCVDTAQ)
  ProgramCallService     → *PGM caller (EBCDIC parameter handling)
  PortfolioRepository    → DB2 for i JDBC (CHAIN/READ/UPDATE)
```

**Both layers connect to the same PUB400 IBM i and the same CODELIVER1 library** built in DOC 1.

---

## 2. macOS Prerequisites — Java Toolchain

### 2.1 Java 21 (Temurin LTS)

```bash
brew install --cask temurin@21
java -version
# Expected: openjdk version "21.x.x"
```

### 2.2 Maven

```bash
brew install maven
mvn --version
# Expected: Apache Maven 3.9.x
```

### 2.3 Git (already installed from DOC 1)

```bash
git --version
```

### 2.4 Set JAVA_HOME in ~/.zshrc

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc
echo $JAVA_HOME
# Expected: /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

### 2.5 Set PUB400 Environment Variables (never commit credentials)

```bash
# Add to ~/.zshrc — these are loaded every terminal session
echo 'export IBMI_HOST=pub400.com'      >> ~/.zshrc
echo 'export IBMI_USER=CODELIVER'       >> ~/.zshrc
echo 'export IBMI_PASSWORD=yourpassword' >> ~/.zshrc
echo 'export IBMI_LIBRARY=CODELIVER1'   >> ~/.zshrc
source ~/.zshrc
```

Verify:

```bash
echo $IBMI_HOST      # pub400.com
echo $IBMI_USER      # CODELIVER
echo $IBMI_LIBRARY   # CODELIVER1
```

`IBMI_USER` is the PUB400 sign-on user. `IBMI_LIBRARY` is the IBM i library/schema that contains the Layer 1 tables from DOC 1.

---

## 3. VS Code Setup for Java

### 3.1 Install the Java Extension Pack

```bash
code --install-extension vscjava.vscode-java-pack
# This installs:
#   Language Support for Java (Red Hat)
#   Debugger for Java (Microsoft)
#   Test Runner for Java (Microsoft)
#   Maven for Java (Microsoft)
#   Project Manager for Java (Microsoft)
```

### 3.2 Install REST Client Extension (for .http files)

```bash
code --install-extension humao.rest-client
```

### 3.3 Open the Project Folder in VS Code

```bash
mkdir -p ~/projects/ibmi-batch-simulator
cd ~/projects/ibmi-batch-simulator
code .
```

---

## 4. Project Scaffold — Maven Structure

Create the full directory structure from the VS Code terminal:

```bash
mkdir -p src/main/java/com/example/ibmi/{config,model,ibmi,db2i,api}
mkdir -p src/main/resources
mkdir -p src/test/java/com/example/ibmi/{unit,integration,system}
mkdir -p src/test/resources
mkdir -p src/test/http
mkdir -p docs/ibmi-native/{sql,rpgle,srvsrc,cbl,clle}
touch pom.xml
touch src/main/resources/application.yml
touch src/test/resources/application-test.yml
touch .gitignore
```

Final structure:

```
ibmi-batch-simulator/
├── pom.xml
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/example/ibmi/
│   │   │   ├── IbmiApplication.java
│   │   │   ├── config/
│   │   │   │   └── IbmiConnectionConfig.java
│   │   │   ├── model/
│   │   │   │   ├── Portfolio.java
│   │   │   │   ├── TradeOrder.java
│   │   │   │   └── ApiResponse.java
│   │   │   ├── ibmi/
│   │   │   │   ├── CommandExecutorService.java
│   │   │   │   ├── DataQueueService.java
│   │   │   │   └── ProgramCallService.java
│   │   │   ├── db2i/
│   │   │   │   └── PortfolioRepository.java
│   │   │   └── api/
│   │   │       └── PortfolioController.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/com/example/ibmi/
│       │   ├── unit/
│       │   │   ├── PortfolioRepositoryUnitTest.java
│       │   │   └── DataQueueServiceUnitTest.java
│       │   ├── integration/
│       │   │   ├── PortfolioRepositoryIntegrationTest.java
│       │   │   └── ProgramCallIntegrationTest.java
│       │   └── system/
│       │       └── BatchSettlementSystemTest.java
│       ├── resources/
│       │   └── application-test.yml
│       └── http/
│           └── ibmi-tests.http
└── docs/
    └── ibmi-native/   ← (IBM i source from DOC 1: SQL, RPGLE, binder source, COBOL, CL)
```

---

## 5. pom.xml — All Dependencies

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
    <relativePath/>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>ibmi-batch-simulator</artifactId>
  <version>2.0.0</version>
  <name>ibmi-batch-simulator</name>
  <description>
    IBM i Portfolio Management System — Spring Boot + JT400
    Demonstrates IBM i integration: DB2 for i, *DTAQ, *PGM calls
    Connects to PUB400.com (free public IBM i 7.5 system)
  </description>

  <properties>
    <java.version>21</java.version>
    <jt400.version>20.0.0</jt400.version>
  </properties>

  <dependencies>

    <!-- Spring Boot Web — REST API (Layer 4) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot JDBC — DB2 for i data access (Layer 3) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <!-- JT400 — IBM Toolbox for Java (IBM i connectivity) -->
    <!-- Provides: AS400, ProgramCall, DataQueue, AS400JDBCDriver -->
    <dependency>
      <groupId>net.sf.jt400</groupId>
      <artifactId>jt400</artifactId>
      <version>${jt400.version}</version>
    </dependency>

    <!-- Jackson — JSON serialisation/deserialisation -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Spring Boot Test — JUnit 5, Mockito, MockMvc -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>

    <!-- Mockito — mock IBM i dependencies in unit tests -->
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <scope>test</scope>
    </dependency>

  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <!-- Surefire — runs JUnit 5 tests -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <!-- Skip integration/system tests by default;
               run them with: mvn test -Pintegration -->
          <excludes>
            <exclude>**/integration/**</exclude>
            <exclude>**/system/**</exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <!-- Profile to run integration + system tests (requires PUB400) -->
  <profiles>
    <profile>
      <id>integration</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <excludes>
                <exclude>none</exclude>
              </excludes>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>

</project>
```

---

## 6. application.yml — Configuration

`src/main/resources/application.yml` — **safe to commit** (no real credentials):

```yaml
# IBM i / PUB400 connection — credentials always from env vars
ibmi:
  host:     ${IBMI_HOST:pub400.com}
  user:     ${IBMI_USER:CODELIVER}
  password: ${IBMI_PASSWORD}          # NEVER hardcode — always env var
  library:  ${IBMI_LIBRARY:CODELIVER1}
  # Data queue name for async order processing
  order-queue: ORDERQ

# Spring JDBC datasource — DB2 for i via AS400JDBCDriver (JT400)
spring:
  datasource:
    url:                   jdbc:as400://pub400.com/CODELIVER1;naming=sql;errors=full
    username:              ${IBMI_USER:CODELIVER}
    password:              ${IBMI_PASSWORD}
    driver-class-name:     com.ibm.as400.access.AS400JDBCDriver
    # Connection pool
    hikari:
      maximum-pool-size:   5
      minimum-idle:        1
      connection-timeout:  30000
      idle-timeout:        300000

# Server
server:
  port: 8080

# Logging
logging:
  level:
    com.example.ibmi: DEBUG
    com.ibm.as400:    WARN
```

`src/test/resources/application-test.yml` — test profile:

```yaml
# Test profile — used by unit tests (no real IBM i connection needed)
ibmi:
  host:     localhost
  user:     testuser
  password: testpass
  library:  TESTLIB
  order-queue: TESTQ

spring:
  datasource:
    url:               jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username:          sa
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema-h2.sql
      data-locations:   classpath:data-test.sql
```

`src/test/resources/schema-h2.sql` — H2 in-memory schema for unit tests:

```sql
CREATE TABLE IF NOT EXISTS PORTFOLIO (
    PORTF_ID    CHAR(10)       NOT NULL PRIMARY KEY,
    OWNER       CHAR(40)       NOT NULL,
    CURRENCY    CHAR(3)        NOT NULL DEFAULT 'USD',
    TOTAL_VALUE DECIMAL(15,2)  NOT NULL DEFAULT 0,
    STATUS      CHAR(1)        NOT NULL DEFAULT 'A',
    LAST_UPD    DATE           NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS TRADE_ORDERS (
    ORDER_ID    CHAR(20)       NOT NULL PRIMARY KEY,
    PORTF_ID    CHAR(10)       NOT NULL,
    ISIN        CHAR(12)       NOT NULL,
    QUANTITY    DECIMAL(15,4)  NOT NULL DEFAULT 0,
    PRICE       DECIMAL(15,4)  NOT NULL DEFAULT 0,
    ORDER_DT    DATE           NOT NULL DEFAULT CURRENT_DATE,
    PROCESS_DT  DATE,
    STATUS      CHAR(4)        NOT NULL DEFAULT 'PEND'
);
```

`src/test/resources/data-test.sql` — seed data for unit tests:

```sql
INSERT INTO PORTFOLIO VALUES ('PF001     ', 'Richard Papen                           ', 'USD', 150000.00, 'A', CURRENT_DATE);
INSERT INTO PORTFOLIO VALUES ('PF002     ', 'Henry Winter                            ', 'CHF', 280000.00, 'A', CURRENT_DATE);
INSERT INTO PORTFOLIO VALUES ('PF003     ', 'Camilla Macaulay                        ', 'EUR',  95000.00, 'I', CURRENT_DATE);
INSERT INTO TRADE_ORDERS VALUES ('ORD-2026-001', 'PF001     ', 'TSH000000001', 100, 182.50, CURRENT_DATE, NULL, 'PEND');
INSERT INTO TRADE_ORDERS VALUES ('ORD-2026-002', 'PF001     ', 'TSH000000002',  50, 312.00, CURRENT_DATE, NULL, 'PEND');
INSERT INTO TRADE_ORDERS VALUES ('ORD-2026-003', 'PF002     ', 'TSH000000003', 200,  45.75, CURRENT_DATE, NULL, 'PEND');
```

---

## 7. Layer 3A — IbmiConnectionConfig.java

`src/main/java/com/example/ibmi/config/IbmiConnectionConfig.java`

```java
package com.example.ibmi.config;

import com.ibm.as400.access.AS400;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IBM i Connection Configuration
 *
 * Creates and manages the AS400 connection object used by all JT400 services.
 * IBM i concept: AS400 object = the JT400 entry point for all IBM i operations.
 *                Equivalent to opening a CICS region connection or MQ QueueManager handle.
 * z/OS eq:       CICS connection factory / MQ QueueManager connection object
 */
@Configuration
public class IbmiConnectionConfig {

    @Value("${ibmi.host}")
    private String host;

    @Value("${ibmi.user}")
    private String user;

    @Value("${ibmi.password}")
    private String password;

    @Value("${ibmi.library}")
    private String library;

    /**
     * Primary AS400 connection bean.
     * setGuiAvailable(false) prevents the 5250 sign-on dialog in server-side mode.
     * IBM i concept: One AS400 object = one authenticated connection to one IBM i system.
     */
    @Bean
    public AS400 as400() {
        AS400 as400 = new AS400(host, user, password);
        as400.setGuiAvailable(false);   // No 5250 pop-up in headless server mode
        return as400;
    }

    /**
     * Exposes the library name as a Spring bean for injection into services.
     * IBM i concept: Library = z/OS HLQ or DB2 schema name.
     */
    @Bean
    public String ibmiLibrary() {
        return library;
    }
}
```

---

## 8. Layer 3B — CommandExecutorService.java

`src/main/java/com/example/ibmi/ibmi/CommandExecutorService.java`

```java
package com.example.ibmi.ibmi;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CommandExecutorService — CL Command Runner
 *
 * Wraps JT400 CommandCall to execute any CL command string from Java.
 * IBM i concept: CommandCall = Java equivalent of calling QCMDEXC API from RPG.
 *                In RPG:  CALL QCMDEXC PARM(cmdString cmdLen)
 *                In Java: new CommandCall(as400).run("DSPLIBL OUTPUT(*PRINT)")
 * z/OS eq:       EXEC CICS LINK to a utility program that runs QCMDEXC
 */
@Service
public class CommandExecutorService {

    private static final Logger log = LoggerFactory.getLogger(CommandExecutorService.class);

    private final AS400 as400;
    private final String library;

    public CommandExecutorService(AS400 as400, String ibmiLibrary) {
        this.as400   = as400;
        this.library = ibmiLibrary;
    }

    /**
     * Execute any CL command string on the IBM i.
     *
     * @param command The CL command string, e.g. "DSPLIBL OUTPUT(*PRINT)"
     * @return true if the command completed successfully, false otherwise
     *
     * IBM i concept: CommandCall.run() = QCMDEXC API invocation
     *                getMessageList()  = RCVMSG in CL
     */
    public boolean execute(String command) {
        try {
            CommandCall cmd = new CommandCall(as400);
            boolean success = cmd.run(command);

            if (!success) {
                // IBM i sends back AS400Message objects on failure
                // z/OS equivalent: reading SYSOUT messages after JCL step failure
                for (AS400Message msg : cmd.getMessageList()) {
                    log.error("IBM i message [{}]: {}", msg.getID(), msg.getText());
                }
            }
            return success;
        } catch (Exception e) {
            log.error("CommandCall failed for command [{}]: {}", command, e.getMessage());
            return false;
        }
    }

    /**
     * Convenience method: create a physical file on IBM i.
     * IBM i concept: CRTPF = Create Physical File (*FILE object)
     * z/OS eq:       IDCAMS DEFINE CLUSTER or CREATE TABLE in DB2 z/OS
     */
    public boolean createPhysicalFile(String fileName, String text) {
        String cmd = String.format(
            "CRTPF FILE(%s/%s) RCDLEN(200) TEXT('%s')",
            library, fileName, text
        );
        return execute(cmd);
    }
}
```

---

## 9. Layer 3C — DataQueueService.java

`src/main/java/com/example/ibmi/ibmi/DataQueueService.java`

```java
package com.example.ibmi.ibmi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.ibmi.model.TradeOrder;

import java.util.Arrays;
import java.util.Optional;

/**
 * DataQueueService — IBM i *DTAQ Producer and Consumer
 *
 * Produces and consumes IBM i Data Queues (*DTAQ) using JT400 DataQueue class.
 * IBM i concept: *DTAQ = FIFO message channel between IBM i jobs.
 *                CL:   CRTDTAQ → SNDDTAQ → RCVDTAQ
 *                Java: DataQueue.create() → DataQueue.write() → DataQueue.read()
 * z/OS eq:       IBM MQ (inter-system) or CICS Temporary Storage Queue (intra-CICS)
 * Business use:  Models how Olympic receives orders from external systems asynchronously
 */
@Service
public class DataQueueService {

    private static final Logger log  = LoggerFactory.getLogger(DataQueueService.class);
    private static final int    ENTRY_LENGTH = 200;   // Max bytes per data queue entry

    private final AS400        as400;
    private final String       library;
    private final String       queueName;
    private final ObjectMapper mapper;

    public DataQueueService(
            AS400 as400,
            String ibmiLibrary,
            @Value("${ibmi.order-queue:ORDERQ}") String queueName) {
        this.as400     = as400;
        this.library   = ibmiLibrary;
        this.queueName = queueName;
        this.mapper    = new ObjectMapper();
    }

    /**
     * Enqueue a TradeOrder to the IBM i *DTAQ.
     * IBM i concept: SNDDTAQ DTAQ(ORDERQ) LEN(200) DATA(jsonBytes)
     * z/OS eq:       MQPUT to an MQ queue
     *
     * @param order The trade order to enqueue
     * @return true if successfully written to the queue
     */
    public boolean enqueueOrder(TradeOrder order) {
        try {
            DataQueue queue = new DataQueue(as400,
                String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));

            // Serialise order to JSON, pad to ENTRY_LENGTH bytes
            // IBM i *DTAQ entries are fixed-length — must be exactly ENTRY_LENGTH
            String json    = mapper.writeValueAsString(order);
            byte[] payload = new byte[ENTRY_LENGTH];
            byte[] jsonBytes = json.getBytes("UTF-8");
            System.arraycopy(jsonBytes, 0, payload, 0,
                Math.min(jsonBytes.length, ENTRY_LENGTH));

            queue.write(payload);
            log.debug("Enqueued order {} to *DTAQ {}", order.getOrderId(), queueName);
            return true;

        } catch (Exception e) {
            log.error("Failed to enqueue order to *DTAQ {}: {}", queueName, e.getMessage());
            return false;
        }
    }

    /**
     * Dequeue the next TradeOrder from the IBM i *DTAQ.
     * IBM i concept: RCVDTAQ DTAQ(ORDERQ) LEN(200) DATA(&RESULT) WAIT(5)
     * z/OS eq:       MQGET from an MQ queue with a timeout
     *
     * @param waitSeconds How long to wait for an entry (0 = no wait, -1 = wait forever)
     * @return Optional<TradeOrder> — empty if no entry within the timeout
     */
    public Optional<TradeOrder> dequeueOrder(int waitSeconds) {
        try {
            DataQueue queue = new DataQueue(as400,
                String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));

            // read(timeout) in seconds — blocks up to timeout waiting for an entry
            DataQueueEntry entry = queue.read(waitSeconds);

            if (entry == null) {
                return Optional.empty();   // Timeout — nothing in the queue
            }

            // Trim null padding bytes before JSON deserialisation
            byte[] raw   = entry.getData();
            int    len   = raw.length;
            while (len > 0 && raw[len - 1] == 0) len--;
            String json  = new String(Arrays.copyOf(raw, len), "UTF-8").trim();

            TradeOrder order = mapper.readValue(json, TradeOrder.class);
            log.debug("Dequeued order {} from *DTAQ {}", order.getOrderId(), queueName);
            return Optional.of(order);

        } catch (Exception e) {
            log.error("Failed to dequeue from *DTAQ {}: {}", queueName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Create the *DTAQ on IBM i if it does not already exist.
     * IBM i concept: CRTDTAQ DTAQ(LIB/ORDERQ) MAXLEN(200)
     */
    public boolean createQueueIfAbsent() {
        try {
            DataQueue queue = new DataQueue(as400,
                String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));
            queue.create(ENTRY_LENGTH);
            log.info("Created *DTAQ {}/{}", library, queueName);
            return true;
        } catch (Exception e) {
            // Queue may already exist — not necessarily an error
            log.debug("*DTAQ {}/{} may already exist: {}", library, queueName, e.getMessage());
            return false;
        }
    }
}
```

---

## 10. Layer 3D — ProgramCallService.java

`src/main/java/com/example/ibmi/ibmi/ProgramCallService.java`

```java
package com.example.ibmi.ibmi;

import com.ibm.as400.access.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ProgramCallService — IBM i *PGM Caller
 *
 * Calls IBM i *PGM objects from Java using JT400 ProgramCall.
 * IBM i concept: ProgramCall = Java equivalent of CALL PGM() in CL or CALL in RPG.
 *                AS400Text(n,37,as400) = EBCDIC CHAR(n) converter
 *                ProgramParameter with length = output parameter
 * z/OS eq:       EXEC CICS LINK or COBOL CALL to a named load module
 *
 * Key methods:
 *   checkEligibility() — calls a hypothetical CPECHKR *PGM (demonstrates ProgramCall)
 *   getJobInfo()       — calls QUSRJOBI system API (real IBM i system API on PUB400)
 */
@Service
public class ProgramCallService {

    private static final Logger log = LoggerFactory.getLogger(ProgramCallService.class);

    private final AS400  as400;
    private final String library;

    public ProgramCallService(AS400 as400, String ibmiLibrary) {
        this.as400   = as400;
        this.library = ibmiLibrary;
    }

    /**
     * Demonstrates IBM i *PGM call with EBCDIC parameter passing.
     *
     * IBM i concept: AS400Text(12,37,as400).toBytes(isin) converts the Java String
     *                to EBCDIC CHAR(12) — exactly what the RPG program expects in its DCL-PI.
     *                CCSID 37 = IBM i standard English EBCDIC.
     * z/OS eq:       EXEC CICS LINK PROGRAM('CPECHKR') COMMAREA(data)
     *
     * @param portfolioId The portfolio ID to check eligibility for
     * @param isin        The ISIN security code (12 chars)
     * @return Map with eligibility result and IBM i concept explanation
     */
    public Map<String, String> checkEligibility(String portfolioId, String isin) {
        Map<String, String> result = new HashMap<>();

        try {
            // AS400Text = EBCDIC converter (z/OS: COBOL COMP-1 EBCDIC field)
            AS400Text portfIdConverter = new AS400Text(10, 37, as400);
            AS400Text isinConverter    = new AS400Text(12, 37, as400);
            AS400Text retCodeConverter = new AS400Text(2,  37, as400);

            // Build parameter list (z/OS: COMMAREA or CICS LINK PARM)
            ProgramParameter[] parms = new ProgramParameter[] {
                new ProgramParameter(portfIdConverter.toBytes(
                    String.format("%-10s", portfolioId))),   // Input: CHAR(10)
                new ProgramParameter(isinConverter.toBytes(
                    String.format("%-12s", isin))),           // Input: CHAR(12)
                new ProgramParameter(2)                       // Output: CHAR(2) return code
            };

            ProgramCall pgmCall = new ProgramCall(as400);
            pgmCall.setProgram(
                String.format("/QSYS.LIB/%s.LIB/CPECHKR.PGM", library), parms);

            if (pgmCall.run()) {
                String retCode = (String) retCodeConverter.toObject(
                    parms[2].getOutputData());
                result.put("portfolioId", portfolioId);
                result.put("isin",        isin);
                result.put("retCode",     retCode.trim());
                result.put("eligible",    "00".equals(retCode.trim()) ? "true" : "false");
                result.put("ibmiConcept", "ProgramCall: called CPECHKR *PGM with EBCDIC CHAR(10) and CHAR(12) parameters using AS400Text CCSID-37 converter");
            } else {
                result.put("error", "Program call failed");
                for (AS400Message msg : pgmCall.getMessageList()) {
                    log.error("IBM i message: {} — {}", msg.getID(), msg.getText());
                }
            }
        } catch (Exception e) {
            log.error("checkEligibility failed: {}", e.getMessage());
            result.put("error",      e.getMessage());
            result.put("ibmiConcept","ProgramCall with EBCDIC parameter conversion via AS400Text");
        }

        return result;
    }

    /**
     * Calls the real IBM i system API QUSRJOBI to get current job information.
     * This works on PUB400 and returns the actual running job name, user, and number.
     *
     * IBM i concept: QUSRJOBI is a real IBM i system API (*PGM in QSYS library).
     *                Job name, user, job number — the IBM i job identity triple.
     * z/OS eq:       QSYS functions or EXEC CICS INQUIRE TASK for job/task info
     */
    public Map<String, String> getJobInfo() {
        Map<String, String> result = new HashMap<>();

        try {
            Job job = new Job(as400);   // Current job on IBM i
            result.put("jobName",   job.getName());
            result.put("jobUser",   job.getUser());
            result.put("jobNumber", job.getNumber());
            result.put("jobType",   String.valueOf(job.getType()));
            result.put("ibmiConcept",
                "Job identity on IBM i: name/user/number triple. " +
                "QUSRJOBI system API via JT400 Job class. " +
                "z/OS eq: EXEC CICS INQUIRE TASK / DISPLAY JOBS JCL command.");
        } catch (Exception e) {
            log.error("getJobInfo failed: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }
}
```

---

## 11. Layer 3E — PortfolioRepository.java

`src/main/java/com/example/ibmi/db2i/PortfolioRepository.java`

```java
package com.example.ibmi.db2i;

import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PortfolioRepository — DB2 for i Data Access
 *
 * Uses Spring JdbcTemplate with AS400JDBCDriver (from JT400) to access DB2 for i.
 * IBM i concepts demonstrated:
 *   findById()        — keyed direct read (CHAIN opcode equivalent)
 *   findAllActive()   — sequential read loop (READ opcode equivalent)
 *   updateValue()     — journaled UPDATE with @Transactional (COMMIT equivalent)
 *   findPendingOrders()— cursor-equivalent SELECT with filter
 * z/OS eq: JDBC to DB2 for z/OS or COBOL EXEC SQL SELECT/UPDATE
 */
@Repository
public class PortfolioRepository {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRepository.class);

    private final JdbcTemplate jdbc;
    private final String       library;

    public PortfolioRepository(JdbcTemplate jdbcTemplate, String ibmiLibrary) {
        this.jdbc    = jdbcTemplate;
        this.library = ibmiLibrary;
    }

    // ── Row mappers ──────────────────────────────────────────────────────────

    private final RowMapper<Portfolio> portfolioMapper = (rs, rowNum) -> {
        Portfolio p = new Portfolio();
        p.setPortfId(   rs.getString("PORTF_ID").trim());
        p.setOwner(     rs.getString("OWNER").trim());
        p.setCurrency(  rs.getString("CURRENCY").trim());
        p.setTotalValue(rs.getBigDecimal("TOTAL_VALUE"));
        p.setStatus(    rs.getString("STATUS").trim());
        p.setLastUpd(   rs.getDate("LAST_UPD") != null
                         ? rs.getDate("LAST_UPD").toLocalDate() : null);
        return p;
    };

    private final RowMapper<TradeOrder> orderMapper = (rs, rowNum) -> {
        TradeOrder o = new TradeOrder();
        o.setOrderId(  rs.getString("ORDER_ID").trim());
        o.setPortfId(  rs.getString("PORTF_ID").trim());
        o.setIsin(     rs.getString("ISIN").trim());
        o.setQuantity( rs.getBigDecimal("QUANTITY"));
        o.setPrice(    rs.getBigDecimal("PRICE"));
        o.setStatus(   rs.getString("STATUS").trim());
        return o;
    };

    // ── Portfolio operations ─────────────────────────────────────────────────

    /**
     * Find portfolio by ID — keyed direct read.
     * IBM i concept: CHAIN opcode (RPG) — reads a record directly by key
     *                EXEC SQL SELECT INTO :var WHERE PORTF_ID = :key (SQLRPGLE)
     * z/OS eq:       EXEC SQL SELECT INTO :WS-OWNER FROM PORTFOLIO WHERE PORTF_ID = :LK-ID
     */
    public Optional<Portfolio> findById(String portfolioId) {
        String sql = String.format(
            "SELECT PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD " +
            "FROM %s.PORTFOLIO WHERE PORTF_ID = ?", library);

        try {
            // Pad to CHAR(10) — IBM i CHAR fields are fixed length
            List<Portfolio> rows = jdbc.query(sql,
                portfolioMapper,
                String.format("%-10s", portfolioId));
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            log.error("findById failed for {}: {}", portfolioId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Find all active portfolios — sequential read through the view.
     * IBM i concept: READ opcode on ACTIVE_PORTFOLIOS view (logical file equivalent)
     *                SQL View enforces the STATUS='A' business rule at DB level
     * z/OS eq:       EXEC SQL SELECT * FROM ACTIVE_PORTFOLIOS (DB2 z/OS view)
     */
    public List<Portfolio> findAllActive() {
        String sql = String.format(
            "SELECT PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD " +
            "FROM %s.ACTIVE_PORTFOLIOS " +
            "ORDER BY PORTF_ID", library);

        return jdbc.query(sql, portfolioMapper);
    }

    /**
     * Update portfolio total value — journaled UPDATE with commitment control.
     * IBM i concept: @Transactional = STRCMTCTL + COMMIT on success / ROLLBACK on exception
     *                DB2 for i journals every change — audit trail is automatic
     * z/OS eq:       EXEC SQL UPDATE + EXEC SQL COMMIT / CICS SYNCPOINT
     */
    @Transactional
    public boolean updateValue(String portfolioId, java.math.BigDecimal newValue) {
        String sql = String.format(
            "UPDATE %s.PORTFOLIO " +
            "SET TOTAL_VALUE = ?, LAST_UPD = CURRENT_DATE " +
            "WHERE PORTF_ID = ?", library);

        int rows = jdbc.update(sql,
            newValue,
            String.format("%-10s", portfolioId));

        return rows > 0;
    }

    // ── Trade order operations ───────────────────────────────────────────────

    /**
     * Find all pending trade orders.
     * IBM i concept: DECLARE CURSOR FOR SELECT WHERE STATUS='PEND' (ORDRBATCH)
     * z/OS eq:       COBOL DECLARE C_ORDERS CURSOR FOR SELECT ... WHERE STATUS = 'PEND'
     */
    public List<TradeOrder> findPendingOrders() {
        String sql = String.format(
            "SELECT ORDER_ID, PORTF_ID, ISIN, QUANTITY, PRICE, STATUS " +
            "FROM %s.TRADE_ORDERS " +
            "WHERE STATUS = 'PEND' " +
            "ORDER BY ORDER_DT, ORDER_ID", library);

        return jdbc.query(sql, orderMapper);
    }

    /**
     * Process a trade order — update STATUS from PEND to PROC.
     * IBM i concept: UPDATE WHERE CURRENT OF cursor (ORDRBATCH batch processor)
     *                @Transactional ensures COMMIT or ROLLBACK
     * z/OS eq:       EXEC SQL UPDATE WHERE CURRENT OF C_ORDERS + EXEC SQL COMMIT
     */
    @Transactional
    public boolean processOrder(String orderId) {
        String sql = String.format(
            "UPDATE %s.TRADE_ORDERS " +
            "SET STATUS = 'PROC', PROCESS_DT = CURRENT_DATE " +
            "WHERE ORDER_ID = ? AND STATUS = 'PEND'", library);

        int rows = jdbc.update(sql, String.format("%-20s", orderId));
        return rows > 0;
    }
}
```

---

## 12. Layer 4 — PortfolioController.java (REST API)

`src/main/java/com/example/ibmi/api/PortfolioController.java`

```java
package com.example.ibmi.api;

import com.example.ibmi.db2i.PortfolioRepository;
import com.example.ibmi.ibmi.CommandExecutorService;
import com.example.ibmi.ibmi.DataQueueService;
import com.example.ibmi.ibmi.ProgramCallService;
import com.example.ibmi.model.ApiResponse;
import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PortfolioController — 8 REST Endpoints
 *
 * Exposes all IBM i capabilities as a modern JSON API.
 * Every response includes an ibmiConcept field explaining the IBM i operation.
 * IBM i concept: REST layer = the modernisation pattern.
 *                RPG/CL at the core, Java REST API as the interface.
 *                "Dress up the green screen" — not replacing RPG, wrapping it.
 * z/OS eq:       CICS Web Services or IBM DataPower gateway over z/OS transactions
 */
@RestController
@RequestMapping("/api/v1")
public class PortfolioController {

    private final PortfolioRepository   portfolioRepo;
    private final DataQueueService      dataQueueService;
    private final ProgramCallService    programCallService;
    private final CommandExecutorService commandExecutorService;

    public PortfolioController(
            PortfolioRepository portfolioRepo,
            DataQueueService dataQueueService,
            ProgramCallService programCallService,
            CommandExecutorService commandExecutorService) {
        this.portfolioRepo          = portfolioRepo;
        this.dataQueueService       = dataQueueService;
        this.programCallService     = programCallService;
        this.commandExecutorService = commandExecutorService;
    }

    /**
     * GET /api/v1/portfolios
     * Returns all active portfolios via the ACTIVE_PORTFOLIOS SQL view.
     * IBM i concept: READ loop on ACTIVE_PORTFOLIOS view (Logical File equivalent)
     */
    @GetMapping("/portfolios")
    public ResponseEntity<ApiResponse<List<Portfolio>>> getAllPortfolios() {
        List<Portfolio> portfolios = portfolioRepo.findAllActive();
        return ResponseEntity.ok(new ApiResponse<>(
            portfolios,
            "DB2 for i SELECT via ACTIVE_PORTFOLIOS SQL View — " +
            "equivalent of READ opcode on a DDS Logical File with SELECT(STATUS='A')"
        ));
    }

    /**
     * GET /api/v1/portfolios/{id}
     * Returns a single portfolio by ID — keyed direct read.
     * IBM i concept: CHAIN opcode — reads directly by key (no sequential scan)
     */
    @GetMapping("/portfolios/{id}")
    public ResponseEntity<ApiResponse<Portfolio>> getPortfolioById(
            @PathVariable String id) {
        Optional<Portfolio> portfolio = portfolioRepo.findById(id);
        if (portfolio.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ApiResponse<>(
            portfolio.get(),
            "DB2 for i JDBC keyed read — CHAIN opcode equivalent. " +
            "PORTF_ID is the primary key (PF = Physical File). " +
            "JdbcTemplate.query() → AS400JDBCDriver → DB2 for i SELECT WHERE PORTF_ID=?"
        ));
    }

    /**
     * PUT /api/v1/portfolios/{id}/value
     * Updates portfolio total value with @Transactional commitment control.
     * IBM i concept: COMMIT (*CHG) — journaled UPDATE, atomic transaction
     */
    @PutMapping("/portfolios/{id}/value")
    public ResponseEntity<ApiResponse<String>> updatePortfolioValue(
            @PathVariable String id,
            @RequestParam BigDecimal newValue) {
        boolean updated = portfolioRepo.updateValue(id, newValue);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ApiResponse<>(
            "Portfolio " + id + " updated to " + newValue,
            "@Transactional in Java = STRCMTCTL + COMMIT on IBM i. " +
            "DB2 for i automatically journals the change — audit trail is built-in. " +
            "z/OS eq: EXEC SQL UPDATE + EXEC SQL COMMIT / CICS SYNCPOINT"
        ));
    }

    /**
     * GET /api/v1/orders/pending
     * Returns all pending trade orders.
     * IBM i concept: DECLARE CURSOR FOR SELECT WHERE STATUS='PEND'
     */
    @GetMapping("/orders/pending")
    public ResponseEntity<ApiResponse<List<TradeOrder>>> getPendingOrders() {
        List<TradeOrder> orders = portfolioRepo.findPendingOrders();
        return ResponseEntity.ok(new ApiResponse<>(
            orders,
            "DB2 for i SELECT WHERE STATUS='PEND' — equivalent of " +
            "DECLARE CURSOR FOR SELECT ... WHERE STATUS='PEND' in ORDRBATCH"
        ));
    }

    /**
     * POST /api/v1/orders/enqueue
     * Writes a trade order to the IBM i *DTAQ.
     * IBM i concept: SNDDTAQ — send to data queue
     */
    @PostMapping("/orders/enqueue")
    public ResponseEntity<ApiResponse<String>> enqueueOrder(
            @RequestBody TradeOrder order) {
        boolean queued = dataQueueService.enqueueOrder(order);
        if (!queued) {
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>("Failed to enqueue order", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(
            "Order " + order.getOrderId() + " enqueued",
            "*DTAQ write via JT400 DataQueue.write() — SNDDTAQ equivalent. " +
            "*DTAQ = FIFO message channel between IBM i jobs. " +
            "z/OS eq: MQPUT to IBM MQ queue / CICS WRITEQ TS"
        ));
    }

    /**
     * GET /api/v1/orders/dequeue
     * Reads the next trade order from the IBM i *DTAQ.
     * IBM i concept: RCVDTAQ — receive from data queue (blocks up to timeout)
     */
    @GetMapping("/orders/dequeue")
    public ResponseEntity<ApiResponse<TradeOrder>> dequeueOrder(
            @RequestParam(defaultValue = "5") int waitSeconds) {
        Optional<TradeOrder> order = dataQueueService.dequeueOrder(waitSeconds);
        if (order.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(
                null,
                "*DTAQ read timed out — no entries within " + waitSeconds + "s. " +
                "RCVDTAQ WAIT(" + waitSeconds + ") equivalent."
            ));
        }
        return ResponseEntity.ok(new ApiResponse<>(
            order.get(),
            "*DTAQ read via JT400 DataQueue.read(" + waitSeconds + ") — RCVDTAQ equivalent. " +
            "z/OS eq: MQGET with wait interval / CICS READQ TS"
        ));
    }

    /**
     * GET /api/v1/eligibility
     * Demonstrates JT400 ProgramCall with EBCDIC parameter passing.
     * IBM i concept: CALL PGM(*PGM) PARM(EBCDIC-encoded values)
     */
    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkEligibility(
            @RequestParam String portfolioId,
            @RequestParam String isin) {
        Map<String, String> result = programCallService.checkEligibility(portfolioId, isin);
        return ResponseEntity.ok(new ApiResponse<>(
            result,
            "JT400 ProgramCall — calling *PGM with AS400Text EBCDIC conversion. " +
            "Parameters passed as EBCDIC CHAR fields (CCSID 37). " +
            "z/OS eq: EXEC CICS LINK PROGRAM('CPECHKR') COMMAREA(data)"
        ));
    }

    /**
     * GET /api/v1/job-info
     * Returns IBM i job information for the current session.
     * IBM i concept: QUSRJOBI system API — job name/user/number triple
     */
    @GetMapping("/job-info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getJobInfo() {
        Map<String, String> info = programCallService.getJobInfo();
        return ResponseEntity.ok(new ApiResponse<>(
            info,
            "QUSRJOBI system API via JT400 Job class. " +
            "Job identity = name/user/number triple (unique on IBM i). " +
            "z/OS eq: EXEC CICS INQUIRE TASK / DISPLAY JOBS JCL"
        ));
    }

    /**
     * GET /api/v1/system/ping
     * Executes a CL command (DSPLIBL) and confirms IBM i connectivity.
     * IBM i concept: CommandCall = QCMDEXC from Java
     */
    @GetMapping("/system/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        boolean ok = commandExecutorService.execute("DSPLIBL OUTPUT(*PRINT)");
        return ResponseEntity.ok(new ApiResponse<>(
            ok ? "IBM i connection alive — PUB400 responding" : "Connection check failed",
            "CommandCall.run('DSPLIBL') — QCMDEXC from Java. " +
            "z/OS eq: EXEC CICS LINK to QCMDEXC utility"
        ));
    }
}
```

---

## 13. Domain Models

`src/main/java/com/example/ibmi/model/Portfolio.java`

```java
package com.example.ibmi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portfolio — domain model for CODELIVER1.PORTFOLIO physical file.
 * IBM i concept: Physical File (PF) — DB2 for i base table, no tablespace.
 * z/OS eq:       VSAM KSDS or DB2 z/OS base tablespace table.
 */
public class Portfolio {

    private String     portfolioId;   // PORTF_ID CHAR(10) — primary key
    private String     owner;         // OWNER    CHAR(40)
    private String     currency;      // CURRENCY CHAR(3) — USD/EUR/CHF/GBP
    private BigDecimal totalValue;    // TOTAL_VALUE DECIMAL(15,2)
    private String     status;        // STATUS CHAR(1) — A=Active, I=Inactive
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate  lastUpd;       // LAST_UPD DATE

    // ── Getters and setters ──────────────────────────────────────────────────
    public String     getPortfId()              { return portfolioId; }
    public void       setPortfId(String v)      { this.portfolioId = v; }
    public String     getOwner()                { return owner; }
    public void       setOwner(String v)        { this.owner = v; }
    public String     getCurrency()             { return currency; }
    public void       setCurrency(String v)     { this.currency = v; }
    public BigDecimal getTotalValue()           { return totalValue; }
    public void       setTotalValue(BigDecimal v){ this.totalValue = v; }
    public String     getStatus()               { return status; }
    public void       setStatus(String v)       { this.status = v; }
    public LocalDate  getLastUpd()              { return lastUpd; }
    public void       setLastUpd(LocalDate v)   { this.lastUpd = v; }
}
```

`src/main/java/com/example/ibmi/model/TradeOrder.java`

```java
package com.example.ibmi.model;

import java.math.BigDecimal;

/**
 * TradeOrder — domain model for CODELIVER1.TRADE_ORDERS physical file.
 * IBM i concept: Physical File (PF) with FK referencing PORTFOLIO.
 * Status lifecycle: PEND → PROC → SETL → FAIL
 * z/OS eq:         DB2 z/OS table with referential integrity constraint.
 */
public class TradeOrder {

    private String     orderId;    // ORDER_ID  CHAR(20) — primary key
    private String     portfId;    // PORTF_ID  CHAR(10) — FK to PORTFOLIO
    private String     isin;       // ISIN      CHAR(12) — security identifier
    private BigDecimal quantity;   // QUANTITY  DECIMAL(15,4)
    private BigDecimal price;      // PRICE     DECIMAL(15,4)
    private String     status;     // STATUS    CHAR(4) — PEND/PROC/SETL/FAIL

    // ── Getters and setters ──────────────────────────────────────────────────
    public String     getOrderId()             { return orderId; }
    public void       setOrderId(String v)     { this.orderId = v; }
    public String     getPortfId()             { return portfId; }
    public void       setPortfId(String v)     { this.portfId = v; }
    public String     getIsin()                { return isin; }
    public void       setIsin(String v)        { this.isin = v; }
    public BigDecimal getQuantity()            { return quantity; }
    public void       setQuantity(BigDecimal v){ this.quantity = v; }
    public BigDecimal getPrice()               { return price; }
    public void       setPrice(BigDecimal v)   { this.price = v; }
    public String     getStatus()              { return status; }
    public void       setStatus(String v)      { this.status = v; }
}
```

`src/main/java/com/example/ibmi/model/ApiResponse.java`

```java
package com.example.ibmi.model;

/**
 * ApiResponse — wrapper for all REST responses.
 * The ibmiConcept field explains the IBM i operation being performed
 * — making every endpoint self-documenting for interview demonstrations.
 */
public class ApiResponse<T> {

    private T      data;
    private String ibmiConcept;

    public ApiResponse(T data, String ibmiConcept) {
        this.data        = data;
        this.ibmiConcept = ibmiConcept;
    }

    public T      getData()        { return data; }
    public String getIbmiConcept() { return ibmiConcept; }
}
```

`src/main/java/com/example/ibmi/IbmiApplication.java`

```java
package com.example.ibmi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IbmiApplication {
    public static void main(String[] args) {
        SpringApplication.run(IbmiApplication.class, args);
    }
}
```

---

## 14. Unit, Integration, and System Tests (AAA Format)

### 14.1 Unit Test: PortfolioRepository with H2 In-Memory DB

`src/test/java/com/example/ibmi/unit/PortfolioRepositoryUnitTest.java`

```java
package com.example.ibmi.unit;

import com.example.ibmi.db2i.PortfolioRepository;
import com.example.ibmi.model.Portfolio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PortfolioRepository using H2 in-memory database.
 * No IBM i connection required — runs in CI/CD without PUB400.
 * Pattern: Arrange → Act → Assert (AAA)
 */
@JdbcTest
@ActiveProfiles("test")
@Sql({"/schema-h2.sql", "/data-test.sql"})
@Import(PortfolioRepositoryUnitTest.TestConfig.class)
class PortfolioRepositoryUnitTest {

    @Configuration
    static class TestConfig {
        @Bean
        public String ibmiLibrary() { return ""; }   // H2 has no schema prefix
        @Bean
        public PortfolioRepository portfolioRepository(
                JdbcTemplate jdbc, String ibmiLibrary) {
            return new PortfolioRepository(jdbc, ibmiLibrary);
        }
    }

    @Autowired
    private PortfolioRepository repo;

    // ── TC-U-01: findById returns portfolio for known ID ─────────────────────
    @Test
    @DisplayName("TC-U-01: findById returns portfolio for existing PF001")
    void findById_knownId_returnsPortfolio() {
        // Arrange
        String portfolioId = "PF001";

        // Act
        Optional<Portfolio> result = repo.findById(portfolioId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getOwner()).containsIgnoringCase("Oliver");
        assertThat(result.get().getCurrency()).isEqualTo("USD");
        assertThat(result.get().getStatus()).isEqualTo("A");
    }

    // ── TC-U-02: findById returns empty for unknown ID ───────────────────────
    @Test
    @DisplayName("TC-U-02: findById returns empty Optional for non-existent ID")
    void findById_unknownId_returnsEmpty() {
        // Arrange
        String unknownId = "XXXXX";

        // Act
        Optional<Portfolio> result = repo.findById(unknownId);

        // Assert
        assertThat(result).isEmpty();
    }

    // ── TC-U-03: findAllActive returns only active portfolios ────────────────
    @Test
    @DisplayName("TC-U-03: findAllActive returns only STATUS=A portfolios")
    void findAllActive_returnsOnlyActivePortfolios() {
        // Arrange — seed data has 2 active (PF001, PF002) and 1 inactive (PF003)

        // Act
        List<Portfolio> result = repo.findAllActive();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Portfolio::getStatus)
            .containsOnly("A");
        assertThat(result).extracting(Portfolio::getPortfId)
            .doesNotContain("PF003");
    }

    // ── TC-U-04: updateValue updates TOTAL_VALUE correctly ──────────────────
    @Test
    @DisplayName("TC-U-04: updateValue persists new total value to DB")
    void updateValue_validPortfolio_updatesValue() {
        // Arrange
        String     portfolioId = "PF001";
        BigDecimal newValue    = new BigDecimal("200000.00");

        // Act
        boolean updated = repo.updateValue(portfolioId, newValue);

        // Assert
        assertThat(updated).isTrue();
        Optional<Portfolio> after = repo.findById(portfolioId);
        assertThat(after).isPresent();
        assertThat(after.get().getTotalValue())
            .isEqualByComparingTo(newValue);
    }

    // ── TC-U-05: updateValue returns false for non-existent portfolio ────────
    @Test
    @DisplayName("TC-U-05: updateValue returns false for non-existent portfolio")
    void updateValue_unknownPortfolio_returnsFalse() {
        // Arrange
        String     unknownId = "ZZZZ";
        BigDecimal anyValue  = new BigDecimal("1000.00");

        // Act
        boolean updated = repo.updateValue(unknownId, anyValue);

        // Assert
        assertThat(updated).isFalse();
    }
}
```

### 14.2 Unit Test: DataQueueService with Mocked AS400

`src/test/java/com/example/ibmi/unit/DataQueueServiceUnitTest.java`

```java
package com.example.ibmi.unit;

import com.example.ibmi.ibmi.DataQueueService;
import com.example.ibmi.model.TradeOrder;
import com.ibm.as400.access.AS400;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataQueueService.
 * Mocks the AS400 object so no IBM i connection is needed.
 * Pattern: Arrange → Act → Assert (AAA)
 */
@ExtendWith(MockitoExtension.class)
class DataQueueServiceUnitTest {

    @Mock
    private AS400 mockAs400;

    private DataQueueService service;

    @BeforeEach
    void setUp() {
        service = new DataQueueService(mockAs400, "CODELIVER1", "ORDERQ");
    }

    // ── TC-U-10: enqueueOrder does not throw for valid order ─────────────────
    @Test
    @DisplayName("TC-U-10: enqueueOrder handles AS400 connection failure gracefully")
    void enqueueOrder_connectionFails_returnsFalse() {
        // Arrange
        TradeOrder order = new TradeOrder();
        order.setOrderId("ORD-TEST-001");
        order.setPortfId("PF001");
        order.setIsin("TSH000000001");
        order.setQuantity(new BigDecimal("100"));
        order.setPrice(new BigDecimal("182.50"));
        order.setStatus("PEND");

        // Act — AS400 is mocked, DataQueue.write() will throw
        boolean result = service.enqueueOrder(order);

        // Assert — service must not throw, must return false gracefully
        assertThat(result).isFalse();
    }

    // ── TC-U-11: dequeueOrder returns empty on timeout ───────────────────────
    @Test
    @DisplayName("TC-U-11: dequeueOrder returns empty Optional on connection failure")
    void dequeueOrder_connectionFails_returnsEmpty() {
        // Arrange — mocked AS400 will cause DataQueue.read() to fail

        // Act
        var result = service.dequeueOrder(1);

        // Assert
        assertThat(result).isEmpty();
    }
}
```

### 14.3 Integration Test: PortfolioRepository against Live PUB400

`src/test/java/com/example/ibmi/integration/PortfolioRepositoryIntegrationTest.java`

```java
package com.example.ibmi.integration;

import com.example.ibmi.db2i.PortfolioRepository;
import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PortfolioRepository against live PUB400 IBM i.
 * Requires: IBMI_HOST, IBMI_USER, IBMI_PASSWORD env vars set.
 * Requires: CODELIVER1.PORTFOLIO table exists (DOC 1 Layer 1 complete).
 * Run with: mvn test -Pintegration
 * Pattern: Arrange → Act → Assert (AAA)
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:as400://pub400.com/CODELIVER1;naming=sql",
    "spring.datasource.driver-class-name=com.ibm.as400.access.AS400JDBCDriver"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PortfolioRepositoryIntegrationTest {

    @Autowired
    private PortfolioRepository repo;

    // ── TC-I-01: findById returns PF001 from live DB2 for i ─────────────────
    @Test
    @Order(1)
    @DisplayName("TC-I-01: findById reads PF001 from live CODELIVER1.PORTFOLIO on PUB400")
    void findById_liveDB2_returnsPF001() {
        // Arrange
        String portfolioId = "PF001";

        // Act
        Optional<Portfolio> result = repo.findById(portfolioId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getPortfId()).isEqualTo("PF001");
        assertThat(result.get().getCurrency()).isEqualTo("USD");
        assertThat(result.get().getStatus()).isEqualTo("A");
        assertThat(result.get().getTotalValue()).isGreaterThan(BigDecimal.ZERO);
    }

    // ── TC-I-02: findAllActive returns only STATUS=A rows from live view ─────
    @Test
    @Order(2)
    @DisplayName("TC-I-02: findAllActive reads ACTIVE_PORTFOLIOS view on live PUB400")
    void findAllActive_liveDB2_returnsOnlyActive() {
        // Arrange — PF003 is STATUS='I', should not appear

        // Act
        List<Portfolio> portfolios = repo.findAllActive();

        // Assert
        assertThat(portfolios).isNotEmpty();
        assertThat(portfolios).allMatch(p -> "A".equals(p.getStatus()));
        assertThat(portfolios).extracting(Portfolio::getPortfId)
            .doesNotContain("PF003");
    }

    // ── TC-I-03: findPendingOrders returns PEND orders ───────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-I-03: findPendingOrders reads PEND orders from TRADE_ORDERS")
    void findPendingOrders_liveDB2_returnsPendingOrders() {
        // Arrange — seed data has 3 PEND orders (reset in system test if needed)

        // Act
        List<TradeOrder> orders = repo.findPendingOrders();

        // Assert — may be 0 if STEST01 or system test already processed them
        assertThat(orders).allMatch(o -> "PEND".equals(o.getStatus()));
    }

    // ── TC-I-04: updateValue persists to live DB2 and is readable back ───────
    @Test
    @Order(4)
    @DisplayName("TC-I-04: updateValue persists to live DB2 for i with @Transactional")
    void updateValue_liveDB2_persistsValue() {
        // Arrange
        String     portfolioId  = "PF001";
        BigDecimal originalValue = repo.findById(portfolioId)
            .map(Portfolio::getTotalValue)
            .orElse(BigDecimal.ZERO);
        BigDecimal updatedValue  = originalValue.add(new BigDecimal("5000.00"));

        // Act
        boolean updated = repo.updateValue(portfolioId, updatedValue);

        // Assert
        assertThat(updated).isTrue();
        Optional<Portfolio> after = repo.findById(portfolioId);
        assertThat(after).isPresent();
        assertThat(after.get().getTotalValue()).isEqualByComparingTo(updatedValue);

        // Restore original value (test cleanup)
        repo.updateValue(portfolioId, originalValue);
    }
}
```

### 14.4 System Test: Full REST API + IBM i End-to-End

`src/test/java/com/example/ibmi/system/BatchSettlementSystemTest.java`

```java
package com.example.ibmi.system;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * System tests: full HTTP → Spring Boot → JT400 → IBM i round-trips.
 * Exercises the entire stack: REST endpoint → service → DB2/JT400 → PUB400.
 * Requires all env vars and a live PUB400 connection.
 * Run with: mvn test -Pintegration
 * Pattern: Arrange → Act → Assert (AAA)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BatchSettlementSystemTest {

    @Autowired
    private MockMvc mockMvc;

    // ── TC-S-01: GET /system/ping responds with 200 and IBM i concept ────────
    @Test
    @Order(1)
    @DisplayName("TC-S-01: /system/ping confirms IBM i connectivity end-to-end")
    void ping_liveIBMi_returns200() throws Exception {
        // Arrange — application started with live PUB400 connection

        // Act + Assert
        mockMvc.perform(get("/api/v1/system/ping"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ibmiConcept").exists())
               .andExpect(jsonPath("$.data").value(
                   org.hamcrest.Matchers.containsString("IBM i connection alive")));
    }

    // ── TC-S-02: GET /portfolios returns active portfolios from PUB400 ────────
    @Test
    @Order(2)
    @DisplayName("TC-S-02: /portfolios returns active portfolios via ACTIVE_PORTFOLIOS view")
    void getAllPortfolios_liveIBMi_returnsActivePortfolios() throws Exception {
        // Arrange — ACTIVE_PORTFOLIOS view must exist (DOC 1 Layer 1)

        // Act + Assert
        MvcResult result = mockMvc.perform(get("/api/v1/portfolios"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ibmiConcept").exists())
               .andExpect(jsonPath("$.data").isArray())
               .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("PF001");
        assertThat(body).contains("USD");
        assertThat(body).doesNotContain("PF003");   // Inactive — should be filtered by view
    }

    // ── TC-S-03: GET /portfolios/{id} returns PF001 ──────────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-S-03: /portfolios/PF001 returns single portfolio via keyed DB2 read")
    void getPortfolioById_PF001_returns200() throws Exception {
        // Arrange
        String portfolioId = "PF001";

        // Act + Assert
        mockMvc.perform(get("/api/v1/portfolios/" + portfolioId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.portfolioId").value("PF001"))
               .andExpect(jsonPath("$.data.currency").value("USD"))
               .andExpect(jsonPath("$.ibmiConcept").value(
                   org.hamcrest.Matchers.containsString("CHAIN")));
    }

    // ── TC-S-04: POST /orders/enqueue + GET /orders/dequeue round-trip ───────
    @Test
    @Order(4)
    @DisplayName("TC-S-04: enqueue + dequeue round-trip via IBM i *DTAQ")
    void enqueueDequeue_roundTrip_succeeds() throws Exception {
        // Arrange
        String orderJson = """
            {
              "orderId":  "ORD-SYSTEST-001",
              "portfId":  "PF001",
              "isin":     "TSH000000001",
              "quantity": 10,
              "price":    182.50,
              "status":   "PEND"
            }
            """;

        // Act — enqueue
        mockMvc.perform(post("/api/v1/orders/enqueue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ibmiConcept").value(
                   org.hamcrest.Matchers.containsString("SNDDTAQ")));

        // Act — dequeue (wait up to 10 seconds)
        MvcResult dequeueResult =
            mockMvc.perform(get("/api/v1/orders/dequeue?waitSeconds=10"))
                   .andExpect(status().isOk())
                   .andReturn();

        // Assert
        String body = dequeueResult.getResponse().getContentAsString();
        assertThat(body).contains("ORD-SYSTEST-001");
        assertThat(body).contains("RCVDTAQ");
    }

    // ── TC-S-05: GET /job-info returns real IBM i job details ────────────────
    @Test
    @Order(5)
    @DisplayName("TC-S-05: /job-info returns real IBM i QUSRJOBI job name/user/number")
    void getJobInfo_liveIBMi_returnsJobDetails() throws Exception {
        // Arrange — requires live PUB400 connection

        // Act + Assert
        mockMvc.perform(get("/api/v1/job-info"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.jobName").exists())
               .andExpect(jsonPath("$.data.jobUser").exists())
               .andExpect(jsonPath("$.data.jobNumber").exists())
               .andExpect(jsonPath("$.ibmiConcept").value(
                   org.hamcrest.Matchers.containsString("QUSRJOBI")));
    }
}
```

---

## 15. Running the Application Locally

### 15.1 Build (first time — downloads all dependencies)

```bash
cd ~/projects/ibmi-batch-simulator
mvn clean install -DskipTests
# Expected: BUILD SUCCESS
```

### 15.2 Run Unit Tests Only (no IBM i required)

```bash
mvn test
# Runs: PortfolioRepositoryUnitTest, DataQueueServiceUnitTest
# Expected: Tests run: 7, Failures: 0, Errors: 0
```

### 15.3 Run Integration + System Tests (requires live PUB400)

```bash
# Ensure env vars are set:
echo $IBMI_HOST      # pub400.com
echo $IBMI_USER      # CODELIVER
echo $IBMI_PASSWORD  # (your password)

mvn test -Pintegration
# Runs ALL tests including integration and system tests
```

### 15.4 Start the Application

```bash
mvn spring-boot:run
# OR
java -jar target/ibmi-batch-simulator-2.0.0.jar
```

Expected startup output:

```
Started IbmiApplication in 4.3 seconds
Tomcat started on port(s): 8080
```

### 15.5 Test from VS Code Terminal (curl)

```bash
# Ping IBM i
curl -s http://localhost:8080/api/v1/system/ping | python3 -m json.tool

# Get all active portfolios
curl -s http://localhost:8080/api/v1/portfolios | python3 -m json.tool

# Get portfolio by ID
curl -s http://localhost:8080/api/v1/portfolios/PF001 | python3 -m json.tool

# Get job info
curl -s http://localhost:8080/api/v1/job-info | python3 -m json.tool
```

---

## 16. Manual API Tests with REST Client (.http file)

`src/test/http/ibmi-tests.http`

Install the VS Code REST Client extension, then click **Send Request** above each block:

```http
### 1. Ping IBM i — confirms JT400 connection to PUB400
GET http://localhost:8080/api/v1/system/ping
Accept: application/json

###

### 2. Get all active portfolios (ACTIVE_PORTFOLIOS view)
GET http://localhost:8080/api/v1/portfolios
Accept: application/json

###

### 3. Get portfolio PF001 by ID (keyed DB2 read — CHAIN equivalent)
GET http://localhost:8080/api/v1/portfolios/PF001
Accept: application/json

###

### 4. Get portfolio PF002
GET http://localhost:8080/api/v1/portfolios/PF002
Accept: application/json

###

### 5. Update portfolio PF001 total value
PUT http://localhost:8080/api/v1/portfolios/PF001/value?newValue=175000.00
Accept: application/json

###

### 6. Get all pending trade orders
GET http://localhost:8080/api/v1/orders/pending
Accept: application/json

###

### 7. Enqueue a trade order to IBM i *DTAQ
POST http://localhost:8080/api/v1/orders/enqueue
Content-Type: application/json

{
  "orderId":  "ORD-2026-HTTP-001",
  "portfId":  "PF001",
  "isin":     "TSH000000001",
  "quantity": 25,
  "price":    182.50,
  "status":   "PEND"
}

###

### 8. Dequeue next order from IBM i *DTAQ (wait 5 seconds)
GET http://localhost:8080/api/v1/orders/dequeue?waitSeconds=5
Accept: application/json

###

### 9. Get IBM i job info (QUSRJOBI system API)
GET http://localhost:8080/api/v1/job-info
Accept: application/json

###

### 10. Check eligibility (ProgramCall with EBCDIC params)
GET http://localhost:8080/api/v1/eligibility?portfolioId=PF001&isin=TSH000000001
Accept: application/json
```

---

*End of DOC 2 — Layer 3 & Layer 4: Java Spring Boot + JT400 REST API*  
*Next: DOC 3 — Repository Setup, Git Workflow, and Project Tie-Together*
