package com.challenge40x.contactflow.contact;

import com.challenge40x.contactflow.consultation.Consultation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "contact_task")
public class ContactTask {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "consultation_id", nullable = false)
  private Consultation consultation;

  @Enumerated(EnumType.STRING)
  @Column(name = "task_type", nullable = false)
  private TaskType taskType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "assignee")
  private String assignee;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "customer_preferred_at")
  private Instant customerPreferredAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "evidence", length = 1000)
  private String evidence;

  protected ContactTask() {
  }

  // 신규 상담에 연결할 첫 연락 작업 생성
  public static ContactTask firstContact(Consultation consultation) {
    var task = new ContactTask();
    task.consultation = consultation;
    task.taskType = TaskType.FIRST_CONTACT;
    task.status = Status.PENDING;
    return task;
  }

  // 연락 작업의 조회 응답에 사용할 정보 반환
  public long getId() {
    return id;
  }

  public String getTaskType() {
    return taskType.name();
  }

  public String getStatus() {
    return status.name();
  }

  // 담당자 지정 및 기본 기한과 고객 희망 시각 별도 저장
  void assign(String assignee, Instant dueAt, Instant customerPreferredAt) {
    this.assignee = assignee;
    this.dueAt = dueAt;
    this.customerPreferredAt = customerPreferredAt;
  }

  // 배정된 담당자와 기본 연락 기한 조회
  String getAssignee() {
    return assignee;
  }

  Instant getDueAt() {
    return dueAt;
  }

  // 고객 희망 연락 시각 조회
  Instant getCustomerPreferredAt() {
    return customerPreferredAt;
  }

  // 실제 연락 완료 시각과 근거 기록
  void complete(Instant completedAt, String evidence) {
    this.completedAt = completedAt;
    this.evidence = evidence;
    this.status = Status.COMPLETED;
  }

  // 실제 연락 완료 시각 조회
  Instant getCompletedAt() {
    return completedAt;
  }

  // 실제 연락 완료 근거 조회
  String getEvidence() {
    return evidence;
  }

  private enum Status {
    PENDING,
    COMPLETED
  }

  private enum TaskType {
    FIRST_CONTACT
  }
}