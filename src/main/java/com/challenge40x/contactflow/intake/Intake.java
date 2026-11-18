package com.challenge40x.contactflow.intake;

import com.challenge40x.contactflow.consultation.Consultation;
import java.time.Instant;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "intake",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_intake_source_external_id",
        columnNames = {"source", "external_id"}
    )
)
public class Intake {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false)
  private IntakeSource source;

  @Column(name = "external_id", nullable = false)
  private String externalId;

  @Column(name = "customer_key", nullable = false)
  private String customerKey;

  @Column(name = "received_at")
  private Instant receivedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consultation_id")
  private Consultation consultation;

  @Column(name = "recovered")
  private Boolean recovered;

  @Column(name = "recovery_confirmed_by")
  private String recoveryConfirmedBy;

  @Column(name = "recovery_confirmed_at")
  private Instant recoveryConfirmedAt;

  protected Intake() {
  }

  public Long getId() {
    return id;
  }

  // 원본 접수의 경로와 외부 식별자 조회
  IntakeSource getSource() {
    return source;
  }

  String getExternalId() {
    return externalId;
  }

  // 저장된 고객 식별자와 요청의 고객 식별자 비교
  boolean hasCustomerKey(String customerKey) {
    return this.customerKey.equals(customerKey);
  }

  // 연결 대상 상담의 고객 일치 확인에 사용할 식별자 반환
  String getCustomerKey() {
    return customerKey;
  }

  // 최초 원본 접수 시각 조회
  Instant getReceivedAt() {
    return receivedAt;
  }

  // 원본 접수에 연결된 상담 조회
  Consultation getConsultation() {
    return consultation;
  }

  // 상담 연결과 기존 상담을 통한 복구 여부 기록
  void linkConsultation(Consultation consultation, boolean recovered) {
    this.consultation = consultation;
    this.recovered = recovered;
  }

  // 복구된 원본의 최초 담당자 확인 기록 저장
  void confirmRecovery(String confirmedBy, Instant confirmedAt) {
    if (!Boolean.TRUE.equals(recovered) || consultation == null) {
      throw new IntakeConflictException();
    }

    if (recoveryConfirmedAt != null) {
      if (recoveryConfirmedAt.equals(confirmedAt) && recoveryConfirmedBy.equals(confirmedBy)) {
        return;
      }

      throw new IntakeConflictException();
    }

    this.recoveryConfirmedBy = confirmedBy;
    this.recoveryConfirmedAt = confirmedAt;
  }

  // 복구 확인 담당자와 확인 시각 조회
  String getRecoveryConfirmedBy() {
    return recoveryConfirmedBy;
  }

  Instant getRecoveryConfirmedAt() {
    return recoveryConfirmedAt;
  }
}