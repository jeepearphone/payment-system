package io.github.jeepearphone.payment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class PaymentRules {
    private PaymentRules() {
    }

    static RuleResult credit(GameState current, long amount, Instant time) {
        if (amount <= 0) {
            return new RuleResult(OperationStatus.INVALID_AMOUNT, current);
        }
        if (amount > Long.MAX_VALUE - current.balance()) {
            return new RuleResult(OperationStatus.OVERFLOW, current);
        }

        return recordOperation(current, current.inventory(), time, TransactionType.CREDIT, amount, null);
    }

    static RuleResult purchase(GameState current, Map<String, Item> catalog, String itemId, Instant time) {
        Item item = itemId == null ? null : catalog.get(itemId);
        if (item == null) {
            return new RuleResult(OperationStatus.UNKNOWN_ITEM, current);
        }
        if (current.balance() < item.price()) {
            return new RuleResult(OperationStatus.INSUFFICIENT_FUNDS, current);
        }
        int owned = current.inventory().getOrDefault(itemId, 0);
        if (owned == Integer.MAX_VALUE) {
            return new RuleResult(OperationStatus.OVERFLOW, current);
        }

        var inventory = new HashMap<>(current.inventory());
        inventory.put(itemId, owned + 1);
        return recordOperation(current, inventory, time, TransactionType.PURCHASE, -item.price(), itemId);
    }

    private static RuleResult recordOperation(GameState current, Map<String, Integer> inventory,
                                              Instant time, TransactionType type, long amount, String itemId) {
        long balance = current.balance() + amount;
        var history = new ArrayList<>(current.history());
        history.add(new Transaction(history.size() + 1L, time, type, amount, itemId, balance));
        return new RuleResult(OperationStatus.SUCCESS, new GameState(balance, inventory, history));
    }
}
