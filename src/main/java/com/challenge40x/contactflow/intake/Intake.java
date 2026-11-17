package com.challenge40x.contactflow.intake;

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

  protected Intake() {
  }

  public Intake(IntakeSource source, String externalId, String customerKey) {
    this.source = source;
    this.externalId = externalId;
    this.customerKey = customerKey;
  }

  public Long getId() {
    return id;
  }
}