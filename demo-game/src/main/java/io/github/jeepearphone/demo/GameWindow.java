package io.github.jeepearphone.demo;

import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;

final class GameWindow {
    static final int WIDTH = 880;
    static final int HEIGHT = 560;

    private GameWindow() {
    }

    static void open() {
        InitWindow(WIDTH, HEIGHT, "Coin Workshop");
        if (!IsWindowReady()) {
            throw new IllegalStateException("raylib could not open a window");
        }
        SetTargetFPS(60);
    }

    public static void main(String[] args) {
        open();
        try {
            BeginDrawing();
            ClearBackground(RAYWHITE);
            DrawText("Coin Workshop", 32, 32, 32, DARKBLUE);
            DrawText("Coins 0123456789", 32, 84, 22, DARKGRAY);
            EndDrawing();
            System.out.println("raylib opened a window and rendered a frame with text.");
        } finally {
            CloseWindow();
        }
    }
}
