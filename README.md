# RadioModem POG — MT63 KISS/TCP TNC (Android)

**Android 10+ (ARM64), Kotlin, AGP 8.13, Gradle 8.13, Java 17.**
Тёмная тема по умолчанию, EN→RU локали. Без иконок, чистые LF-окончания.

## Назначение
Преобразует смартфон + рацию (через аудиокабель) в ПО‑TNC:
- **KISS over TCP** сервер для Sideband/Reticulum/Meshchat;
- **MT63‑2000** модуляция/демодуляция через аудиотракт;
- Диагностика: осциллограф RX, спектр поднесущих, метрики BER/SNR.

## Сборка
Открыть в Android Studio Narwhal 3. Цели: `arm64-v8a`.
minSdk 29, targetSdk 35.

## Быстрый старт
1. Подключите телефон к рации аудиокабелем (TX↔MIC, RX↔SPK).
2. Включите **Foreground Service** на главном экране.
3. Подключайте клиент KISS (например Sideband): `tcp://127.0.0.1:8100`.
4. Профили MT63 лежат в `res/raw/profiles.json`. Выберите в настройках.

## Настройки
- **Sample rate / FFT / CP / carriers / pilots** — из профиля или вручную.
- **FEC (Hadamard) + Interleave (legacy/short/long)** — мягкая коррекция ошибок.
- **Language/Theme** — язык и тема приложения.

## Архитектура
- `modem/MT63Modem` — аудио I/O, очереди TX, метрики, glue.
- `modem/MT63Modulator` — OFDM/QPSK, пилоты, CP, FEC 64×64 блоками.
- `modem/MT63Demodulator` — FFT, PLL/CFO, накопление 64 символов, деинтерливинг, Hadamard soft‑decode.
- `net/KISSServer` — KISS over TCP.
- `service/ModemForegroundService` — Foreground‑сервис.
- `ui/*` — главный экран, настройки, лог, помощь, осциллограф, спектр.

## Тесты
`src/test` содержит e2e‑тесты без Android зависимостей.

## Известные ограничения
- Оценка BER теоретическая (по SNR), не измерительный BER.
- Полная кросс‑совместимость MT63‑2000 зависит от параметров профиля и дальнейшей калибровки.

## Лицензия
Сохранена исходная лицензия репозитория (если была). Если отсутствует — добавьте по необходимости.

