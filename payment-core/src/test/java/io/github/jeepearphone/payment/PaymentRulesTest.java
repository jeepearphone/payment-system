package io.github.jeepearphone.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.jeepearphone.payment.OperationStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentRulesTest {
    private static final Instant TIME = Instant.parse("2026-11-20T00:00:00Z");
    private static final Map<String, Item> CATALOG = Map.of("shield", new Item("shield", "Shield", 30));

    @Test
    void creditsCoinsAndRecordsReward() {
        GameState original = state(0);
        RuleResult result = PaymentRules.credit(original, 10, TIME);

        assertEquals(SUCCESS, result.status());
        assertEquals(10, result.state().balance());
        assertEquals(List.of(new Transaction(1, TIME, TransactionType.CREDIT, 10, null, 10)),
                result.state().history());
        assertEquals(state(0), original);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveCredits(long amount) {
        GameState original = state(10);
        assertEquals(new RuleResult(INVALID_AMOUNT, original), PaymentRules.credit(original, amount, TIME));
    }

    @Test
    void rejectsBalanceOverflow() {
        GameState original = state(Long.MAX_VALUE);
        assertEquals(new RuleResult(OVERFLOW, original), PaymentRules.credit(original, 1, TIME));
    }

    @Test
    void purchasesItemAndCreatesHistoryEntry() {
        GameState original = state(50);
        RuleResult result = PaymentRules.purchase(original, CATALOG, "shield", TIME);

        assertEquals(SUCCESS, result.status());
        assertEquals(20, result.state().balance());
        assertEquals(Map.of("shield", 1), result.state().inventory());
        assertEquals(List.of(new Transaction(1, TIME, TransactionType.PURCHASE, -30, "shield", 20)),
                result.state().history());
        assertEquals(state(50), original);
    }

    @Test
    void spendsExactBalance() {
        RuleResult result = PaymentRules.purchase(state(30), CATALOG, "shield", TIME);
        assertEquals(SUCCESS, result.status());
        assertEquals(0, result.state().balance());
        assertEquals(Map.of("shield", 1), result.state().inventory());
    }

    @Test
    void rejectsPurchaseWithInsufficientFunds() {
        GameState original = state(29);
        assertEquals(new RuleResult(INSUFFICIENT_FUNDS, original),
                PaymentRules.purchase(original, CATALOG, "shield", TIME));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", " "})
    void rejectsUnknownItems(String itemId) {
        GameState original = state(50);
        assertEquals(new RuleResult(UNKNOWN_ITEM, original),
                PaymentRules.purchase(original, CATALOG, itemId, TIME));
    }

    @Test
    void accumulatesItemsAndNumbersPurchases() {
        RuleResult first = PaymentRules.purchase(state(70), CATALOG, "shield", TIME);
        RuleResult second = PaymentRules.purchase(first.state(), CATALOG, "shield", TIME.plusSeconds(1));

        assertEquals(SUCCESS, second.status());
        assertEquals(10, second.state().balance());
        assertEquals(Map.of("shield", 2), second.state().inventory());
        assertEquals(List.of(1L, 2L), second.state().history().stream().map(Transaction::number).toList());
    }

    @Test
    void rejectsInventoryOverflow() {
        GameState original = new GameState(50, Map.of("shield", Integer.MAX_VALUE), List.of());
        assertEquals(new RuleResult(OVERFLOW, original),
                PaymentRules.purchase(original, CATALOG, "shield", TIME));
    }

    @Test
    void stateKeepsReadOnlyCopiesOfCollections() {
        var inventory = new HashMap<>(Map.of("shield", 1));
        var history = new ArrayList<>(List.of(new Transaction(1, TIME, TransactionType.CREDIT, 10, null, 10)));
        GameState state = new GameState(10, inventory, history);

        inventory.clear();
        history.clear();
        assertEquals(Map.of("shield", 1), state.inventory());
        assertEquals(1, state.history().size());
        assertThrows(UnsupportedOperationException.class, () -> state.inventory().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.history().clear());
    }

    @Test
    void rejectsInvalidItemPrices() {
        assertThrows(IllegalArgumentException.class, () -> new Item("shield", "Shield", 0));
    }

    private static GameState state(long balance) {
        return new GameState(balance, Map.of(), List.of());
    }
}
