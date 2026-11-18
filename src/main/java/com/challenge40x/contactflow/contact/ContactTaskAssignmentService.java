package com.challenge40x.contactflow.contact;

import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactTaskAssignmentService {

  private final ContactTaskRepository contactTaskRepository;
  private final ContactTaskChangeRepository contactTaskChangeRepository;

  public ContactTaskAssignmentService(
      ContactTaskRepository contactTaskRepository,
      ContactTaskChangeRepository contactTaskChangeRepository) {
    this.contactTaskRepository = contactTaskRepository;
    this.contactTaskChangeRepository = contactTaskChangeRepository;
  }

  // 연락 작업을 잠근 뒤 배정 정보와 변경 이력 저장
  @Transactional
  public boolean assign(
      long consultationId, long taskId, String assignee, Instant dueAt,
      Instant customerPreferredAt, Instant agreedContactAt,
      String changedBy, String changeReason) {
    var existing = contactTaskRepository.findByIdAndConsultation_Id(taskId, consultationId);
    if (existing.isEmpty()) {
      return false;
    }

    ContactTask task = existing.get();
    String previousAssignee = task.getAssignee();
    Instant previousEffectiveDueAt = task.getEffectiveDueAt();

    task.assign(assignee, dueAt, customerPreferredAt, agreedContactAt);

    if (!Objects.equals(previousAssignee, task.getAssignee())
        || !Objects.equals(previousEffectiveDueAt, task.getEffectiveDueAt())) {
      contactTaskChangeRepository.save(new ContactTaskChange(
          task, previousAssignee, previousEffectiveDueAt,
          changedBy, changeReason, Instant.now()));
    }

    return true;
  }

  // 상담 내 연락 작업을 잠근 뒤 담당자 수락 기록
  @Transactional
  public boolean accept(long consultationId, long taskId, String assignee, Instant acceptedAt) {
    var task = contactTaskRepository.findByIdAndConsultation_Id(taskId, consultationId);
    if (task.isEmpty()) {
      return false;
    }

    task.get().acceptAssignment(assignee, acceptedAt);
    return true;
  }
}