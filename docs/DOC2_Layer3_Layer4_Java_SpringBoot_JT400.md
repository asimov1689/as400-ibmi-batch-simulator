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
7. [Config — IbmiConnectionConfig, CacheConfig, OpenApiConfig](#7-config--ibmiconnectionconfig-cacheconfig-openapiconfig)
8. [IBM i Services — CommandExecutor, DataQueue, ProgramCall](#8-ibm-i-services--commandexecutor-dataqueue-programcall)
9. [Repository — PortfolioRepository (DB2 for i JDBC)](#9-repository--portfoliorepository-db2-for-i-jdbc)
10. [Service — PortfolioService (Orchestration + Caching)](#10-service--portfolioservice-orchestration--caching)
11. [DTOs — ApiResponse, PortfolioDto, TradeOrderDto, EnqueueRequest](#11-dtos--apiresponse-portfoliodto-tradeorderdto-enqueuerequest)
12. [Domain Models — Portfolio, TradeOrder](#12-domain-models--portfolio-tradeorder)
13. [Controller — PortfolioController (REST API + OpenAPI)](#13-controller--portfoliocontroller-rest-api--openapi)
14. [Unit, Integration, and System Tests (AAA Format)](#14-unit-integration-and-system-tests-aaa-format)
15. [Running the Application Locally](#15-running-the-application-locally)
16. [OpenAPI / Swagger UI](#16-openapi--swagger-ui)
17. [Code Formatting — Spotless](#17-code-formatting--spotless)
18. [Manual API Tests with REST Client (.http file)](#18-manual-api-tests-with-rest-client-http-file)

---

## 1. What You Are Building

Layer 3 and Layer 4 are the Java side of the project. A Spring Boot application runs on your **local macOS machine** and connects to the live IBM i (PUB400) using the JT400 library (IBM Toolbox for Java).

The architecture follows proper Spring Boot layering:

```
LAYER 4 — REST API (Spring Boot Controller)
  PortfolioController    → 8 REST endpoints under /api/v1
      GET  /portfolios
      GET  /portfolios/{id}
      PUT  /portfolios/{id}/value
      GET  /orders/pending
      POST /orders/enqueue
      GET  /orders/dequeue
      GET  /eligibility
      GET  /job-info
      GET  /system/ping

SERVICE LAYER — Orchestration + Caching
  PortfolioService       → business logic, model-to-DTO mapping
                           @Cacheable on reads, @CacheEvict on writes

LAYER 3 — IBM i Integration Services (JT400)
  IbmiConnectionConfig   → AS400 connection bean
  CommandExecutorService → CL command runner (QCMDEXC)
  DataQueueService       → *DTAQ write/read (SNDDTAQ/RCVDTAQ)
  ProgramCallService     → *PGM caller (EBCDIC parameter handling)

REPOSITORY LAYER — DB2 for i Data Access
  PortfolioRepository    → DB2 for i JDBC (CHAIN/READ/UPDATE)

DTO LAYER — API contracts
  ApiResponse<T>         → standard wrapper with ibmiConcept field
  PortfolioDto           → portfolio data transfer object
  TradeOrderDto          → trade order data transfer object
  EnqueueRequest         → inbound enqueue request body

CROSS-CUTTING
  CacheConfig            → Caffeine in-memory cache (5-min TTL)
  OpenApiConfig          → Swagger UI + OpenAPI 3.0 spec
  Spotless               → Google Java Format (AOSP) auto-formatter
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
```

### 3.2 Install REST Client Extension (for .http files)

```bash
code --install-extension humao.rest-client
```

### 3.3 Open the Project Folder in VS Code

```bash
cd ~/projects/ibmi-batch-simulator
code .
```

---

## 4. Project Scaffold — Maven Structure

```
ibmi-batch-simulator/
├── pom.xml
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/example/ibmi/
│   │   │   ├── IbmiApplication.java
│   │   │   ├── config/
│   │   │   │   ├── IbmiConnectionConfig.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/
│   │   │   │   └── PortfolioController.java
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── EnqueueRequest.java
│   │   │   │   ├── PortfolioDto.java
│   │   │   │   └── TradeOrderDto.java
│   │   │   ├── model/
│   │   │   │   ├── Portfolio.java
│   │   │   │   └── TradeOrder.java
│   │   │   ├── repository/
│   │   │   │   └── PortfolioRepository.java
│   │   │   └── service/
│   │   │       ├── PortfolioService.java
│   │   │       └── ibmi/
│   │   │           ├── CommandExecutorService.java
│   │   │           ├── DataQueueService.java
│   │   │           └── ProgramCallService.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/com/example/ibmi/
│       │   ├── unit/
│       │   │   ├── PortfolioRepositoryUnitTest.java
│       │   │   ├── DataQueueServiceUnitTest.java
│       │   │   ├── PortfolioServiceUnitTest.java
│       │   │   └── PortfolioControllerUnitTest.java
│       │   ├── integration/
│       │   │   ├── PortfolioRepositoryIntegrationTest.java
│       │   │   ├── ProgramCallIntegrationTest.java
│       │   │   └── JournalingSetupTest.java
│       │   └── system/
│       │       └── BatchSettlementSystemTest.java
│       ├── resources/
│       │   ├── application-test.yml
│       │   ├── schema-h2.sql
│       │   └── data-test.sql
│       └── http/
│           └── ibmi-tests.http
├── docs/
│   ├── DOC1_Layer1_IBMi_Native_Development.md
│   └── DOC2_Layer3_Layer4_Java_SpringBoot_JT400.md
└── src/ibmi/                          ← IBM i native source (DOC 1)
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

    <!-- Springdoc OpenAPI — auto-generated OpenAPI 3.0 spec + Swagger UI -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.5.0</version>
    </dependency>

    <!-- Spring Boot Cache + Caffeine — in-memory caching -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
      <groupId>com.github.ben-manes.caffeine</groupId>
      <artifactId>caffeine</artifactId>
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

    <!-- H2 — in-memory database for unit tests -->
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>

  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <!-- Spotless — auto-format Java source (Google Java Style) -->
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>2.43.0</version>
        <configuration>
          <java>
            <googleJavaFormat>
              <version>1.22.0</version>
              <style>AOSP</style>
            </googleJavaFormat>
            <removeUnusedImports/>
            <trimTrailingWhitespace/>
            <endWithNewline/>
          </java>
        </configuration>
      </plugin>
      <!-- Surefire — runs JUnit 5 tests -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
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

`src/main/resources/application.yml` — **safe to commit** (all credentials from env vars):

```yaml
# IBM i connection — all credentials from environment variables
ibmi:
  host:     ${IBMI_HOST}
  user:     ${IBMI_USER}
  password: ${IBMI_PASSWORD}
  library:  ${IBMI_LIBRARY}
  order-queue: ORDERQ

# Spring JDBC datasource — DB2 for i via AS400JDBCDriver (JT400)
spring:
  datasource:
    url:                   jdbc:as400://${IBMI_HOST}/${IBMI_LIBRARY};naming=sql;errors=full;commit=none
    username:              ${IBMI_USER}
    password:              ${IBMI_PASSWORD}
    driver-class-name:     com.ibm.as400.access.AS400JDBCDriver
    hikari:
      maximum-pool-size:   5
      minimum-idle:        1
      connection-timeout:  30000
      idle-timeout:        300000
      auto-commit:         true

# Server
server:
  port: 8080

# Logging
logging:
  level:
    com.example.ibmi: DEBUG
    com.ibm.as400:    WARN
```

**Security note:** Zero hardcoded hostnames, usernames, or passwords. All connection details come from `IBMI_*` environment variables. The JDBC URL uses `commit=none` because PUB400 tables with foreign-key constraints require journaling for commitment control; `auto-commit=true` ensures updates work without server-side journaling.

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
      mode: never
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

CREATE OR REPLACE VIEW ACTIVE_PORTFOLIOS AS
    SELECT * FROM PORTFOLIO
    WHERE STATUS = 'A';
```

`src/test/resources/data-test.sql` — seed data for unit tests:

```sql
INSERT INTO PORTFOLIO VALUES ('PF001     ', 'Richard Papen                           ', 'USD', 150000.00, 'A', CURRENT_DATE);
INSERT INTO PORTFOLIO VALUES ('PF002     ', 'Henry Winter                            ', 'CHF', 280000.00, 'A', CURRENT_DATE);
INSERT INTO PORTFOLIO VALUES ('PF003     ', 'Camilla Macaulay                        ', 'EUR',  95000.00, 'I', CURRENT_DATE);
INSERT INTO TRADE_ORDERS VALUES ('ORD-2026-001        ', 'PF001     ', 'TSH000000001', 100, 182.50, CURRENT_DATE, NULL, 'PEND');
INSERT INTO TRADE_ORDERS VALUES ('ORD-2026-002        ', 'PF001     ', 'TSH000000002',  50, 312.00, CURRENT_DATE, NULL, 'PEND');
INSERT INTO TRADE_ORDERS VALUES ('ORD-2026-003        ', 'PF002     ', 'TSH000000003', 200,  45.75, CURRENT_DATE, NULL, 'PEND');
```

---

## 7. Config — IbmiConnectionConfig, CacheConfig, OpenApiConfig

### 7.1 IbmiConnectionConfig.java

`src/main/java/com/example/ibmi/config/IbmiConnectionConfig.java`

```java
package com.example.ibmi.config;

import com.ibm.as400.access.AS400;
import java.beans.PropertyVetoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public AS400 as400() throws PropertyVetoException {
        AS400 as400 = new AS400(host, user, password);
        as400.setGuiAvailable(false);
        return as400;
    }

    @Bean
    public String ibmiLibrary() {
        return library;
    }
}
```

IBM i concept: `AS400` object = the JT400 entry point for all IBM i operations. `setGuiAvailable(false)` prevents the 5250 sign-on dialog in server-side mode. z/OS equivalent: CICS connection factory / MQ QueueManager connection object.

### 7.2 CacheConfig.java

`src/main/java/com/example/ibmi/config/CacheConfig.java`

```java
package com.example.ibmi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("portfolios", "pendingOrders");
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats());
        return manager;
    }
}
```

Two named caches: `portfolios` (for portfolio reads) and `pendingOrders` (for pending order list). Entries expire after 5 minutes. `recordStats()` enables hit/miss metrics.

### 7.3 OpenApiConfig.java

`src/main/java/com/example/ibmi/config/OpenApiConfig.java`

```java
package com.example.ibmi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ibmiOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("IBM i Portfolio Management API")
                                .version("2.0.0")
                                .description(
                                        "REST API layer over IBM i native programs via JT400 (IBM Toolbox for Java). "
                                                + "Demonstrates DB2 for i JDBC, *DTAQ (Data Queue), *PGM (Program Call), "
                                                + "and CL command execution against a live IBM i system.")
                                .contact(new Contact().name("Oliver Jaramillo")))
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:8080")
                                        .description("Local development")));
    }
}
```

Provides API metadata for the Swagger UI at `/swagger-ui.html` and the OpenAPI spec at `/v3/api-docs`.

---

## 8. IBM i Services — CommandExecutor, DataQueue, ProgramCall

### 8.1 CommandExecutorService.java

`src/main/java/com/example/ibmi/service/ibmi/CommandExecutorService.java`

Wraps JT400 `CommandCall` to execute any CL command string from Java. IBM i concept: `CommandCall` = Java equivalent of calling `QCMDEXC` API from RPG. z/OS equivalent: EXEC CICS LINK to a utility program that runs QCMDEXC.

```java
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
```

### 8.2 DataQueueService.java

`src/main/java/com/example/ibmi/service/ibmi/DataQueueService.java`

Produces and consumes IBM i Data Queues (`*DTAQ`) using JT400 `DataQueue` class. IBM i concept: `*DTAQ` = FIFO message channel between IBM i jobs. CL: `CRTDTAQ` -> `SNDDTAQ` -> `RCVDTAQ`. z/OS equivalent: IBM MQ or CICS Temporary Storage Queue.

```java
package com.example.ibmi.service.ibmi;

import com.example.ibmi.model.TradeOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataQueueService {

    private static final Logger log = LoggerFactory.getLogger(DataQueueService.class);
    private static final int ENTRY_LENGTH = 200;

    private final AS400 as400;
    private final String library;
    private final String queueName;
    private final ObjectMapper mapper;

    public DataQueueService(
            AS400 as400,
            String ibmiLibrary,
            @Value("${ibmi.order-queue:ORDERQ}") String queueName) {
        this.as400 = as400;
        this.library = ibmiLibrary;
        this.queueName = queueName;
        this.mapper = new ObjectMapper();
    }

    public boolean enqueueOrder(TradeOrder order) {
        try {
            DataQueue queue =
                    new DataQueue(
                            as400, String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));

            String json = mapper.writeValueAsString(order);
            byte[] payload = new byte[ENTRY_LENGTH];
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(jsonBytes, 0, payload, 0, Math.min(jsonBytes.length, ENTRY_LENGTH));

            queue.write(payload);
            log.debug("Enqueued order {} to *DTAQ {}", order.getOrderId(), queueName);
            return true;
        } catch (Exception e) {
            log.error("Failed to enqueue order to *DTAQ {}: {}", queueName, e.getMessage());
            return false;
        }
    }

    public Optional<TradeOrder> dequeueOrder(int waitSeconds) {
        try {
            DataQueue queue =
                    new DataQueue(
                            as400, String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));

            DataQueueEntry entry = queue.read(waitSeconds);
            if (entry == null) {
                return Optional.empty();
            }

            byte[] raw = entry.getData();
            int len = raw.length;
            while (len > 0 && raw[len - 1] == 0) len--;
            String json = new String(Arrays.copyOf(raw, len), StandardCharsets.UTF_8).trim();

            TradeOrder order = mapper.readValue(json, TradeOrder.class);
            log.debug("Dequeued order {} from *DTAQ {}", order.getOrderId(), queueName);
            return Optional.of(order);
        } catch (Exception e) {
            log.error("Failed to dequeue from *DTAQ {}: {}", queueName, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean createQueueIfAbsent() {
        try {
            DataQueue queue =
                    new DataQueue(
                            as400, String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));
            queue.create(ENTRY_LENGTH);
            log.info("Created *DTAQ {}/{}", library, queueName);
            return true;
        } catch (Exception e) {
            log.debug("*DTAQ {}/{} may already exist: {}", library, queueName, e.getMessage());
            return false;
        }
    }
}
```

### 8.3 ProgramCallService.java

`src/main/java/com/example/ibmi/service/ibmi/ProgramCallService.java`

Calls IBM i `*PGM` objects from Java using JT400 `ProgramCall`. IBM i concept: `ProgramCall` = Java equivalent of `CALL PGM()` in CL. `AS400Text(n,37,as400)` = EBCDIC CHAR(n) converter. z/OS equivalent: EXEC CICS LINK or COBOL CALL.

```java
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
```

---

## 9. Repository — PortfolioRepository (DB2 for i JDBC)

`src/main/java/com/example/ibmi/repository/PortfolioRepository.java`

Uses Spring `JdbcTemplate` with `AS400JDBCDriver` (from JT400) to access DB2 for i. The `table()` helper method prefixes the library name for IBM i or leaves it bare for H2 unit tests.

IBM i concepts demonstrated:
- `findById()` — keyed direct read (CHAIN opcode equivalent)
- `findAllActive()` — sequential read loop (READ opcode equivalent)
- `updateValue()` — DB2 for i UPDATE with JDBC auto-commit
- `findPendingOrders()` — `SELECT WHERE STATUS='PEND'` with ordering

```java
package com.example.ibmi.repository;

import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioRepository {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRepository.class);

    private final JdbcTemplate jdbc;
    private final String library;

    public PortfolioRepository(JdbcTemplate jdbcTemplate, String ibmiLibrary) {
        this.jdbc = jdbcTemplate;
        this.library = (ibmiLibrary == null || ibmiLibrary.isBlank()) ? "" : ibmiLibrary;
    }

    private String table(String name) {
        return library.isEmpty() ? name : library + "." + name;
    }

    private final RowMapper<Portfolio> portfolioMapper =
            (rs, rowNum) -> {
                Portfolio p = new Portfolio();
                p.setPortfId(rs.getString("PORTF_ID").trim());
                p.setOwner(rs.getString("OWNER").trim());
                p.setCurrency(rs.getString("CURRENCY").trim());
                p.setTotalValue(rs.getBigDecimal("TOTAL_VALUE"));
                p.setStatus(rs.getString("STATUS").trim());
                p.setLastUpd(
                        rs.getDate("LAST_UPD") != null
                                ? rs.getDate("LAST_UPD").toLocalDate()
                                : null);
                return p;
            };

    private final RowMapper<TradeOrder> orderMapper =
            (rs, rowNum) -> {
                TradeOrder o = new TradeOrder();
                o.setOrderId(rs.getString("ORDER_ID").trim());
                o.setPortfId(rs.getString("PORTF_ID").trim());
                o.setIsin(rs.getString("ISIN").trim());
                o.setQuantity(rs.getBigDecimal("QUANTITY"));
                o.setPrice(rs.getBigDecimal("PRICE"));
                o.setStatus(rs.getString("STATUS").trim());
                return o;
            };

    public Optional<Portfolio> findById(String portfolioId) {
        String sql =
                "SELECT PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD "
                        + "FROM " + table("PORTFOLIO") + " WHERE PORTF_ID = ?";
        try {
            List<Portfolio> rows =
                    jdbc.query(sql, portfolioMapper, String.format("%-10s", portfolioId));
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            log.error("findById failed for {}: {}", portfolioId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Portfolio> findAllActive() {
        String sql =
                "SELECT PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD "
                        + "FROM " + table("ACTIVE_PORTFOLIOS") + " ORDER BY PORTF_ID";
        return jdbc.query(sql, portfolioMapper);
    }

    public boolean updateValue(String portfolioId, BigDecimal newValue) {
        String sql = "UPDATE " + table("PORTFOLIO")
                + " SET TOTAL_VALUE = ?, LAST_UPD = CURRENT_DATE WHERE PORTF_ID = ?";
        int rows = jdbc.update(sql, newValue, String.format("%-10s", portfolioId));
        return rows > 0;
    }

    public List<TradeOrder> findPendingOrders() {
        String sql =
                "SELECT ORDER_ID, PORTF_ID, ISIN, QUANTITY, PRICE, STATUS "
                        + "FROM " + table("TRADE_ORDERS")
                        + " WHERE STATUS = 'PEND' ORDER BY ORDER_DT, ORDER_ID";
        return jdbc.query(sql, orderMapper);
    }

    public boolean processOrder(String orderId) {
        String sql = "UPDATE " + table("TRADE_ORDERS")
                + " SET STATUS = 'PROC', PROCESS_DT = CURRENT_DATE "
                + "WHERE ORDER_ID = ? AND STATUS = 'PEND'";
        int rows = jdbc.update(sql, String.format("%-20s", orderId));
        return rows > 0;
    }
}
```

---

## 10. Service — PortfolioService (Orchestration + Caching)

`src/main/java/com/example/ibmi/service/PortfolioService.java`

The service layer sits between the controller and the repository/IBM i services. It handles:
- Model-to-DTO mapping (domain objects never leak to the API)
- Caffeine caching with `@Cacheable` and `@CacheEvict`
- Delegation to the correct IBM i service

```java
package com.example.ibmi.service;

import com.example.ibmi.dto.EnqueueRequest;
import com.example.ibmi.dto.PortfolioDto;
import com.example.ibmi.dto.TradeOrderDto;
import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import com.example.ibmi.repository.PortfolioRepository;
import com.example.ibmi.service.ibmi.CommandExecutorService;
import com.example.ibmi.service.ibmi.DataQueueService;
import com.example.ibmi.service.ibmi.ProgramCallService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepo;
    private final DataQueueService dataQueueService;
    private final ProgramCallService programCallService;
    private final CommandExecutorService commandExecutorService;

    public PortfolioService(
            PortfolioRepository portfolioRepo,
            DataQueueService dataQueueService,
            ProgramCallService programCallService,
            CommandExecutorService commandExecutorService) {
        this.portfolioRepo = portfolioRepo;
        this.dataQueueService = dataQueueService;
        this.programCallService = programCallService;
        this.commandExecutorService = commandExecutorService;
    }

    @Cacheable("portfolios")
    public List<PortfolioDto> getAllActivePortfolios() {
        return portfolioRepo.findAllActive().stream().map(this::toDto).toList();
    }

    @Cacheable(value = "portfolios", key = "#id")
    public Optional<PortfolioDto> getPortfolioById(String id) {
        return portfolioRepo.findById(id).map(this::toDto);
    }

    @CacheEvict(value = "portfolios", allEntries = true)
    public boolean updatePortfolioValue(String id, BigDecimal newValue) {
        return portfolioRepo.updateValue(id, newValue);
    }

    @Cacheable("pendingOrders")
    public List<TradeOrderDto> getPendingOrders() {
        return portfolioRepo.findPendingOrders().stream().map(this::toOrderDto).toList();
    }

    @CacheEvict(value = "pendingOrders", allEntries = true)
    public boolean enqueueOrder(EnqueueRequest request) {
        TradeOrder order = new TradeOrder();
        order.setOrderId(request.getOrderId());
        order.setPortfId(request.getPortfId());
        order.setIsin(request.getIsin());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus("PEND");
        return dataQueueService.enqueueOrder(order);
    }

    public Optional<TradeOrderDto> dequeueOrder(int waitSeconds) {
        return dataQueueService.dequeueOrder(waitSeconds).map(this::toOrderDto);
    }

    public Map<String, String> checkEligibility(String portfolioId, String isin) {
        return programCallService.checkEligibility(portfolioId, isin);
    }

    public Map<String, String> getJobInfo() {
        return programCallService.getJobInfo();
    }

    public boolean pingIbmi() {
        return commandExecutorService.execute("DSPLIBL OUTPUT(*PRINT)");
    }

    private PortfolioDto toDto(Portfolio p) {
        return new PortfolioDto(
                p.getPortfId(), p.getOwner(), p.getCurrency(),
                p.getTotalValue(), p.getStatus(), p.getLastUpd());
    }

    private TradeOrderDto toOrderDto(TradeOrder o) {
        return new TradeOrderDto(
                o.getOrderId(), o.getPortfId(), o.getIsin(),
                o.getQuantity(), o.getPrice(), o.getStatus());
    }
}
```

### Caching behaviour

| Method | Annotation | Behaviour |
|--------|-----------|-----------|
| `getAllActivePortfolios()` | `@Cacheable("portfolios")` | First call hits DB2; subsequent calls served from Caffeine for 5 min |
| `getPortfolioById(id)` | `@Cacheable(key = "#id")` | Cached per portfolio ID |
| `getPendingOrders()` | `@Cacheable("pendingOrders")` | Cached until an enqueue evicts it |
| `updatePortfolioValue()` | `@CacheEvict(allEntries)` | Clears portfolio cache so next read gets fresh data |
| `enqueueOrder()` | `@CacheEvict(allEntries)` | Clears pending orders cache |

---

## 11. DTOs — ApiResponse, PortfolioDto, TradeOrderDto, EnqueueRequest

### 11.1 ApiResponse.java

`src/main/java/com/example/ibmi/dto/ApiResponse.java`

```java
package com.example.ibmi.dto;

public class ApiResponse<T> {

    private T data;
    private String ibmiConcept;

    public ApiResponse(T data, String ibmiConcept) {
        this.data = data;
        this.ibmiConcept = ibmiConcept;
    }

    public T getData()              { return data; }
    public String getIbmiConcept()  { return ibmiConcept; }
}
```

### 11.2 PortfolioDto.java

`src/main/java/com/example/ibmi/dto/PortfolioDto.java`

```java
package com.example.ibmi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioDto {

    private String portfId;
    private String owner;
    private String currency;
    private BigDecimal totalValue;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastUpd;

    public PortfolioDto() {}

    public PortfolioDto(String portfId, String owner, String currency,
                        BigDecimal totalValue, String status, LocalDate lastUpd) {
        this.portfId = portfId;
        this.owner = owner;
        this.currency = currency;
        this.totalValue = totalValue;
        this.status = status;
        this.lastUpd = lastUpd;
    }

    // Getters and setters omitted for brevity — see source
}
```

### 11.3 TradeOrderDto.java

`src/main/java/com/example/ibmi/dto/TradeOrderDto.java`

```java
package com.example.ibmi.dto;

import java.math.BigDecimal;

public class TradeOrderDto {

    private String orderId;
    private String portfId;
    private String isin;
    private BigDecimal quantity;
    private BigDecimal price;
    private String status;

    public TradeOrderDto() {}

    public TradeOrderDto(String orderId, String portfId, String isin,
                         BigDecimal quantity, BigDecimal price, String status) {
        this.orderId = orderId;
        this.portfId = portfId;
        this.isin = isin;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    // Getters and setters omitted for brevity — see source
}
```

### 11.4 EnqueueRequest.java

`src/main/java/com/example/ibmi/dto/EnqueueRequest.java`

```java
package com.example.ibmi.dto;

import java.math.BigDecimal;

public class EnqueueRequest {

    private String orderId;
    private String portfId;
    private String isin;
    private BigDecimal quantity;
    private BigDecimal price;

    // Getters and setters omitted for brevity — see source
}
```

---

## 12. Domain Models — Portfolio, TradeOrder

### 12.1 Portfolio.java

`src/main/java/com/example/ibmi/model/Portfolio.java`

```java
package com.example.ibmi.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Portfolio {

    private String portfId;       // PORTF_ID CHAR(10) — primary key
    private String owner;         // OWNER    CHAR(40)
    private String currency;      // CURRENCY CHAR(3) — USD/EUR/CHF/GBP
    private BigDecimal totalValue;// TOTAL_VALUE DECIMAL(15,2)
    private String status;        // STATUS CHAR(1) — A=Active, I=Inactive
    private LocalDate lastUpd;    // LAST_UPD DATE

    // Getters and setters omitted for brevity — see source
}
```

IBM i concept: Physical File (PF) — DB2 for i base table. z/OS equivalent: VSAM KSDS or DB2 z/OS base tablespace table.

### 12.2 TradeOrder.java

`src/main/java/com/example/ibmi/model/TradeOrder.java`

```java
package com.example.ibmi.model;

import java.math.BigDecimal;

public class TradeOrder {

    private String orderId;    // ORDER_ID  CHAR(20) — primary key
    private String portfId;    // PORTF_ID  CHAR(10) — FK to PORTFOLIO
    private String isin;       // ISIN      CHAR(12) — security identifier
    private BigDecimal quantity;// QUANTITY  DECIMAL(15,4)
    private BigDecimal price;  // PRICE     DECIMAL(15,4)
    private String status;     // STATUS    CHAR(4) — PEND/PROC/SETL/FAIL

    // Getters and setters omitted for brevity — see source
}
```

### 12.3 IbmiApplication.java

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

## 13. Controller — PortfolioController (REST API + OpenAPI)

`src/main/java/com/example/ibmi/controller/PortfolioController.java`

The controller delegates to `PortfolioService` (not directly to repositories or IBM i services). Every endpoint is annotated with `@Operation` and `@Parameter` for OpenAPI/Swagger documentation.

```java
package com.example.ibmi.controller;

import com.example.ibmi.dto.ApiResponse;
import com.example.ibmi.dto.EnqueueRequest;
import com.example.ibmi.dto.PortfolioDto;
import com.example.ibmi.dto.TradeOrderDto;
import com.example.ibmi.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(
        name = "IBM i Portfolio Management",
        description = "REST endpoints over IBM i native objects via JT400")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @Operation(summary = "List active portfolios",
               description = "DB2 for i SELECT via ACTIVE_PORTFOLIOS SQL View")
    @GetMapping("/portfolios")
    public ResponseEntity<ApiResponse<List<PortfolioDto>>> getAllPortfolios() { ... }

    @Operation(summary = "Get portfolio by ID",
               description = "Keyed direct read — CHAIN opcode equivalent")
    @GetMapping("/portfolios/{id}")
    public ResponseEntity<ApiResponse<PortfolioDto>> getPortfolioById(
            @Parameter(description = "Portfolio ID (e.g. PF001)") @PathVariable String id) { ... }

    @Operation(summary = "Update portfolio value",
               description = "DB2 for i UPDATE via AS400JDBCDriver using JDBC auto-commit")
    @PutMapping("/portfolios/{id}/value")
    public ResponseEntity<ApiResponse<String>> updatePortfolioValue(
            @Parameter(description = "Portfolio ID") @PathVariable String id,
            @Parameter(description = "New total value") @RequestParam BigDecimal newValue) { ... }

    @Operation(summary = "List pending trade orders")
    @GetMapping("/orders/pending")
    public ResponseEntity<ApiResponse<List<TradeOrderDto>>> getPendingOrders() { ... }

    @Operation(summary = "Enqueue trade order to IBM i *DTAQ",
               description = "SNDDTAQ equivalent")
    @PostMapping("/orders/enqueue")
    public ResponseEntity<ApiResponse<String>> enqueueOrder(
            @RequestBody EnqueueRequest request) { ... }

    @Operation(summary = "Dequeue next order from IBM i *DTAQ",
               description = "RCVDTAQ equivalent")
    @GetMapping("/orders/dequeue")
    public ResponseEntity<ApiResponse<TradeOrderDto>> dequeueOrder(
            @Parameter(description = "Seconds to wait (0 = no wait)")
            @RequestParam(defaultValue = "5") int waitSeconds) { ... }

    @Operation(summary = "Check portfolio eligibility",
               description = "JT400 ProgramCall with EBCDIC params")
    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkEligibility(
            @Parameter(description = "Portfolio ID") @RequestParam String portfolioId,
            @Parameter(description = "ISIN (12 chars)") @RequestParam String isin) { ... }

    @Operation(summary = "Get IBM i job info",
               description = "QUSRJOBI system API — job name/user/number triple")
    @GetMapping("/job-info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getJobInfo() { ... }

    @Operation(summary = "Ping IBM i system",
               description = "DSPLIBL via CommandCall (QCMDEXC)")
    @GetMapping("/system/ping")
    public ResponseEntity<ApiResponse<String>> ping() { ... }
}
```

See the full source file for complete method bodies. Every response wraps data in `ApiResponse<T>` with an `ibmiConcept` field explaining the IBM i operation being performed.

---

## 14. Unit, Integration, and System Tests (AAA Format)

All tests use the **Arrange → Act → Assert (AAA)** pattern.

### Test summary — 28 tests total

| Category | Class | Tests | Requires IBM i? |
|----------|-------|-------|----------------|
| Unit | `PortfolioRepositoryUnitTest` | 7 | No (H2) |
| Unit | `DataQueueServiceUnitTest` | 2 | No (Mockito) |
| Unit | `PortfolioServiceUnitTest` | 3 | No (Mockito) |
| Unit | `PortfolioControllerUnitTest` | 4 | No (MockMvc) |
| Integration | `PortfolioRepositoryIntegrationTest` | 4 | Yes (live PUB400) |
| Integration | `ProgramCallIntegrationTest` | 1 | Yes (live PUB400) |
| Integration | `JournalingSetupTest` | 2 | Yes (creates ORDERQ, starts journaling) |
| System | `BatchSettlementSystemTest` | 5 | Yes (full HTTP round-trip) |

### Run commands

```bash
# Unit tests only (no IBM i required) — 16 tests
mvn test

# All 28 tests including integration + system (requires PUB400)
mvn test -Pintegration
```

### 14.1 Unit Tests — PortfolioRepositoryUnitTest (H2)

`src/test/java/com/example/ibmi/unit/PortfolioRepositoryUnitTest.java`

Tests the repository against H2 in-memory database. No IBM i connection needed. Schema and seed data loaded via `@Sql`.

Key tests:
- `findById_knownId_returnsPortfolio` — verifies keyed read
- `findById_unknownId_returnsEmpty` — verifies not-found handling
- `findAllActive_returnsOnlyActivePortfolios` — verifies view filtering
- `updateValue_validPortfolio_updatesValue` — verifies UPDATE + re-read
- `updateValue_unknownPortfolio_returnsFalse` — verifies not-found
- `findPendingOrders_returnsOnlyPendingOrders` — verifies PEND filter
- `processOrder_pendingOrder_updatesStatus` — verifies PEND→PROC transition

### 14.2 Unit Tests — DataQueueServiceUnitTest (Mockito)

`src/test/java/com/example/ibmi/unit/DataQueueServiceUnitTest.java`

Mocks the `AS400` object so no IBM i connection is needed.

- `enqueueOrder_connectionFails_returnsFalse` — verifies graceful failure
- `dequeueOrder_connectionFails_returnsEmpty` — verifies empty return on failure

### 14.3 Unit Tests — PortfolioServiceUnitTest (Mockito)

`src/test/java/com/example/ibmi/unit/PortfolioServiceUnitTest.java`

Mocks repository and IBM i services to test the service layer in isolation.

- `getAllActivePortfolios_returnsDtoList` — verifies entity-to-DTO mapping
- `getPortfolioById_notFound_returnsEmpty` — verifies empty propagation
- `pingIbmi_delegatesToCommandExecutor` — verifies delegation

### 14.4 Unit Tests — PortfolioControllerUnitTest (MockMvc)

`src/test/java/com/example/ibmi/unit/PortfolioControllerUnitTest.java`

Uses `@WebMvcTest` to test the controller slice with a mocked `PortfolioService`.

- `getAllPortfolios_returns200` — verifies JSON structure
- `getPortfolioById_notFound_returns404` — verifies 404 handling
- `getPortfolioById_found_returns200` — verifies response body
- `ping_returns200` — verifies connectivity endpoint

### 14.5 Integration Tests — Live PUB400

`src/test/java/com/example/ibmi/integration/PortfolioRepositoryIntegrationTest.java`

Requires `IBMI_*` env vars and live PUB400 connection. Reads and writes against the real DB2 for i tables.

`src/test/java/com/example/ibmi/integration/ProgramCallIntegrationTest.java`

Calls `QUSRJOBI` system API on PUB400 and verifies the job name/user/number triple is returned.

`src/test/java/com/example/ibmi/integration/JournalingSetupTest.java`

Creates the ORDERQ `*DTAQ` on IBM i if absent, and starts journaling on PORTFOLIO and TRADE_ORDERS tables (required for UPDATE operations on tables with FK constraints).

### 14.6 System Tests — Full HTTP Round-Trip

`src/test/java/com/example/ibmi/system/BatchSettlementSystemTest.java`

Full HTTP → Spring Boot → JT400 → IBM i round-trips via MockMvc:

- `/system/ping` — confirms connectivity
- `/portfolios` — reads from ACTIVE_PORTFOLIOS view
- `/portfolios/PF001` — keyed read by ID
- `/orders/enqueue` + `/orders/dequeue` — *DTAQ round-trip
- `/job-info` — QUSRJOBI system API

---

## 15. Running the Application Locally

### 15.1 Build

```bash
cd ~/projects/ibmi-batch-simulator
mvn clean install -DskipTests
# Expected: BUILD SUCCESS
```

### 15.2 Run Unit Tests Only (no IBM i required)

```bash
mvn test
# Runs: 16 unit tests
# Expected: Tests run: 16, Failures: 0, Errors: 0
```

### 15.3 Run All Tests (requires live PUB400)

```bash
# Ensure env vars are set:
echo $IBMI_HOST      # pub400.com
echo $IBMI_USER      # CODELIVER
echo $IBMI_PASSWORD  # (your password)

mvn test -Pintegration
# Runs ALL 28 tests including integration and system tests
```

### 15.4 Start the Application

```bash
mvn spring-boot:run
```

Expected startup output:

```
Started IbmiApplication in 4.3 seconds
Tomcat started on port(s): 8080
```

### 15.5 Test from Terminal (curl)

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

## 16. OpenAPI / Swagger UI

With the application running:

- **Swagger UI:** http://localhost:8080/swagger-ui.html — interactive API docs, try endpoints in the browser
- **OpenAPI JSON spec:** http://localhost:8080/v3/api-docs — importable into Postman, API gateways, etc.

The OpenAPI spec is auto-generated from the `@Operation`, `@Parameter`, and `@Tag` annotations on the controller. No manual YAML maintenance needed.

---

## 17. Code Formatting — Spotless

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format (AOSP style) for consistent code formatting. This is the Java equivalent of KTLint for Kotlin.

```bash
# Check formatting (fails if code is not formatted)
mvn spotless:check

# Auto-fix formatting
mvn spotless:apply
```

The Spotless configuration is in `pom.xml`:

```xml
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>2.43.0</version>
  <configuration>
    <java>
      <googleJavaFormat>
        <version>1.22.0</version>
        <style>AOSP</style>
      </googleJavaFormat>
      <removeUnusedImports/>
      <trimTrailingWhitespace/>
      <endWithNewline/>
    </java>
  </configuration>
</plugin>
```

---

## 18. Manual API Tests with REST Client (.http file)

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
  "price":    182.50
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
