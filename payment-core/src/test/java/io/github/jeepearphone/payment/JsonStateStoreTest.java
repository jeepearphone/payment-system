package io.github.jeepearphone.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonStateStoreTest {
    @TempDir
    Path directory;

    @Test
    void replacesTheSaveAndRestoresEveryField() throws IOException {
        Path path = directory.resolve("profile.json");
        var store = new JsonStateStore(path);
        Instant time = Instant.parse("2026-11-20T00:00:00Z");
        var credit = new Transaction(1, time, TransactionType.CREDIT, 50, null, 50);
        var purchase = new Transaction(2, time.plusSeconds(1), TransactionType.PURCHASE, -30, "shield", 20);

        store.save(new GameState(50, Map.of(), List.of(credit)));
        var latest = new GameState(20, Map.of("shield", 1), List.of(credit, purchase));
        store.save(latest);

        assertEquals(latest, new JsonStateStore(path).load().orElseThrow());
        try (var files = Files.list(directory)) {
            assertEquals(List.of(path), files.toList());
        }
    }

    @Test
    void failedReplacementKeepsTheDestinationAndRemovesTheTemporaryFile() throws IOException {
        Path destination = Files.createDirectory(directory.resolve("profile.json"));
        Path marker = Files.writeString(destination.resolve("keep.txt"), "keep");
        var store = new JsonStateStore(destination);

        assertThrows(IOException.class, () -> store.save(new GameState(0, Map.of(), List.of())));
        assertEquals("keep", Files.readString(marker));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(destination), files.toList());
        }
    }
}
