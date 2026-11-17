package com.challenge40x.contactflow.intake;

import com.challenge40x.contactflow.consultation.Consultation;
import com.challenge40x.contactflow.consultation.ConsultationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeConsultationService {

  private final IntakeRepository intakeRepository;
  private final ConsultationRepository consultationRepository;

  public IntakeConsultationService(
      IntakeRepository intakeRepository,
      ConsultationRepository consultationRepository) {
    this.intakeRepository = intakeRepository;
    this.consultationRepository = consultationRepository;
  }

  // 미연결 원본에 상담 생성 및 재처리 시 기존 상담 반환
  @Transactional
  public Optional<LinkResult> link(long intakeId) {
    var intake = intakeRepository.findByIdForUpdate(intakeId);
    if (intake.isEmpty()) {
      return Optional.empty();
    }

    Intake storedIntake = intake.get();
    Consultation existing = storedIntake.getConsultation();
    if (existing != null) {
      return Optional.of(new LinkResult(existing.getId(), false));
    }

    Consultation created = consultationRepository.save(new Consultation());
    storedIntake.linkConsultation(created);
    return Optional.of(new LinkResult(created.getId(), true));
  }

  public record LinkResult(long consultationId, boolean created) {
  }
}