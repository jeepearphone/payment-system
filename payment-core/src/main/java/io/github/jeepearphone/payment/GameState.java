package io.github.jeepearphone.payment;

import java.util.List;
import java.util.Map;

public record GameState(long balance, Map<String, Integer> inventory, List<Transaction> history) {
    public GameState {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance must be nonnegative");
        }

        inventory = Map.copyOf(inventory);
        history = List.copyOf(history);
        for (var entry : inventory.entrySet()) {
            if (entry.getKey().isBlank() || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Inventory requires item IDs and positive quantities");
            }
        }
    }
}
