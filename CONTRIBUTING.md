# Контракт модели и правил

Модель и правила находятся в `payment-core/src/main/java/io/github/jeepearphone/payment/`. У всех классов пакет `io.github.jeepearphone.payment`. JDK 21 поддерживает синтаксис Java `record`.

| Файл | Сигнатура |
|---|---|
| Item.java | `public record Item(String id, String name, long price)` |
| GameState.java | `public record GameState(long balance, Map<String, Integer> inventory, List<Transaction> history)` |
| Transaction.java | `public record Transaction(long number, Instant time, TransactionType type, long amount, String itemId, long balanceAfter)` |
| PaymentRules.java | `final class PaymentRules` с двумя статическими методами, указанными ниже |

```java
static RuleResult credit(GameState current, long amount, Instant time)
static RuleResult purchase(GameState current, Map<String, Item> catalog, String itemId, Instant time)
```

`RuleResult`, `OperationStatus` и `TransactionType` уже объявлены в проекте. Сервис передаёт правилам текущее состояние, проверенный каталог и время. Правила возвращают `new RuleResult(status, state)`. Сохранением, часами, загрузкой каталога и сообщениями интерфейса управляет остальной проект.

## Поля и инварианты

- `Item` принимает непустые `id` и `name`, цена строго положительная. Нарушение вызовет `IllegalArgumentException`.
- `GameState` принимает баланс от нуля и выше. Значения в инвентаре строго положительные, ключи непустые. Храни свои копии карты и списка, доступные только для чтения. Начальное состояние создаётся как `new GameState(0, Map.of(), List.of())`.
- `Transaction` содержит положительный порядковый номер, время, тип и неотрицательный остаток. Начисление хранит положительную сумму и `itemId == null`. Покупка хранит отрицательную сумму и идентификатор непустой строкой. У каждого успешно применённого действия номер на единицу больше длины предшествующей истории.
- Для отказа правила возвращают прежнее состояние и соответствующий статус. Успешные действия возвращают новый снимок с добавленной в конец истории записью. Используй переданный `Instant` в записи.

## Примеры для самопроверки

В примерах в каталоге есть `new Item("shield", "Shield", 30)`. Прогоны используют независимые начальные состояния.

| Вход | Результат |
|---|---|
| Баланс 0, начисление 10 | `SUCCESS`, баланс 10, одна запись `CREDIT` с суммой 10. |
| Баланс 10, начисления 0 и −1 | `INVALID_AMOUNT`, тот же снимок для обоих действий. |
| Баланс `Long.MAX_VALUE`, начисление 1 | `OVERFLOW`, прежний снимок. |
| Баланс 50, покупка `shield` | `SUCCESS`, баланс 20, инвентарь `shield → 1`, запись `PURCHASE` на −30. |
| Баланс 30, покупка `shield` | `SUCCESS`, баланс 0. |
| Баланс 29, покупка `shield` | `INSUFFICIENT_FUNDS`, прежний снимок. |
| Покупка по неизвестному, пустому или отсутствующему идентификатору | `UNKNOWN_ITEM`, прежний снимок. |
| Баланс 70, две последовательные покупки щита | Баланс 10, два щита, номера операций 1 и 2. |
| Предмет уже представлен количеством `Integer.MAX_VALUE`, монет достаточно | `OVERFLOW`, прежний снимок. |

Запуск проверок из корня проекта — `.\mvnw.cmd -pl payment-core test`.
