package com.challenge40x.contactflow.intake;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IntakeServiceTest {

  @Autowired
  private IntakeService intakeService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("동일 접수를 재전송하면 기존 ID를 반환하고 한 건만 저장한다")
  void returnsExistingIdOnDuplicateIntake() {
    var command = new ReceiveIntakeCommand(
        IntakeSource.WEBSITE, "WEB-001", "CUSTOMER-001");

    long firstId = intakeService.receive(command);
    long secondId = intakeService.receive(command);

    assertThat(secondId).isEqualTo(firstId);
    assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM intake
                WHERE source = ? AND external_id = ?
                """, Long.class, "WEBSITE", "WEB-001")).isEqualTo(1L);
  }
}