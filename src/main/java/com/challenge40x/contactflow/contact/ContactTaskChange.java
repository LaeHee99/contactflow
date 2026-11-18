package com.challenge40x.contactflow.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "contact_task_change")
public class ContactTaskChange {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false)
  private ContactTask task;

  private String previousAssignee;
  private String newAssignee;
  private Instant previousEffectiveDueAt;
  private Instant newEffectiveDueAt;

  @Column(length = 32)
  private String previousChannel;

  @Column(length = 32)
  private String newChannel;

  private Instant previousScheduledAt;
  private Instant newScheduledAt;
  private String changedBy;

  @Column(length = 1000)
  private String changeReason;

  @Column(nullable = false)
  private Instant changedAt;

  protected ContactTaskChange() {
  }

  // 배정 변경 전후의 담당자·적용 기한과 변경 근거 기록
  ContactTaskChange(
      ContactTask task, String previousAssignee, Instant previousEffectiveDueAt,
      String changedBy, String changeReason, Instant changedAt) {
    this.task = task;
    this.previousAssignee = previousAssignee;
    this.newAssignee = task.getAssignee();
    this.previousEffectiveDueAt = previousEffectiveDueAt;
    this.newEffectiveDueAt = task.getEffectiveDueAt();
    this.changedBy = changedBy;
    this.changeReason = changeReason;
    this.changedAt = changedAt;
  }

  // 재연락 예약의 채널·시각·담당자 변경 전후 기록 생성
  static ContactTaskChange callbackChanged(
      ContactTask task, CallbackSnapshot before, CallbackSnapshot after,
      String changedBy, String changeReason, Instant changedAt) {
    var change = new ContactTaskChange(
        task, before.assignee(), task.getEffectiveDueAt(),
        changedBy, changeReason, changedAt);
    change.previousChannel = before.channel();
    change.newChannel = after.channel();
    change.previousScheduledAt = before.scheduledAt();
    change.newScheduledAt = after.scheduledAt();
    return change;
  }

  // 현재 재연락 예약의 변경 비교용 값 보관
  record CallbackSnapshot(String assignee, String channel, Instant scheduledAt) {
    static CallbackSnapshot from(ContactTask task) {
      return new CallbackSnapshot(task.getAssignee(), task.getChannel(), task.getScheduledAt());
    }
  }
}