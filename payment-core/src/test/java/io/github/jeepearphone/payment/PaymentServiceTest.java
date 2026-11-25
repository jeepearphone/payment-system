package io.github.jeepearphone.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.jeepearphone.payment.OperationStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {
    private static final Item SHIELD = new Item("shield", "Shield", 30);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path directory;

    @Test
    void startsWithEmptyProfile() throws IOException {
        var service = open(new JsonStateStore(directory.resolve("profile.json")));
        assertEquals(new GameState(0, Map.of(), List.of()), service.snapshot());
        assertEquals(List.of(SHIELD), service.catalog());
        assertTrue(service.history().isEmpty());
    }

    @Test
    void rejectsDuplicateCatalogIds() {
        var store = new JsonStateStore(directory.resolve("profile.json"));
        assertThrows(IllegalArgumentException.class, () -> PaymentService.open(store, List.of(SHIELD, SHIELD)));
    }

    @Test
    void purchaseSurvivesReopening() throws IOException {
        var store = new JsonStateStore(directory.resolve("profile.json"));
        var service = open(store);

        assertEquals(new OperationResult(SUCCESS, 50), service.credit(50));
        assertEquals(new OperationResult(SUCCESS, 20), service.purchase("shield"));
        assertEquals(new OperationResult(INSUFFICIENT_FUNDS, 20), service.purchase("shield"));

        var reopened = open(store);
        assertEquals(20, reopened.snapshot().balance());
        assertEquals(Map.of("shield", 1), reopened.snapshot().inventory());
        assertEquals(List.of(
                new Transaction(1, CLOCK.instant(), TransactionType.CREDIT, 50, null, 50),
                new Transaction(2, CLOCK.instant(), TransactionType.PURCHASE, -30, "shield", 20)
        ), reopened.history());
    }

    @Test
    void preservesMalformedSave() throws IOException {
        Path path = directory.resolve("profile.json");
        Files.writeString(path, "{broken");

        assertThrows(IOException.class, () -> open(new JsonStateStore(path)));
        assertEquals("{broken", Files.readString(path));
    }

    @Test
    void rejectsSaveMissingBalance() throws IOException {
        Path path = directory.resolve("profile.json");
        Files.writeString(path, """
                {"version":1,"state":{"inventory":{},"history":[]}}
                """);

        assertThrows(IOException.class, () -> open(new JsonStateStore(path)));
    }

    @Test
    void keepsCommittedStateOnFailureAndAllowsRetry() throws IOException {
        var underlying = new JsonStateStore(directory.resolve("profile.json"));
        var store = new FailingStore(underlying);
        var service = open(store);
        assertEquals(SUCCESS, service.credit(50).status());
        GameState original = service.snapshot();
        store.failWrites = true;

        assertEquals(new OperationResult(STORAGE_ERROR, 50), service.purchase("shield"));
        assertEquals(original, service.snapshot());
        assertEquals(original, open(underlying).snapshot());

        store.failWrites = false;
        assertEquals(new OperationResult(SUCCESS, 20), service.purchase("shield"));
        assertEquals(Map.of("shield", 1), service.snapshot().inventory());
        assertEquals(2, service.history().size());
    }

    private static PaymentService open(StateStore store) throws IOException {
        return PaymentService.open(store, List.of(SHIELD), CLOCK);
    }

    private static final class FailingStore implements StateStore {
        private final StateStore delegate;
        private boolean failWrites;

        private FailingStore(StateStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<GameState> load() throws IOException {
            return delegate.load();
        }

        @Override
        public void save(GameState state) throws IOException {
            if (failWrites) {
                throw new IOException("simulated write failure");
            }
            delegate.save(state);
        }
    }
}
