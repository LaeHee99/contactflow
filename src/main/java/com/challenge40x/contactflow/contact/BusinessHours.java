package com.challenge40x.contactflow.contact;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

final class BusinessHours {

  private BusinessHours() {
  }

  // 접수 시각부터 2영업시간 뒤의 첫 안내 기한 계산
  static Instant firstContactDueAt(Instant receivedAt, Set<LocalDate> holidays) {
    throw new UnsupportedOperationException("Business deadline calculation is not implemented");
  }
}