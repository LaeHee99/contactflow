package com.challenge40x.contactflow.intake;

import com.challenge40x.contactflow.consultation.Consultation;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consultation_id")
  private Consultation consultation;

  protected Intake() {
  }

  public Long getId() {
    return id;
  }

  // 저장된 고객 식별자와 요청의 고객 식별자 비교
  boolean hasCustomerKey(String customerKey) {
    return this.customerKey.equals(customerKey);
  }

  // 연결 대상 상담의 고객 일치 확인에 사용할 식별자 반환
  String getCustomerKey() {
    return customerKey;
  }

  // 원본 접수에 연결된 상담 조회
  Consultation getConsultation() {
    return consultation;
  }

  // 원본 접수에 상담 연결
  void linkConsultation(Consultation consultation) {
    this.consultation = consultation;
  }
}