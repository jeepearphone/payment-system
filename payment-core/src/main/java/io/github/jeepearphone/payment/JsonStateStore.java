package io.github.jeepearphone.payment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.Strictness;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Persists the balance, inventory and history as one versioned JSON snapshot. */
public final class JsonStateStore implements StateStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (time, type, context) -> new JsonPrimitive(time.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (value, type, context) -> Instant.parse(value.getAsString()))
            .create();

    private final Path file;

    public JsonStateStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    @Override
    public Optional<GameState> load() throws IOException {
        Reader input;
        try {
            input = Files.newBufferedReader(file);
        } catch (NoSuchFileException exception) {
            return Optional.empty();
        }

        try (input) {
            SavedGame saved = GSON.fromJson(input, SavedGame.class);
            if (saved == null || saved.version() != FORMAT_VERSION || saved.state() == null) {
                throw new IOException("Unsupported or incomplete save: " + file);
            }
            return Optional.of(saved.state().toState());
        } catch (RuntimeException exception) {
            throw new IOException("Invalid save: " + file, exception);
        }
    }

    @Override
    public void save(GameState state) throws IOException {
        Path parent = file.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".wallet-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(new SavedGame(FORMAT_VERSION, SaveState.from(state)), writer);
            }
            Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (IOException | JsonParseException failure) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                failure.addSuppressed(cleanupError);
            }
            throw new IOException("Could not save profile: " + file, failure);
        }
    }

    private record SavedGame(int version, SaveState state) {
    }

    private record SaveState(Long balance, java.util.Map<String, Integer> inventory,
                             java.util.List<Transaction> history) {
        static SaveState from(GameState state) {
            return new SaveState(state.balance(), state.inventory(), state.history());
        }

        GameState toState() throws IOException {
            if (balance == null || inventory == null || history == null) {
                throw new IOException("Save must contain balance, inventory and history");
            }

            return new GameState(balance, inventory, history);
        }
    }
}
