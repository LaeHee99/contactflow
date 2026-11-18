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

    var existing = contactTaskRepository.findAllByConsultation_IdOrderByIdAsc(consultationId)
        .stream()
        .filter(ContactTask::isCallback)
        .findFirst();

    if (existing.isPresent()) {
      existing.get().scheduleCallback(reason, scheduledAt, channel, assignee);
      return Optional.of(false);
    }

    contactTaskRepository.save(
        ContactTask.callback(consultation.get(), reason, scheduledAt, channel, assignee));
    return Optional.of(true);
  }
}