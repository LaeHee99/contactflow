package com.challenge40x.contactflow.consultation;

import com.challenge40x.contactflow.contact.ContactTaskRepository;
import com.challenge40x.contactflow.intake.IntakeRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultationMetricsService {

  private final ConsultationRepository consultationRepository;
  private final IntakeRepository intakeRepository;
  private final ContactTaskRepository contactTaskRepository;

  public ConsultationMetricsService(
      ConsultationRepository consultationRepository,
      IntakeRepository intakeRepository,
      ContactTaskRepository contactTaskRepository) {
    this.consultationRepository = consultationRepository;
    this.intakeRepository = intakeRepository;
    this.contactTaskRepository = contactTaskRepository;
  }

  // 상담별 접수 복구 건수와 기한 내 첫 연락 건수 조회
  @Transactional(readOnly = true)
  public Optional<MetricsView> find(long consultationId) {
    if (!consultationRepository.existsById(consultationId)) {
      return Optional.empty();
    }

    return Optional.of(new MetricsView(
        intakeRepository.countByConsultation_IdAndRecoveredTrue(consultationId),
        contactTaskRepository.countOnTimeFirstContacts(consultationId)));
  }

  public record MetricsView(long recoveredIntakeCount, long onTimeFirstContactCount) {
  }
}