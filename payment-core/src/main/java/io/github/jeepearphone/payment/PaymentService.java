package io.github.jeepearphone.payment;

import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Entry point for a host game. Use a single active service per save file. */
public final class PaymentService {
    private final StateStore store;
    private final Map<String, Item> catalog;
    private final Clock clock;
    private GameState state;

    private PaymentService(StateStore store, Map<String, Item> catalog, Clock clock, GameState state) {
        this.store = store;
        this.catalog = catalog;
        this.clock = clock;
        this.state = state;
    }

    public static PaymentService open(StateStore store, Collection<Item> items) throws IOException {
        return open(store, items, Clock.systemUTC());
    }

    static PaymentService open(StateStore store, Collection<Item> items, Clock clock) throws IOException {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(clock, "clock");

        Map<String, Item> catalog = new LinkedHashMap<>();
        for (Item item : items) {
            if (catalog.putIfAbsent(item.id(), item) != null) {
                throw new IllegalArgumentException("Duplicate item ID: " + item.id());
            }
        }

        GameState initialState = store.load()
                .orElseGet(() -> new GameState(0, Map.of(), List.of()));
        return new PaymentService(store, Collections.unmodifiableMap(catalog), clock, initialState);
    }

    public synchronized GameState snapshot() {
        return state;
    }

    public List<Item> catalog() {
        return List.copyOf(catalog.values());
    }

    public synchronized List<Transaction> history() {
        return state.history();
    }

    public synchronized OperationResult credit(long amount) {
        return commit(PaymentRules.credit(state, amount, clock.instant()));
    }

    public synchronized OperationResult purchase(String itemId) {
        return commit(PaymentRules.purchase(state, catalog, itemId, clock.instant()));
    }

    private OperationResult commit(RuleResult result) {
        if (result.status() != OperationStatus.SUCCESS) {
            return new OperationResult(result.status(), state.balance());
        }

        try {
            store.save(result.state());
        } catch (IOException exception) {
            return new OperationResult(OperationStatus.STORAGE_ERROR, state.balance());
        }

        state = result.state();
        return new OperationResult(OperationStatus.SUCCESS, state.balance());
    }
}
