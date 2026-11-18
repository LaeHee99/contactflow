package com.challenge40x.contactflow.intake;

import java.time.Instant;

public record ReceiveIntakeCommand(
    IntakeSource source,
    String externalId,
    String customerKey,
    Instant receivedAt
) {
  // 접수 시각을 생략한 요청 생성
  public ReceiveIntakeCommand(IntakeSource source, String externalId, String customerKey) {
    this(source, externalId, customerKey, null);
  }
}