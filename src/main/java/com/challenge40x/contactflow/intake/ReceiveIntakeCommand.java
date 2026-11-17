package com.challenge40x.contactflow.intake;

public record ReceiveIntakeCommand(
    IntakeSource source,
    String externalId,
    String customerKey
) {
}