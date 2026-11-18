package com.challenge40x.contactflow.consultation;

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
@Table(name = "handoff_follow_up_history")
public class HandoffFollowUpHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "consultation_id", nullable = false)
  private Consultation consultation;

  private String followUpOwner;
  private Instant checkedAt;
  private Instant previousNextCheckAt;
  private Instant nextCheckAt;

  @Column(length = 32)
  private String reason;

  @Column(length = 1000)
  private String note;

  protected HandoffFollowUpHistory() {
  }

  // 확인 당시 담당자와 후속 조치의 기한 변경 전후 기록
  HandoffFollowUpHistory(Consultation consultation, Instant previousNextCheckAt) {
    this.consultation = consultation;
    this.followUpOwner = consultation.getFollowUpOwner();
    this.checkedAt = consultation.getLastCheckedAt();
    this.previousNextCheckAt = previousNextCheckAt;
    this.nextCheckAt = consultation.getNextCheckAt();
    this.reason = consultation.getLastFollowUpReason();
    this.note = consultation.getLastFollowUpNote();
  }
}