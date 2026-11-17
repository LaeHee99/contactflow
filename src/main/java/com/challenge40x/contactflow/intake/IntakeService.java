package com.challenge40x.contactflow.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeService {

  private final IntakeRepository intakeRepository;

  public IntakeService(IntakeRepository intakeRepository) {
    this.intakeRepository = intakeRepository;
  }

  // 동일 원본 접수 재전송 시 기존 접수 ID 반환
  @Transactional
  public long receive(ReceiveIntakeCommand command) {
    return intakeRepository
        .findBySourceAndExternalId(command.source(), command.externalId())
        .orElseGet(() -> intakeRepository.save(new Intake(
            command.source(),
            command.externalId(),
            command.customerKey()
        )))
        .getId();
  }
}