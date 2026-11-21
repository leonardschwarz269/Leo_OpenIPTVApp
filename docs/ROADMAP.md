# Roadmap

## Текущее состояние: 6.0.1 + локальные исправления

- Подписанная release-сборка 6.0.1, CI.
- Исправлены: версия в настройках, выбор декодера, напоминания Android 13+, добавление плейлиста с главного экрана.
- Устранено переполнение диска при загрузке EPG.
- Проверено на эмуляторе Android 15; проверка на физическом устройстве заказчика ещё предстоит.

### Customer fixes (локальный `main`, ещё не опубликовано)
- `79e3f86` Prisma: белый экран исправлен. Причина — сертификат `prisma.ws` выдан для `*.a.trbcdn.net`; WebView молча отменял TLS. Prisma использует scoped HTTP-entry, глобального TLS-bypass нет. BrowserScreen получил loading/error/retry только для main-frame.
- `79e3f86` Верхняя панель: кнопки/иконки увеличены (phone 48/24dp), фокус и offline-тесты.
- `f7582da` Служебные каталоги агента добавлены в `.gitignore`.

## Roadmap-ветка `feat/automotive-roadmap` (не показывать заказчику до приёмки)

База: исправленный `main` (`f7582da`). Источник идей — [Headswap](https://t.me/headswap); код/визуальный стиль не копировались. Реализованные рабочие срезы:

| Срез | Коммит | Статус | Проверка |
|---|---|---|---|
| Безопасный Back / двойной Back | `51a381f` | реализовано | BackPoliciesTest + runtime smoke в `docs/verification.md` |
| Живые плитки: Продолжить просмотр | `f683a62` | реализовано | ContinueWatchingTest + RowTest + runtime-строка в `docs/verification.md` |
| Очистка кэша с подтверждением | `0718f31` | реализовано | CacheMaintenanceTest, runtime Settings → Backup & Network → Clear cache |
| Авто-пресеты / контраст стекла | `d74fe6b` | реализовано | ScreenPresetsTest/ContrastTest, car 64/32/24/24 policy, WCAG scrim |

## Заблокировано / требует решения или железа

Подробно: `docs/automotive-platform-decisions.md`.

1. **Проекция экрана телефона / запуск произвольных приложений на магнитоле.**
   Нет официального механизма Android Auto для произвольного зеркалирования/запуска. Legacy projection + Xposed + accessibility injection не являются безопасной shippable-фичей.
   Решение: отдельный experimental flavor (не Play release), либо официальный аудио-путь / AAOS parked / EAP.

2. **Навигационные плитки / интеграция.**
   Видео на surface NavigationTemplate — категорийное злоупотребление. Настоящая навигация требует NavigationManager/TBT/map.
   Решение: либо реальная навигация, либо убрать NAVIGATION-категорию из release.

3. **Wi-Fi ADB как обязательная настройка.**
   Небезопасно как потребительский сценарий. Только опциональный dev-инструмент после отдельного security-review.

4. **Физические автомобильные экраны.**
   DHU/эмуляторы проверяют пресеты, но OEM-поведение требует физической магнитолы.

## Следующие безопасные шаги

1. DHU-прогон официального аудио-пути на 800×480 и 1920×1080.
2. Разделение release/experimental flavor.
3. AAOS parked-video как отдельный трек; заявка в EAP видео Android Auto.
4. Security-review Wi-Fi-передачи и network-security-config.
5. Если experimental mirror остаётся: исправить MediaProjection consent/FGS/order defects.

## Прочее будущее

- Ограничение размера / окна хранения EPG для устройств с небольшим накопителем.
- Фоновое обновление EPG по расписанию.
- Экспорт / резервное копирование плейлистов и настроек.
- Отдельный security-review Wi-Fi-передачи плейлистов.