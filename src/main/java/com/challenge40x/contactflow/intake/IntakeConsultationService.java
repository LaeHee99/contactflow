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

  // 새 상담 생성 또는 지정된 기존 상담 연결 및 재처리 결과 반환
  @Transactional
  public Optional<LinkResult> link(long intakeId, Long targetConsultationId) {
    var intake = intakeRepository.findByIdForUpdate(intakeId);
    if (intake.isEmpty()) {
      return Optional.empty();
    }

    Intake storedIntake = intake.get();
    Consultation existing = storedIntake.getConsultation();
    if (existing != null) {
      if (targetConsultationId != null && !existing.getId().equals(targetConsultationId)) {
        throw new IntakeConflictException();
      }

      return Optional.of(new LinkResult(existing.getId(), false));
    }

    Consultation target;
    if (targetConsultationId == null) {
      target = consultationRepository.save(new Consultation());
    } else {
      var requestedConsultation = consultationRepository.findById(targetConsultationId);
      if (requestedConsultation.isEmpty()) {
        return Optional.empty();
      }

      if (!intakeRepository.existsByConsultation_IdAndCustomerKey(
          targetConsultationId, storedIntake.getCustomerKey())) {
        throw new IntakeConflictException();
      }

      target = requestedConsultation.get();
    }

    storedIntake.linkConsultation(target);
    return Optional.of(new LinkResult(target.getId(), true));
  }

  public record LinkResult(long consultationId, boolean created) {
  }
}