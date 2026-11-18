package com.challenge40x.contactflow.contact;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactTaskAssignmentService {

  private final ContactTaskRepository contactTaskRepository;

  public ContactTaskAssignmentService(ContactTaskRepository contactTaskRepository) {
    this.contactTaskRepository = contactTaskRepository;
  }

  // 상담에 속한 연락 작업에 담당자와 기한 정보 저장
  @Transactional
  public boolean assign(
      long consultationId, long taskId, String assignee, Instant dueAt,
      Instant customerPreferredAt, Instant agreedContactAt) {
    var task = contactTaskRepository.findByIdAndConsultation_Id(taskId, consultationId);
    if (task.isEmpty()) {
      return false;
    }

    task.get().assign(assignee, dueAt, customerPreferredAt, agreedContactAt);
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