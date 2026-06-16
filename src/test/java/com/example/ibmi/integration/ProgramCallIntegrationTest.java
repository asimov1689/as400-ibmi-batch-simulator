package com.example.ibmi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ibmi.service.ibmi.ProgramCallService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Requires live PUB400 connection. Run with: mvn test -Pintegration */
@SpringBootTest
class ProgramCallIntegrationTest {

    @Autowired private ProgramCallService programCallService;

    @Test
    @DisplayName("TC-I-10: getJobInfo returns real IBM i job details from PUB400")
    void getJobInfo_liveIBMi_returnsJobDetails() {
        // Arrange — requires live PUB400 connection

        // Act
        Map<String, String> result = programCallService.getJobInfo();

        // Assert
        assertThat(result).containsKey("jobName");
        assertThat(result).containsKey("jobUser");
        assertThat(result).containsKey("jobNumber");
        assertThat(result.get("jobName")).isNotBlank();
    }
}
