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

  // 상담 잠금 후 GA 인수 기록 저장
  @Transactional
  public UpdateResult accept(long consultationId, Instant acceptedAt) {
    return consultationRepository.findByIdForUpdate(consultationId)
        .filter(Consultation::hasHandoff)
        .map(consultation -> consultation.recordAcceptance(acceptedAt)
            ? UpdateResult.SAVED : UpdateResult.CONFLICT)
        .orElse(UpdateResult.NOT_FOUND);
  }

  // 상담 잠금 후 실제 연락 시각과 근거 저장
  @Transactional
  public UpdateResult recordContact(long consultationId, Instant contactedAt, String evidence) {
    return consultationRepository.findByIdForUpdate(consultationId)
        .filter(Consultation::hasHandoff)
        .map(consultation -> consultation.recordGaContact(contactedAt, evidence)
            ? UpdateResult.SAVED : UpdateResult.CONFLICT)
        .orElse(UpdateResult.NOT_FOUND);
  }

  // 저장된 인수·연락 기록을 기준으로 인계 상태와 확인 책임 조회
  @Transactional(readOnly = true)
  public Optional<HandoffView> find(long consultationId) {
    return consultationRepository.findById(consultationId)
        .filter(Consultation::hasHandoff)
        .map(consultation -> {
          Instant acceptedAt = consultation.getGaAcceptedAt();
          Instant contactedAt = consultation.getGaContactedAt();

          return new HandoffView(
              consultation.getGaAssignee(),
              acceptedAt == null ? "PENDING" : "ACCEPTED",
              contactedAt == null ? "UNCONFIRMED" : "CONFIRMED",
              consultation.getFollowUpOwner(),
              consultation.getNextCheckAt().toString(),
              acceptedAt == null ? null : acceptedAt.toString(),
              contactedAt == null ? null : contactedAt.toString(),
              consultation.getGaContactEvidence());
        });
  }

  public record HandoffView(
      String gaAssignee,
      String acceptanceStatus,
      String contactStatus,
      String followUpOwner,
      String nextCheckAt,
      String acceptedAt,
      String contactedAt,
      String contactEvidence
  ) {
  }

  public enum UpdateResult {
    SAVED,
    NOT_FOUND,
    CONFLICT
  }
}