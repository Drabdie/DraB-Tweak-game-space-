# DraB Tweak

Профессиональный no-root Android system manager с опциональной интеграцией Shizuku.

## Что нового в 1.1.0

- **Реальный Shizuku-мост**: нативный `ShellService` (AIDL UserService) исполняет команды от shell-UID через `Shizuku.bindUserService`. Fallback-строки «bridge недоступен» больше нет.
- **Профили теперь реально применяются**: `settings put global` для window/transition/animator animation scale + управление Doze (`dumpsys deviceidle enable/disable/force-idle/unforce`).
- **Честная верификация**: после каждой правки значение читается назад (`settings get global`) и сверяется; в диалоге результата видно OK / FAIL / SKIP по каждой команде.
- **Заморозка приложений**: `pm suspend --user 0` / `pm unsuspend` для выбранных пользовательских приложений.
- **Очистка кэшей**: `pm trim-caches 512G`.
- **Auto Game Mode**: при включении Usage Access приложение само применяет Game-профиль, когда открыта игра, и откатывает изменения после выхода.
- **Снимок и откат**: перед применением сохраняются исходные значения анимаций; Restore в Tools возвращает их.
- **Монитор**: RAM, батарея + температура, хранилище, частота CPU (через Shizuku), починено обновление статусов (в 1.0 значения RAM/BATTERY оставались «loading» навсегда).
- **Исправлен краш при запуске в 1.0.x**: класс `rikka.shizuku.ShizukuProvider` отсутствовал в APK (не был подключён артефакт `dev.rikka.shizuku:provider`).
- Память выбранного профиля, fallback-применение анимаций через WRITE_SETTINGS без Shizuku, диалог About со статусом разрешений.

Приложение намеренно не имитирует доступ к CPU governor, GPU frequency или `/sys`: неподдерживаемые операции должны оставаться честно недоступными.

## Сборка

```bash
export JAVA_HOME=/path/to/jdk21
export ANDROID_SDK_ROOT=/path/to/android-sdk
/path/to/gradle-8.10.2/bin/gradle assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`, готовый — в `dist/DraB-Tweak-1.1.0.apk`.

## Проверка

```bash
./verify_build.sh        # dex/aapt/apksigner статические проверки
gradle lint              # 0 ошибок
```

Рантайм-проверка Shizuku-операций требует реального устройства с запущенным Shizuku.

## Ограничения

- Без Shizuku доступны только анимации (через WRITE_SETTINGS) и мониторинг.
- Doze/заморозка/trim требуют Shizuku в ADB-режиме или root.
- CPU governor, GPU, `/sys` — только root (не поддерживается и не имитируется).
