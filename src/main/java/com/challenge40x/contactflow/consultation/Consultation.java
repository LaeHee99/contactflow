package com.challenge40x.contactflow.consultation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
@Table(name = "consultation")
public class Consultation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ga_assignee")
  private String gaAssignee;

  @Column(name = "follow_up_owner")
  private String followUpOwner;

  @Column(name = "next_check_at")
  private Instant nextCheckAt;

  @Column(name = "last_checked_at")
  private Instant lastCheckedAt;

  @Column(name = "last_follow_up_reason", length = 32)
  private String lastFollowUpReason;

  @Column(name = "last_follow_up_note", length = 1000)
  private String lastFollowUpNote;

  @Column(name = "ga_accepted_at")
  private Instant gaAcceptedAt;

  @Column(name = "ga_contacted_at")
  private Instant gaContactedAt;

  @Column(name = "ga_contact_evidence", length = 1000)
  private String gaContactEvidence;

  public Consultation() {
  }

  // 저장된 상담 식별자 반환
  public Long getId() {
    return id;
  }

  // GA 인계 담당자와 인톡 확인 책임 기록
  void recordHandoff(String gaAssignee, String followUpOwner, Instant nextCheckAt) {
    this.gaAssignee = gaAssignee;
    this.followUpOwner = followUpOwner;
    this.nextCheckAt = nextCheckAt;
  }

  // 상담의 GA 인계 등록 여부 확인
  boolean hasHandoff() {
    return gaAssignee != null;
  }

  String getGaAssignee() {
    return gaAssignee;
  }

  String getFollowUpOwner() {
    return followUpOwner;
  }

  Instant getNextCheckAt() {
    return nextCheckAt;
  }

  // 확인 기한이 지난 미확인 인계의 결과와 다음 확인 기한 기록
  boolean recordFollowUp(Instant checkedAt, Instant nextCheckAt, String note) {
    if (checkedAt.equals(lastCheckedAt)) {
      return nextCheckAt.equals(this.nextCheckAt) && note.equals(lastFollowUpNote);
    }

    if (gaContactedAt != null
        || checkedAt.isBefore(this.nextCheckAt)
        || !nextCheckAt.isAfter(checkedAt)) {
      return false;
    }

    this.lastCheckedAt = checkedAt;
    this.lastFollowUpReason = gaAcceptedAt == null || gaAcceptedAt.isAfter(checkedAt)
        ? "GA_NOT_ACCEPTED"
        : "GA_CONTACT_UNCONFIRMED";
    this.lastFollowUpNote = note;
    this.nextCheckAt = nextCheckAt;
    return true;
  }

  // 최근 인톡 확인 시각과 결과 조회
  Instant getLastCheckedAt() {
    return lastCheckedAt;
  }

  String getLastFollowUpReason() {
    return lastFollowUpReason;
  }

  String getLastFollowUpNote() {
    return lastFollowUpNote;
  }

  // 최초 GA 인수 시각 기록 및 동일 요청 재처리
  boolean recordAcceptance(Instant acceptedAt) {
    if (gaAcceptedAt != null) {
      return gaAcceptedAt.equals(acceptedAt);
    }

    gaAcceptedAt = acceptedAt;
    return true;
  }

  // GA 인수 이후 실제 연락 시각과 근거 기록
  boolean recordGaContact(Instant contactedAt, String evidence) {
    if (gaAcceptedAt == null || contactedAt.isBefore(gaAcceptedAt)) {
      return false;
    }

    if (gaContactedAt != null) {
      return gaContactedAt.equals(contactedAt) && gaContactEvidence.equals(evidence);
    }

    gaContactedAt = contactedAt;
    gaContactEvidence = evidence;
    return true;
  }

  // GA 인수 및 실제 연락 기록 조회
  Instant getGaAcceptedAt() {
    return gaAcceptedAt;
  }

  Instant getGaContactedAt() {
    return gaContactedAt;
  }

  String getGaContactEvidence() {
    return gaContactEvidence;
  }
}