package com.challenge40x.contactflow.contact;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

final class BusinessHours {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalTime OPENING_TIME = LocalTime.of(9, 0);
  private static final LocalTime CLOSING_TIME = LocalTime.of(18, 0);
  private static final Duration FIRST_CONTACT_WINDOW = Duration.ofHours(2);

  private BusinessHours() {
  }

  // 주말과 지정 휴일을 제외한 첫 안내 기한 계산
  static Instant firstContactDueAt(Instant receivedAt, Set<LocalDate> holidays) {
    ZonedDateTime cursor = receivedAt.atZone(BUSINESS_ZONE);
    Duration remaining = FIRST_CONTACT_WINDOW;

    while (true) {
      LocalDate date = cursor.toLocalDate();
      ZonedDateTime opening = date.atTime(OPENING_TIME).atZone(BUSINESS_ZONE);
      ZonedDateTime closing = date.atTime(CLOSING_TIME).atZone(BUSINESS_ZONE);

      if (isBusinessDay(date, holidays) && cursor.isBefore(closing)) {
        ZonedDateTime start = cursor.isBefore(opening) ? opening : cursor;
        Duration available = Duration.between(start, closing);

        if (remaining.compareTo(available) <= 0) {
          return start.plus(remaining).toInstant();
        }

        remaining = remaining.minus(available);
      }

      cursor = date.plusDays(1).atTime(OPENING_TIME).atZone(BUSINESS_ZONE);
    }
  }

  // 평일 여부 및 지정 휴일 확인
  private static boolean isBusinessDay(LocalDate date, Set<LocalDate> holidays) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY
        && day != DayOfWeek.SUNDAY
        && !holidays.contains(date);
  }
}