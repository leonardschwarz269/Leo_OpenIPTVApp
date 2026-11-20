# NOTICE — OpenIPTV

OpenIPTV — форк [Fermata Media Player](https://github.com/AndreyPavlenko/Fermata)
(автор Andrey Pavlenko), адаптированный под IPTV. Исходный форк:
https://github.com/redmi9cnfc/OpenIPTVApp

## Лицензии

- Исходный код проекта: **GNU GPL v3** (см. `LICENSE`), унаследовано от Fermata.
- `app/src/main/lib/aauto.aar` — legacy Android Auto SDK, взят из дерева Fermata
  (пакет `com.google.android.libraries.car.*`). Provenance: не проверен,
  license/notice внутри AAR отсутствуют (106 entries). Перед публичным
  распространением проверить условия Android Auto SDK или заменить на
  официальный `androidx.car.app` (уже подключён параллельно).
- `app/src/main/lib/xposed-api-82.jar` — Xposed API, только compileOnly,
  в APK не включается.
- Остальные зависимости — стандартные Maven (AndroidX, Media3, Coil, OkHttp,
  Room, Kotlin Serialization), Apache-2.0 / MIT, см. `app/build.gradle.kts`.

## Подпись

- Оригинальный OpenIPTV v5.2 (versionCode 6) подписан **debug-сертификатом**
  автора; ключ недоступен → in-place update невозможен.
- Начиная с v6.0.1 (versionCode 17) релиз подписывается новым ключом
  `openiptv-release.keystore` (хранится вне репо, у maintainer'а).
  Установка поверх оригинала = одна чистая переустановка; дальше обновления штатные.
- Debug-сборки имеют `applicationIdSuffix=".debug"` и ставятся рядом с оригиналом.

## Известные ограничения (зафиксированы, не исправлены в baseline)

- Wi-Fi Transfer (NSD + token/password) — без TLS/pairing; оставлен как есть
  по решению владельца, риск задокументирован в docs/.
- Android Auto — экспериментальный режим; production-фокус: phone + Android TV.
