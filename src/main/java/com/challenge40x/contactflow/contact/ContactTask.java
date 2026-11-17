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

  private enum Status {
    PENDING
  }

  private enum TaskType {
    FIRST_CONTACT
  }
}