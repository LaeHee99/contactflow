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
}