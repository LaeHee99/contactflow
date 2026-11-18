package com.challenge40x.contactflow.consultation;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultationHandoffService {

  private final ConsultationRepository consultationRepository;

  public ConsultationHandoffService(ConsultationRepository consultationRepository) {
    this.consultationRepository = consultationRepository;
  }

  // 상담 잠금 후 인계 정보 저장 및 최초 등록 여부 반환
  @Transactional
  public Optional<Boolean> record(
      long consultationId, String gaAssignee, String followUpOwner, Instant nextCheckAt) {
    return consultationRepository.findByIdForUpdate(consultationId)
        .map(consultation -> {
          boolean created = !consultation.hasHandoff();
          consultation.recordHandoff(gaAssignee, followUpOwner, nextCheckAt);
          return created;
        });
  }

  // GA 인계 정보와 인수·연락 초기 상태 조회
  @Transactional(readOnly = true)
  public Optional<HandoffView> find(long consultationId) {
    return consultationRepository.findById(consultationId)
        .filter(Consultation::hasHandoff)
        .map(consultation -> new HandoffView(
            consultation.getGaAssignee(),
            "PENDING",
            "UNCONFIRMED",
            consultation.getFollowUpOwner(),
            consultation.getNextCheckAt().toString()));
  }

  public record HandoffView(
      String gaAssignee,
      String acceptanceStatus,
      String contactStatus,
      String followUpOwner,
      String nextCheckAt
  ) {
  }
}