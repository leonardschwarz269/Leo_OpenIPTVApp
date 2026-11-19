# Verification memory

| Trigger | Check | Incident |
|---|---|---|
| Коммит после правок через patch/write_file | `git status --porcelain` пуст (кроме ignored) **и** `git show HEAD:<file>` содержит ожидаемое изменение — успех коммита не доказывает, что правки в нём | 2026-11-19: коммит `732793d` ушёл в origin без wrapper 8.11.1 и очистки AndroidManifest — правки были на диске, но не в индексе (` M` / `RM` во втором столбце = unstaged, прочитано как staged) |
| Push ветки | `git ls-remote origin refs/heads/<branch>` SHA == локальный HEAD | — |
