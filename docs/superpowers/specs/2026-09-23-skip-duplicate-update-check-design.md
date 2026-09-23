# Дизайн: исключение повторной проверки обновления после установки

## Проблема

После успешной авторизации приложение проходит следующий цикл:

1. Авторизация.
2. Экран обновления: проверка → загрузка файлов → установка → SW-хенд-офф.
3. **Повторная** проверка обновления с надписью «Успешная установка».
4. Только затем открывается само приложение.

Наблюдалось даже появление экрана «Успешная установка» поверх уже отрисованной
главной навигации (сканер + главное меню).

## Причины

Повторный цикл вызван отсутствием короткого замыкания на «уже установлено»
в трёх местах:

- `ShellAppDefinition.startRoute` (webShell) — при `installed && restored` **всегда**
  возвращает `UpdateScreenRoute`, игнорируя `isServiceWorkerControlled()`.
  Значение SW-контроля вычисляется только для диагностического вывода.
- `AuthorizationScreenViewModel.openHomeScreen` (features/authorization) — после
  успешной авторизации **всегда** `navigator.navigate(UpdateScreenRoute)`.
- `UpdateScreenViewModel.startUpdate` (features/update) — при создании экрана **всегда**
  запускает полный цикл (кэш → регистрация SW → sync → «Успешная установка» → reload),
  даже если сборка уже установлена и SW активен.

Сопутствующий баг: `ServiceWorker.kt` регистрирует скрипт по относительному пути
`"update-sw.js"`, из-за чего на GitHub Pages `register()` уходит в корень домена и
заканчивается 404. Без контроля SW после перезагрузки оболочка снова загружается
из сети и повторяет цикл установки.

## Целевое поведение

При уже установленной сборке (`installed = true`), восстановленной сессии
(`restored = true`) и активном Service Worker (`swControlled = true`) приложение
открывается напрямую, минуя экран обновления. Экран «Успешная установка» при этом
не показывается.

## Решение

### 1. Центральный предикат

`shouldSkipUpdate(): Boolean = isAppInstalled() && isServiceWorkerControlled()`

Функции `AppInstallState` (expect/actual) вместе с предикатом перенесены из
`core/update/impl` в `core/update/api`, чтобы ими могли пользоваться `webShell`,
`webApp` и `features/authorization` без лишних зависимостей.

### 2. Shell (webShell)

`ShellAppDefinition.startRoute`:
- если `installed && restored && controlled` — выполнить **защищённый хенд-офф**:
  один `window.location.reload()` (SW отдаст приложение из кэша), ограниченный
  счётчиком попыток `incrementInstallReloadAttempts()` с фолбэком на
  `UpdateScreenRoute` (переустановку) после превышения лимита.

### 3. Экран авторизации

`AuthorizationScreenViewModel.openHomeScreen`:
- `shouldSkipUpdate()` → `navigate(TasksPageRoute)`, иначе `navigate(UpdateScreenRoute)`.

### 4. Экран обновления (защита изнутри)

`UpdateScreenViewModel.startUpdate`:
- если `shouldSkipUpdate()` → сразу `finishInstall()` (передача управления),
  минуя загрузку, регистрацию SW, sync и сообщение «Успешная установка».

### 5. Исправление пути Service Worker

`ServiceWorker.kt`: `SW_PATH` вычисляется как абсолютный URL относительно
`document.baseURI`, чтобы регистрация не уходила в корень домена.

## Обработка ошибок

- Хенд-офф SW ограничен `MAX_HANDOFF_ATTEMPTS = 3`; счётчик сбрасывается
  при успешном переходе или превышении лимита.
- При `installed && !swControlled` оболочка переходит на `UpdateScreenRoute`
  для переустановки — прежнее поведение сохранено.
- `FullAppDefinition` уже сбрасывает счётчик reload-попыток и открывает
  `TasksPageRoute` при `installed && restored` — без изменений.

## Тестирование

- `./gradlew detekt`
- `./gradlew allTests`
- Ручная проверка JS-сборки:
  - чистая установка: авторизация → загрузка → установка → reload → приложение
    без повторной «Успешная установка»;
  - повторный вход: сразу главный экран;
  - сброс SW: оболочка показывает экран обновления для переустановки.