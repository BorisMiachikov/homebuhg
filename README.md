# Домашняя бухгалтерия (HomeBuhg)

Android-приложение для семейного учёта личных финансов на Kotlin + Jetpack Compose.

## Стек

- **Kotlin 2.0** + Coroutines + Flow + Serialization
- **Jetpack Compose** (Material 3, dynamic color), **Navigation Compose** type-safe
- **Hilt** для DI
- **Room 2.6** (offline-first) + KSP
- **Firebase** Auth + Firestore (для синхронизации между устройствами семьи)
- **WorkManager** (регулярные операции, фоновая синхронизация)
- **CameraX + ML Kit Barcode** (сканер QR ФНС)
- **Vico Charts** (графики)
- **Retrofit + Moshi** (API налоговой)

minSdk 29, targetSdk 35, JDK 17.

## Первый запуск

1. Установить **Android Studio Ladybug** или новее (включает Android SDK 35, JDK 17).
   - Скачать: https://developer.android.com/studio
2. Открыть директорию `F:\Project\kotlin\homebuhg` через **File → Open** в Android Studio.
3. Дождаться окончания **Gradle Sync** — Android Studio сам сгенерирует `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, скачает все зависимости и Android SDK.
4. Если Sync ругается на `local.properties` — создать его в корне:
   ```properties
   sdk.dir=C\:\\Users\\<ваш_пользователь>\\AppData\\Local\\Android\\Sdk
   ```
5. Запустить конфигурацию `app` на эмуляторе или подключённом устройстве (Android 10+).

## Firebase (опционально, для синхронизации)

До добавления `app/google-services.json` приложение работает в локальном режиме (без auth и sync).

Чтобы включить Firebase:
1. Создать проект в [Firebase Console](https://console.firebase.google.com).
2. Добавить Android-приложение с package `ru.homebuhg` (debug: `ru.homebuhg.debug`).
3. Скачать `google-services.json` и положить в `app/`.
4. В консоли Firebase включить **Authentication → Google Sign-In** и **Firestore**.
5. Применить security rules из `firestore.rules` (будет создан в Stage 5).

## Сборка из CLI

После первого Sync в Android Studio:

```bash
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit-тесты
./gradlew connectedAndroidTest   # инструментальные тесты на устройстве
./gradlew lint                   # статический анализ
./gradlew bundleRelease          # AAB для Play Console
```

## Структура

```
app/src/main/java/ru/homebuhg/
├── core/
│   ├── data/         Room, репозитории, Firestore
│   ├── domain/       модели, use cases
│   ├── designsystem/ тема Material 3, общие компоненты
│   ├── common/       форматтеры, утилиты
│   ├── di/           Hilt-модули
│   └── sync/         двунаправленная синхронизация
├── feature/
│   ├── auth/         Google Sign-In, household
│   ├── home/         дашборд
│   ├── operations/   список и форма операций
│   ├── accounts/     карты, наличные, переводы
│   ├── categories/   категории и магазины
│   ├── budgets/      бюджеты и лимиты
│   ├── reports/      графики
│   ├── scanner/      QR ФНС
│   ├── sms/          парсинг SMS
│   └── settings/     настройки
├── navigation/       AppNavHost, Destinations
├── HomeBuhgApp.kt    Hilt Application
└── MainActivity.kt   единственный Activity
```

## Дорожная карта

См. `C:\Users\lb426\.claude\plans\composed-knitting-trinket.md` для детального плана этапов.

| Этап | Статус |
|---|---|
| 0. Setup проекта | ✅ |
| 1. Core data (Room) | ✅ |
| 2. MVP UI | ✅ |
| 3. Бюджеты + регулярки | 🔜 |
| 4. Отчёты | ⬜ |
| 5. Firebase sync | ⬜ |
| 6. Сканер QR ФНС | ⬜ |
| 7. Парсинг SMS | ⬜ |
| 8. Экспорт CSV/XLSX | ⬜ |
| 9. Polish + релиз | ⬜ |
