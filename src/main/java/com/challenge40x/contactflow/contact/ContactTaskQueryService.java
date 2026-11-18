package com.challenge40x.contactflow.contact;

import com.challenge40x.contactflow.consultation.ConsultationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactTaskQueryService {

  private final ConsultationRepository consultationRepository;
  private final ContactTaskRepository contactTaskRepository;
  private final ContactTaskChangeRepository contactTaskChangeRepository;

  public ContactTaskQueryService(
      ConsultationRepository consultationRepository,
      ContactTaskRepository contactTaskRepository,
      ContactTaskChangeRepository contactTaskChangeRepository) {
    this.consultationRepository = consultationRepository;
    this.contactTaskRepository = contactTaskRepository;
    this.contactTaskChangeRepository = contactTaskChangeRepository;
  }

  // 상담 존재 여부 확인 및 연락 작업 목록 조회
  @Transactional(readOnly = true)
  public Optional<List<ContactTaskView>> findByConsultation(long consultationId) {
    if (!consultationRepository.existsById(consultationId)) {
      return Optional.empty();
    }

    var tasks = contactTaskRepository.findAllByConsultation_IdOrderByIdAsc(consultationId)
        .stream()
        .map(task -> new ContactTaskView(
            task.getId(),
            task.getTaskType(),
            task.getStatus(),
            task.getAssignee(),
            task.getAssignmentStatus(),
            task.getAcceptedAt() == null ? null : task.getAcceptedAt().toString(),
            task.getDueAt() == null ? null : task.getDueAt().toString(),
            task.getCustomerPreferredAt() == null ? null : task.getCustomerPreferredAt().toString(),
            task.getAgreedContactAt() == null ? null : task.getAgreedContactAt().toString(),
            task.getEffectiveDueAt() == null ? null : task.getEffectiveDueAt().toString(),
            task.needsTimeConfirmation(),
            task.getCompletedAt() == null ? null : task.getCompletedAt().toString(),
            task.getEvidence(),
            task.getScheduledAt() == null ? null : task.getScheduledAt().toString(),
            task.getChannel(),
            task.getStartedAt() == null ? null : task.getStartedAt().toString()))
        .toList();

    return Optional.of(tasks);
  }

  // 지정된 상담에 속한 작업의 배정 변경 이력 조회
  @Transactional(readOnly = true)
  public Optional<List<ContactTaskHistoryView>> findHistory(long consultationId, long taskId) {
    if (!contactTaskRepository.existsByIdAndConsultation_Id(taskId, consultationId)) {
      return Optional.empty();
    }

    var history = contactTaskChangeRepository.findAllByTask_IdOrderByIdAsc(taskId)
        .stream()
        .map(row -> new ContactTaskHistoryView(
            row.getPreviousAssignee(),
            row.getNewAssignee(),
            formatTime(row.getPreviousEffectiveDueAt()),
            formatTime(row.getNewEffectiveDueAt()),
            row.getChangedBy(),
            row.getChangeReason(),
            formatTime(row.getChangedAt())))
        .toList();

    return Optional.of(history);
  }

  // 기록된 시각을 UTC ISO 문자열로 변환
  private static String formatTime(Instant value) {
    return value == null ? null : value.toString();
  }

  public record ContactTaskHistoryView(
      String previousAssignee,
      String newAssignee,
      String previousEffectiveDueAt,
      String newEffectiveDueAt,
      String changedBy,
      String changeReason,
      String changedAt
  ) {
  }

  // 연락 작업의 상태·배정·완료·재연락 예약 정보 반환
  public record ContactTaskView(
      long taskId,
      String taskType,
      String status,
      String assignee,
      String assignmentStatus,
      String acceptedAt,
      String dueAt,
      String customerPreferredAt,
      String agreedContactAt,
      String effectiveDueAt,
      boolean needsTimeConfirmation,
      String completedAt,
      String evidence,
      String scheduledAt,
      String channel,
      String startedAt
  ) {
  }
}