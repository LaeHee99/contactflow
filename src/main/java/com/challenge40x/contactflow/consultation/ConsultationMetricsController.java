package com.challenge40x.contactflow.consultation;

import com.challenge40x.contactflow.consultation.ConsultationMetricsService.MetricsView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsultationMetricsController {

  private final ConsultationMetricsService metricsService;

  public ConsultationMetricsController(ConsultationMetricsService metricsService) {
    this.metricsService = metricsService;
  }

  // 상담별 집계 반환 및 상담 부재 시 404 응답
  @GetMapping("/api/consultations/{consultationId}/metrics")
  public ResponseEntity<MetricsView> getMetrics(
      @PathVariable("consultationId") long consultationId) {
    return ResponseEntity.of(metricsService.find(consultationId));
  }
}