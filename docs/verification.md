# Verification memory

| Trigger | Check | Incident |
|---|---|---|
| Коммит после правок через patch/write_file | `git status --porcelain` пуст (кроме ignored) **и** `git show HEAD:<file>` содержит ожидаемое изменение — успех коммита не доказывает, что правки в нём | 2026-11-19: коммит `732793d` ушёл в origin без wrapper 8.11.1 и очистки AndroidManifest — правки были на диске, но не в индексе (` M` / `RM` во втором столбце = unstaged, прочитано как staged) |
| Push ветки | `git ls-remote origin refs/heads/<branch>` SHA == локальный HEAD | — |
| Правки EPG-схемы/загрузки программ (`EpgProgram`, `EpgDao`, `EpgDatabase`, `processLoadedEpg`) | Установка APK на эмулятор, реальные EPG-источники «On launch», мониторинг `dumpsys meminfo` 4+ мин: **native и Dalvik heap на плато**, процесс жив, размер БД не растёт после дедупа, в списке каналов видны названия передач | 2026-11-19: без индексов native heap 50MB→2.1GB за 30–60с → OOM-убийство (воспроизведено на debug и release 6.0). UNIQUE(channelId,startTime)+индексы startTime/endTime починили native, но окно загрузки ±14 дней (~800K программ ×3 мапы) дало Java OOM (Dalvik 585MB) через ~8 мин. Окно −1/+2 дня → Dalvik 57–98MB, PSS 232–278MB, плато, EPG отображается |
