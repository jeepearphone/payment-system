package io.github.jeepearphone.payment;

public record Item(String id, String name, long price) {
    public Item {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item ID and name must contain text");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
}
