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

  // 상담 내 연락 작업 검색 및 담당자·기한 저장
  @Transactional
  public boolean assign(long consultationId, long taskId, String assignee, Instant dueAt) {
    var task = contactTaskRepository.findByIdAndConsultation_Id(taskId, consultationId);
    if (task.isEmpty()) {
      return false;
    }

    task.get().assign(assignee, dueAt);
    return true;
  }
}