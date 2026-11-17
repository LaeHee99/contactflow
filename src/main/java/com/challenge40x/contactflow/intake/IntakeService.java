package com.challenge40x.contactflow.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeService {

  private final IntakeRepository intakeRepository;

  public IntakeService(IntakeRepository intakeRepository) {
    this.intakeRepository = intakeRepository;
  }

  // 접수 저장, 내용 충돌 확인 및 신규 생성 여부 반환
  @Transactional
  public ReceiveResult receive(ReceiveIntakeCommand command) {
    boolean created = intakeRepository.insertIfAbsent(
        command.source().name(),
        command.externalId(),
        command.customerKey()) == 1;

    Intake storedIntake = intakeRepository
        .findBySourceAndExternalId(command.source(), command.externalId())
        .orElseThrow(() -> new IllegalStateException("Stored intake not found"));

    if (!storedIntake.hasCustomerKey(command.customerKey())) {
      throw new IntakeConflictException();
    }

    return new ReceiveResult(storedIntake.getId(), created);
  }

  public record ReceiveResult(long intakeId, boolean created) {
  }
}