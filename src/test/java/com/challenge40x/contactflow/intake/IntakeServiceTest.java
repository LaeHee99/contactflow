package com.challenge40x.contactflow.intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

  // 동일 원본 접수의 동시 재전송 시 기존 ID 반환 및 단일 저장 검증
  @Test
  void storesConcurrentDuplicateIntakeOnlyOnce() throws Exception {
    var command = new ReceiveIntakeCommand(
        IntakeSource.WEBSITE, "WEB-CONCURRENT-001", "CUSTOMER-002");
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    Callable<Long> receiveIntake = () -> {
      ready.countDown();
      if (!start.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Concurrent requests did not start");
      }
      return intakeService.receive(command);
    };

    try {
      var first = executor.submit(receiveIntake);
      var second = executor.submit(receiveIntake);

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      long firstId = first.get(10, TimeUnit.SECONDS);
      long secondId = second.get(10, TimeUnit.SECONDS);

      assertThat(secondId).isEqualTo(firstId);
      assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM intake
                WHERE source = ? AND external_id = ?
                """, Long.class, "WEBSITE", "WEB-CONCURRENT-001"))
          .isEqualTo(1L);
    } finally {
      executor.shutdownNow();
    }
  }

  // 동일 접수 키에 다른 고객 정보가 들어오면 충돌 처리 및 원본 보존 검증
  @Test
  void rejectsConflictingIntakeAndPreservesOriginal() {
    var original = new ReceiveIntakeCommand(
        IntakeSource.WEBSITE, "WEB-CONFLICT-001", "CUSTOMER-003");
    var conflicting = new ReceiveIntakeCommand(
        IntakeSource.WEBSITE, "WEB-CONFLICT-001", "CUSTOMER-004");

    long originalId = intakeService.receive(original);

    assertThatThrownBy(() -> intakeService.receive(conflicting))
        .isInstanceOf(IntakeConflictException.class);

    assertThat(jdbcTemplate.queryForObject("""
        SELECT customer_key
        FROM intake
        WHERE id = ?
        """, String.class, originalId)).isEqualTo("CUSTOMER-003");
  }
}