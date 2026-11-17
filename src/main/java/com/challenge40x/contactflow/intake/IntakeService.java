package com.challenge40x.contactflow.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeService {

  private final IntakeRepository intakeRepository;

  public IntakeService(IntakeRepository intakeRepository) {
    this.intakeRepository = intakeRepository;
  }

  // 원본 접수 저장 후 신규 또는 기존 접수 ID 반환
  @Transactional
  public long receive(ReceiveIntakeCommand command) {
    intakeRepository.insertIfAbsent(
        command.source().name(),
        command.externalId(),
        command.customerKey());

    return intakeRepository
        .findBySourceAndExternalId(command.source(), command.externalId())
        .orElseThrow(() -> new IllegalStateException("Stored intake not found"))
        .getId();
  }
}