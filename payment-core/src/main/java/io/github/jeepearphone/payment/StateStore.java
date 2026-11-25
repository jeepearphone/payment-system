package io.github.jeepearphone.payment;

import java.io.IOException;
import java.util.Optional;

/** Storage for the committed snapshot of one player profile. */
public interface StateStore {
    Optional<GameState> load() throws IOException;

    void save(GameState state) throws IOException;
}
