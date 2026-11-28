# Встраиваемая игровая платёжная система

Курсовой проект Павла Карелина. Java-модуль для покупок за виртуальную валюту и демонстрационная игра для проверки интеграции.

- `payment-core` — библиотека, которую подключает игра.
- `demo-game` — демонстрационное приложение.

## Сборка и запуск на Windows

Для сборки нужен JDK 21 или новее. Проверенная конфигурация — Windows 11 x64 и OpenJDK 24.0.2. Если системная команда `java` запускает другую версию, задайте `JAVA_HOME` равным пути к каталогу JDK. При первом запуске Maven Wrapper скачивает Maven и зависимости.

Для новой установки откройте PowerShell и получите исходники:

```powershell
git clone https://github.com/jeepearphone/payment-system.git
Set-Location payment-system
```

Из корня уже открытого проекта выполните:

```powershell
.\mvnw.cmd verify
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
& $java --enable-native-access=ALL-UNNAMED -cp "demo-game/target/demo-game-0.1.0-SNAPSHOT.jar;demo-game/target/lib/*" io.github.jeepearphone.demo.DemoGame
```

Двигайтесь клавишами WASD или стрелками и подберите три монеты. Откройте магазин клавишей E или кнопкой **Shop**. Купите щит за 30 монет клавишей 1 или кнопкой **Buy**. Повторите покупку, чтобы увидеть отказ при недостаточном балансе. Клавиша H открывает историю, Page Up и Page Down меняют страницу. Tab возвращает на поле. После перезапуска игра использует тот же профиль `demo-save/profile.json`.

## Подключение библиотеки

Для подключения к другой Java-игре выполните `.\mvnw.cmd -pl payment-core -am install` из корня этого репозитория. Maven установит в локальный репозиторий библиотеку и её родительский POM. Затем добавьте `io.github.jeepearphone:payment-core:0.1.0-SNAPSHOT` в зависимости приложения. Пример объявления зависимости находится в [POM демонстрационной игры](demo-game/pom.xml).

```java
var catalog = List.of(new Item("shield", "Shield", 30));
var wallet = PaymentService.open(new JsonStateStore(Path.of("profile.json")), catalog);
wallet.credit(30);
OperationResult result = wallet.purchase("shield");
```

Классы модуля находятся в пакете `io.github.jeepearphone.payment`. Хост вызывает методы сервиса и выводит статус операции. Для чтения баланса и инвентаря используйте `snapshot()`, для журнала — `history()`. В одном процессе держите один открытый сервис на файл профиля.

## Лицензии

Исходный код проекта распространяется под лицензией MIT.

- [Gson, Apache-2.0](https://github.com/google/gson/blob/gson-parent-2.13.2/LICENSE)
- [Jaylib, GPLv3 с Classpath Exception](https://github.com/electronstudio/jaylib/blob/master/REAL-LICENSE.txt)
- [raylib, zlib/libpng](https://www.raylib.com/license.html)
- [JUnit, EPL-2.0](https://junit.org/junit5/docs/5.13.4/user-guide/)
- [Maven Wrapper, Apache-2.0](https://maven.apache.org/tools/wrapper/license.html)
