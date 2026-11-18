package com.challenge40x.contactflow.contact;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BusinessHoursTest {

  // 금요일 마감 전 접수의 남은 처리 시간을 월요일로 이월
  @Test
  void carriesRemainingTimeAcrossWeekend() {
    Instant receivedAt = Instant.parse("2026-11-20T08:00:00Z");

    Instant dueAt = BusinessHours.firstContactDueAt(receivedAt, Set.of());

    assertThat(dueAt).isEqualTo(Instant.parse("2026-11-23T01:00:00Z"));
  }

  // 주말과 지정 휴일을 건너뛰어 첫 안내 기한 계산
  @Test
  void skipsWeekendAndConfiguredHoliday() {
    Instant receivedAt = Instant.parse("2026-11-20T08:00:00Z");
    Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 11, 23));

    Instant dueAt = BusinessHours.firstContactDueAt(receivedAt, holidays);

    assertThat(dueAt).isEqualTo(Instant.parse("2026-11-24T01:00:00Z"));
  }
}