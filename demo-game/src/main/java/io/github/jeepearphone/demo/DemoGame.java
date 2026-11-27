package io.github.jeepearphone.demo;

import io.github.jeepearphone.payment.GameState;
import io.github.jeepearphone.payment.Item;
import io.github.jeepearphone.payment.JsonStateStore;
import io.github.jeepearphone.payment.OperationResult;
import io.github.jeepearphone.payment.PaymentService;
import io.github.jeepearphone.payment.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;
import static io.github.jeepearphone.payment.OperationStatus.SUCCESS;

public final class DemoGame {
    private static final int FIELD_LEFT = 28;
    private static final int FIELD_TOP = 136;
    private static final int FIELD_RIGHT = 852;
    private static final int FIELD_BOTTOM = 470;
    private static final int PAGE_SIZE = 6;
    private static final long COIN_VALUE = 10;
    private static final int[][] COIN_SPOTS = {{280, 272}, {480, 345}, {660, 220}};

    private final PaymentService wallet;
    private Screen screen = Screen.FIELD;
    private float playerX = 116;
    private float playerY = 272;
    private int coinSpot;
    private int historyOffset;
    private boolean onCoin;
    private String notice = "Find a coin on the field.";

    private DemoGame(PaymentService wallet) {
        this.wallet = wallet;
    }

