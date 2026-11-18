package com.challenge40x.contactflow.contact;

import com.challenge40x.contactflow.consultation.ConsultationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactTaskQueryService {

  private final ConsultationRepository consultationRepository;
  private final ContactTaskRepository contactTaskRepository;

  public ContactTaskQueryService(
      ConsultationRepository consultationRepository,
      ContactTaskRepository contactTaskRepository) {
    this.consultationRepository = consultationRepository;
    this.contactTaskRepository = contactTaskRepository;
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
            task.getDueAt() == null ? null : task.getDueAt().toString(),
            task.getCustomerPreferredAt() == null ? null : task.getCustomerPreferredAt().toString(),
            task.getCompletedAt() == null ? null : task.getCompletedAt().toString(),
            task.getEvidence()))
        .toList();

    return Optional.of(tasks);
  }

  // 연락 작업의 상태·배정 정보·완료 근거 반환
  public record ContactTaskView(
      long taskId,
      String taskType,
      String status,
      String assignee,
      String dueAt,
      String customerPreferredAt,
      String completedAt,
      String evidence
  ) {
  }
}