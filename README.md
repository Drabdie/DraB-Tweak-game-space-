# DraB Tweak

Профессиональный no-root Android system manager с опциональной интеграцией Shizuku.

## Что делает приложение (v2.0)

Все привилегированные действия выполняются через Shizuku UserService (`IShellService.aidl` выполняется внутри процесса Shizuku — от пользователя shell в ADB-режиме или root в root-режиме). Приложение никогда не имитирует успех: если система отказала в операции, пользователь видит ошибку.

### Профили (реальное применение)
- Performance / Balanced / Battery / Game Mode.
- Анимации (`window/transition/animator_animation_scale`), Battery Saver (`low_power`), app standby, `am kill-all` для освобождения RAM.
- Перед первой правкой создаётся снимок изменяемых настроек; «Restore all» возвращает всё обратно.

### Менеджер приложений (экран Apps)
- Список всех приложений с бейджами SYSTEM / DISABLED / SUSPENDED.
- Действия: force-stop, disable (freeze), re-enable, suspend, un-suspend, uninstall для user 0 (данные сохраняются, восстановление — `cmd package install-existing`), standby-бакеты `restricted` / `active`.

### App Ops
- Тонкий контроль разрешений для каждого приложения: камера, микрофон, геолокация, буфер обмена, фоновая работа, wake lock — через `cmd appops set` (ignore / default).
- Неподдерживаемые OEM-опы честно возвращают ошибку.

### Debloat
- Каталог безопасных к удалению пакетов (safe / optional / risky) — показываются только установленные.
- Удаление через `pm uninstall -k --user 0` с подтверждением; APK и данные остаются на диске, восстановление одной командой.

### Мониторинг (только чтение)
- CPU load из `/proc/stat`, частоты ядер из sysfs (только чтение), RAM, батарея (уровень, температура, ток), хранилище.
- Возможности честно размечены: без root / через Shizuku / только root.

### Инструменты
- Trim all caches (`pm trim-caches`), kill background processes (`am kill-all`), вход/выход из Doze (`dumpsys deviceidle force-idle` / `step`).
- Restore all settings (снимок всех изменённых настроек).
- Плитка быстрых настроек для переключения Performance/Battery.

## Что НЕ делает приложение (принципиально)
- CPU governor, GPU frequency, записи в `/sys` и ядро — требуют root и НЕ имитируются.
- Никаких скрытых телеметрических функций и обхода систем защиты.

## Сборка

```bash
export JAVA_HOME=/path/to/jdk21
export ANDROID_SDK_ROOT=/path/to/android-sdk
gradle assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.
CI собирает APK автоматически при пуше в main (artifact в Actions).

## Зависимости
- [Shizuku](https://shizuku.rikka.app/) — для привилегированных действий (не обязателен: приложение работает и как монитор).
- `QUERY_ALL_PACKAGES` — нужен для списка всех приложений (приложение для power users, распространяется вне Play Store).

## Ограничения
Минимальная версия Android — API 24 (Android 7.0). Доступность shell-команд зависит от версии Android и прошивки производителя. Приложение не требует root и не выполняет необратимых низкоуровневых изменений.