    public static void main(String[] args) {
        Path save = Path.of("demo-save", "profile.json");
        try {
            var catalog = List.of(new Item("shield", "Shield", 30), new Item("lantern", "Lantern", 20));
            var wallet = PaymentService.open(new JsonStateStore(save), catalog);
            new DemoGame(wallet).run();
        } catch (IOException exception) {
            System.err.println("Could not load the profile at " + save.toAbsolutePath());
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    private void run() {
        GameWindow.open();
        try {
            while (!WindowShouldClose()) {
                update();
                draw();
            }
        } finally {
            CloseWindow();
        }
    }

    private void update() {
        if (IsKeyPressed(KEY_E) || clicked(440, 74, 126, 34)) {
            screen = Screen.SHOP;
        } else if (IsKeyPressed(KEY_H) || clicked(576, 74, 126, 34)) {
            screen = Screen.HISTORY;
            historyOffset = 0;
        } else if (IsKeyPressed(KEY_TAB) || clicked(712, 74, 140, 34)) {
            screen = Screen.FIELD;
        }

        switch (screen) {
            case FIELD -> movePlayer();
            case SHOP -> {
                List<Item> items = wallet.catalog();
                for (int index = 0; index < items.size(); index++) {
                    int top = 174 + index * 135;
                    if (IsKeyPressed(KEY_ONE + index) || clicked(672, top + 34, 146, 46)) {
                        buy(items.get(index));
                    }
                }
            }
            case HISTORY -> {
                int maximum = Math.max(0, wallet.history().size() - PAGE_SIZE);
                if (IsKeyPressed(KEY_PAGE_UP)) {
                    historyOffset = Math.min(maximum, historyOffset + PAGE_SIZE);
                }
                if (IsKeyPressed(KEY_PAGE_DOWN)) {
                    historyOffset = Math.max(0, historyOffset - PAGE_SIZE);
                }
            }
        }
    }

    private void movePlayer() {
        float distance = Math.min(GetFrameTime(), 0.05f) * 230;
        if (IsKeyDown(KEY_A) || IsKeyDown(KEY_LEFT)) playerX -= distance;
        if (IsKeyDown(KEY_D) || IsKeyDown(KEY_RIGHT)) playerX += distance;
        if (IsKeyDown(KEY_W) || IsKeyDown(KEY_UP)) playerY -= distance;
        if (IsKeyDown(KEY_S) || IsKeyDown(KEY_DOWN)) playerY += distance;

        playerX = Math.clamp(playerX, FIELD_LEFT + 16, FIELD_RIGHT - 16);
        playerY = Math.clamp(playerY, FIELD_TOP + 16, FIELD_BOTTOM - 16);
        int[] coin = COIN_SPOTS[coinSpot];
        boolean touching = Math.hypot(playerX - coin[0], playerY - coin[1]) < 27;

        if (touching && !onCoin) {
            OperationResult result = wallet.credit(COIN_VALUE);
            if (result.status() == SUCCESS) {
                coinSpot = (coinSpot + 1) % COIN_SPOTS.length;
                notice = "Collected " + COIN_VALUE + " coins.";
            } else {
                notice = operationMessage(result);
            }
        }
        onCoin = touching;
    }

    private void buy(Item item) {
        OperationResult result = wallet.purchase(item.id());
        notice = switch (result.status()) {
            case SUCCESS -> "Purchased: " + item.name();
            default -> operationMessage(result);
        };
    }

    private void draw() {
        GameState state = wallet.snapshot();
        BeginDrawing();
        ClearBackground(RAYWHITE);
        DrawText("Coin Workshop", 28, 24, 32, DARKBLUE);
        DrawText("Coins: " + state.balance(), 30, 82, 22, DARKGRAY);
        drawTab("E  Shop", 440, 126, Screen.SHOP);
        drawTab("H  History", 576, 126, Screen.HISTORY);
        drawTab("Tab  Field", 712, 140, Screen.FIELD);
        DrawLine(28, 120, 852, 120, LIGHTGRAY);

        switch (screen) {
            case FIELD -> drawField();
            case SHOP -> drawShop(state);
            case HISTORY -> drawHistory(state.history());
        }

        DrawRectangle(28, 500, 824, 36, LIGHTGRAY);
        DrawText(notice, 40, 509, 18, DARKBLUE);
        EndDrawing();
    }

    private void drawField() {
        DrawRectangle(FIELD_LEFT, FIELD_TOP, FIELD_RIGHT - FIELD_LEFT, FIELD_BOTTOM - FIELD_TOP, DARKBLUE);
        for (int x = FIELD_LEFT + 36; x < FIELD_RIGHT; x += 36) {
            for (int y = FIELD_TOP + 36; y < FIELD_BOTTOM; y += 36) {
                DrawCircle(x, y, 1, SKYBLUE);
            }
        }
        int[] coin = COIN_SPOTS[coinSpot];
        DrawCircle(coin[0], coin[1], 15, ORANGE);
        DrawCircle(coin[0], coin[1], 11, GOLD);
        DrawRectangle((int) playerX - 12, (int) playerY - 12, 24, 24, SKYBLUE);
        DrawText("Move with WASD or arrow keys.", 30, 478, 16, DARKGRAY);
    }

    private void drawShop(GameState state) {
        DrawText("Shop", 30, 136, 24, DARKBLUE);
        List<Item> items = wallet.catalog();
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            int top = 174 + index * 135;
            DrawRectangle(28, top, 824, 112, LIGHTGRAY);
            DrawText(item.name(), 52, top + 24, 25, DARKBLUE);
            int owned = state.inventory().getOrDefault(item.id(), 0);
            DrawText(item.price() + " coins   |   Owned: " + owned, 54, top + 63, 19, DARKGRAY);
            DrawRectangle(672, top + 34, 146, 46, DARKBLUE);
            DrawText((index + 1) + "   Buy", 694, top + 48, 18, RAYWHITE);
        }
    }

    private void drawHistory(List<Transaction> history) {
        DrawText("Transaction history", 30, 136, 24, DARKBLUE);
        if (history.isEmpty()) {
            DrawText("No transactions yet.", 32, 188, 20, DARKGRAY);
            return;
        }

        DrawText("No.      Action                      Change          Balance", 46, 180, 18, DARKGRAY);
        int last = history.size() - historyOffset;
        for (int row = 0; row < PAGE_SIZE && last - row > 0; row++) {
            Transaction transaction = history.get(last - row - 1);
            int y = 210 + row * 41;
            DrawRectangle(28, y, 824, 36, LIGHTGRAY);
            DrawText(Long.toString(transaction.number()), 46, y + 9, 18, DARKBLUE);
            String action = switch (transaction.type()) {
                case CREDIT -> "Coin collected";
                case PURCHASE -> "Item: " + transaction.itemId();
            };
            DrawText(action, 122, y + 9, 18, DARKBLUE);
            DrawText("%+d".formatted(transaction.amount()), 362, y + 9, 18, DARKBLUE);
            DrawText(Long.toString(transaction.balanceAfter()), 500, y + 9, 18, DARKBLUE);
        }
        DrawText("Page Up: older   |   Page Down: newer", 30, 474, 16, DARKGRAY);
    }

    private void drawTab(String label, int x, int width, Screen tab) {
        boolean active = screen == tab;
        DrawRectangle(x, 74, width, 34, active ? DARKBLUE : LIGHTGRAY);
        DrawText(label, x + 12, 83, 17, active ? RAYWHITE : DARKBLUE);
    }

    private static boolean clicked(int x, int y, int width, int height) {
        return IsMouseButtonPressed(MOUSE_BUTTON_LEFT)
                && GetMouseX() >= x && GetMouseX() < x + width
                && GetMouseY() >= y && GetMouseY() < y + height;
    }

    private static String operationMessage(OperationResult result) {
        return switch (result.status()) {
            case SUCCESS -> "Operation complete.";
            case INVALID_AMOUNT -> "Invalid amount.";
            case UNKNOWN_ITEM -> "Unknown item.";
            case INSUFFICIENT_FUNDS -> "Not enough coins.";
            case OVERFLOW -> "Balance or inventory limit reached.";
            case STORAGE_ERROR -> "Could not save the profile. Try again.";
        };
    }

    private enum Screen {
        FIELD, SHOP, HISTORY
    }
}
