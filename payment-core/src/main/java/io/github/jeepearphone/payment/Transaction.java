package io.github.jeepearphone.payment;

import java.time.Instant;
import java.util.Objects;

public record Transaction(long number, Instant time, TransactionType type, long amount,
                          String itemId, long balanceAfter) {
    public Transaction {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(type, "type");
        if (number <= 0 || balanceAfter < 0) {
            throw new IllegalArgumentException("Invalid transaction number or resulting balance");
        }

        boolean valid = switch (type) {
            case CREDIT -> amount > 0 && itemId == null;
            case PURCHASE -> amount < 0 && itemId != null && !itemId.isBlank();
        };
        if (!valid) {
            throw new IllegalArgumentException("Amount and item ID must match the transaction type");
        }
    }
}
