package io.github.jeepearphone.payment;

/** The status and committed balance after a command. */
public record OperationResult(OperationStatus status, long balance) {
}
