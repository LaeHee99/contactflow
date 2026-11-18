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

  @Column(name = "callback_consented")
  private Boolean callbackConsented;

  @Column(name = "callback_reason", length = 1000)
  private String callbackReason;

  @Column(name = "scheduled_at")
  private Instant scheduledAt;

  @Column(name = "channel", length = 32)
  private String channel;

  @Column(name = "started_at")
  private Instant startedAt;

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

  // 동의가 확인된 재연락 대기 작업 생성
  static ContactTask callback(
      Consultation consultation, String reason, Instant scheduledAt,
      String channel, String assignee) {
    var task = new ContactTask();
    task.consultation = consultation;
    task.taskType = TaskType.CALLBACK;
    task.status = Status.PENDING;
    task.scheduleCallback(reason, scheduledAt, channel, assignee);
    return task;
  }

  // 대기 중인 재연락 작업의 동의와 예약 정보 기록
  void scheduleCallback(String reason, Instant scheduledAt, String channel, String assignee) {
    if (status != Status.PENDING) {
      throw new ContactTaskConflictException("Callback task is no longer pending");
    }

    this.callbackConsented = true;
    this.callbackReason = reason;
    this.scheduledAt = scheduledAt;
    this.channel = channel;
    this.assignee = assignee;
  }

  // 재연락 작업 여부 확인
  boolean isCallback() {
    return taskType == TaskType.CALLBACK;
  }

  // 재연락 예약 시각과 채널 조회
  Instant getScheduledAt() {
    return scheduledAt;
  }

  String getChannel() {
    return channel;
  }

  // 동의 철회 기록 및 아직 시작하지 않은 재연락 작업 취소
  void withdrawCallbackConsent() {
    callbackConsented = false;
    if (status == Status.PENDING) {
      status = Status.CANCELLED;
    }
  }

  // 최신 동의와 작업 상태 확인 후 모의 재연락 시작 기록
  void startCallback(Instant now) {
    if (!Boolean.TRUE.equals(callbackConsented)) {
      throw new ContactTaskConflictException("Callback consent is required");
    }

    if (status == Status.IN_PROGRESS) {
      return;
    }

    if (status != Status.PENDING) {
      throw new ContactTaskConflictException("Callback task cannot be started");
    }

    startedAt = now;
    status = Status.IN_PROGRESS;
  }

  // 모의 재연락 시작 시각 조회
  Instant getStartedAt() {
    return startedAt;
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

  // 연락 완료 기록 및 동일 요청 재전송 시 최초 기록 보존
  void complete(Instant completedAt, String evidence) {
    if (status == Status.COMPLETED) {
      if (completedAt.equals(this.completedAt) && evidence.equals(this.evidence)) {
        return;
      }

      throw new ContactTaskConflictException("Contact task already has a different completion");
    }

    if (isCallback() && status != Status.IN_PROGRESS) {
      throw new ContactTaskConflictException("Callback must be started before completion");
    }

    if (assignee == null || assignee.isBlank()) {
      throw new ContactTaskConflictException("Contact task has no assignee");
    }

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
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
  }

  enum TaskType {
    FIRST_CONTACT,
    CALLBACK
  }
}