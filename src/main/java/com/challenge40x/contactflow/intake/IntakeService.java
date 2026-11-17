package com.challenge40x.contactflow.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeService {

  private final IntakeRepository intakeRepository;

  public IntakeService(IntakeRepository intakeRepository) {
    this.intakeRepository = intakeRepository;
  }

  // 신규 접수 저장 및 중복 접수의 기존 ID 반환
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