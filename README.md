# OpenIPTV

> [!CAUTION]
> This application supports a full bypass of Android Auto restrictions and may provide access to features that are not officially supported by the Android Auto platform. Use of this application is entirely at your own risk. The developer assumes no responsibility for any consequences resulting from the use of this application, including but not limited to device damage, software malfunctions, accidents, traffic law violations, property damage, or personal injury. Users are solely responsible for how they use this application and must comply with all applicable laws and road safety regulations.

## 📺 OpenIPTV

IPTV-плеер для Android, Android TV и экспериментального Android Auto.
Текущая версия: **6.1.2**.

Проект основан на [OpenIPTV](https://github.com/redmi9cnfc/OpenIPTVApp); сведения об исходных компонентах и лицензиях — в [NOTICE.md](NOTICE.md).

## 🚀 О проекте

OpenIPTV — лёгкое приложение для просмотра IPTV-каналов по M3U-плейлистам. В версии 6.1.2 исправлен вход в YouTube и RuTube (доступ с телефона с общими cookies, системная клавиатура на телефоне, полные цифры и спецсимволы с привязкой к полю в Android Auto, поддержка OAuth-окон). В 6.1.0–6.1.1 исправлены открытие Prisma, размер верхних кнопок и категорий, поведение Back; добавлены «Продолжить просмотр», очистка кэша с подтверждением и автомобильные пресеты интерфейса.

## ✨ Возможности

- M3U-плейлисты: добавление по URL и импорт из файла
- Воспроизведение IPTV-потоков (HLS / MPEG-TS)
- EPG: текущая и следующая передача, расписание, напоминания
- Выбор декодера: Auto / Hardware (HW) / Software (SW)
- Избранные каналы, поиск, недавние и «Продолжить просмотр»
- Безопасный двойной Back и очистка кэша с подтверждением
- Поддержка Android TV и пульта
- Экспериментальная поддержка Android Auto (см. предупреждение выше)

## 🧱 Технологии

- Kotlin
- ExoPlayer / Media3
- Android Jetpack
- MVVM

## 📦 Установка

- Требуется Android 7.0 или выше.
- Установите подписанный APK; для установки вне магазина разрешите установку из выбранного источника.
- Если установлена оригинальная версия с другой подписью, сначала удалите её.
- Готовый подписанный APK доступен в [Releases](https://github.com/leonardschwarz269/Leo_OpenIPTVApp/releases) (текущий: [v6.1.2](https://github.com/leonardschwarz269/Leo_OpenIPTVApp/releases/tag/v6.1.2)).

## 🛠 Сборка

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Для release-сборки требуется локальный keystore; параметры подписи не хранятся в репозитории.

## 🗺 План развития

См. [docs/ROADMAP.md](docs/ROADMAP.md). Безопасные автомобильные срезы реализованы в 6.1.0; платформенные ограничения и следующие этапы зафиксированы отдельно.

## 🐞 Сообщения об ошибках

Используйте [GitHub Issues](https://github.com/leonardschwarz269/Leo_OpenIPTVApp/issues). Укажите устройство, версию Android, шаги воспроизведения и ожидаемое поведение.