package com.challenge40x.contactflow.contact;

import com.challenge40x.contactflow.consultation.ConsultationRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallbackService {

  private final ConsultationRepository consultationRepository;
  private final ContactTaskRepository contactTaskRepository;

  public CallbackService(
      ConsultationRepository consultationRepository,
      ContactTaskRepository contactTaskRepository) {
    this.consultationRepository = consultationRepository;
    this.contactTaskRepository = contactTaskRepository;
  }

  // 동의 확인 후 재연락 작업 생성 또는 기존 예약 갱신
  @Transactional
  public Optional<Boolean> schedule(
      long consultationId, boolean consented, String reason, Instant scheduledAt,
      String channel, String assignee) {
    var consultation = consultationRepository.findByIdForUpdate(consultationId);
    if (consultation.isEmpty()) {
      return Optional.empty();
    }

    if (!consented) {
      throw new ContactTaskConflictException("Callback consent is required");
    }

    var existing = findCallbackForUpdate(consultationId);

    if (existing.isPresent()) {
      existing.get().scheduleCallback(reason, scheduledAt, channel, assignee);
      return Optional.of(false);
    }

    contactTaskRepository.save(
        ContactTask.callback(consultation.get(), reason, scheduledAt, channel, assignee));
    return Optional.of(true);
  }

  // 재연락 동의 철회 및 대기 작업 취소
  @Transactional
  public boolean withdrawConsent(long consultationId) {
    if (consultationRepository.findByIdForUpdate(consultationId).isEmpty()) {
      return false;
    }

    var callback = findCallbackForUpdate(consultationId);
    if (callback.isEmpty()) {
      return false;
    }

    callback.get().withdrawCallbackConsent();
    return true;
  }

  // 재연락 작업과 최신 동의 확인 후 모의 실행 시작
  @Transactional
  public boolean start(long consultationId) {
    if (consultationRepository.findByIdForUpdate(consultationId).isEmpty()) {
      return false;
    }

    var callback = findCallbackForUpdate(consultationId);
    if (callback.isEmpty()) {
      return false;
    }

    callback.get().startCallback(Instant.now());
    return true;
  }

  // 상담과 재연락 작업을 잠근 뒤 미응답 결과 저장
  @Transactional
  public boolean recordNoAnswer(long consultationId) {
    if (consultationRepository.findByIdForUpdate(consultationId).isEmpty()) {
      return false;
    }

    var callback = findCallbackForUpdate(consultationId);
    if (callback.isEmpty()) {
      return false;
    }

    callback.get().recordNoAnswer();
    return true;
  }

  // 상담에 연결된 재연락 작업 잠금 조회
  private Optional<ContactTask> findCallbackForUpdate(long consultationId) {
    return contactTaskRepository.findByConsultation_IdAndTaskType(
        consultationId, ContactTask.TaskType.CALLBACK);
  }
}