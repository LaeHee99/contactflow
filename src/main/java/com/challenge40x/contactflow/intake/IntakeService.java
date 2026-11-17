package com.challenge40x.contactflow.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeService {

  private final IntakeRepository intakeRepository;

  public IntakeService(IntakeRepository intakeRepository) {
    this.intakeRepository = intakeRepository;
  }

  // 신규 접수 저장 및 동일 접수 키의 내용 충돌 확인
  @Transactional
  public long receive(ReceiveIntakeCommand command) {
    intakeRepository.insertIfAbsent(
        command.source().name(),
        command.externalId(),
        command.customerKey());

    Intake storedIntake = intakeRepository
        .findBySourceAndExternalId(command.source(), command.externalId())
        .orElseThrow(() -> new IllegalStateException("Stored intake not found"));

    if (!storedIntake.hasCustomerKey(command.customerKey())) {
      throw new IntakeConflictException();
    }

    return storedIntake.getId();
  }
}