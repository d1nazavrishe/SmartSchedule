# SmartSchedule (ProjectMM)

**Автор:** [Ваше Имя и Фамилия]

## Описание проекта

Android-приложение «Умное расписание» (SmartSchedule) на языке **Java**, помогающее школьнику или студенту планировать неделю, вести список задач (To-Do List), контролировать загруженность дня и оптимизировать расписание с помощью искусственного интеллекта (**Google Gemini API**).

## Экраны и архитектура

- **SplashActivity (`activity_splash.xml`)** — экран заставки с анимацией появления логотипа (`scale` + `alpha`), который через 3 секунды в отдельном потоке (`Thread`) переходит на экран входа.
- **LoginActivity (`activity_login.xml`)** — экран входа с проверкой email и пароля, получением данных после регистрации через `Intent` и функцией «Запомни меня» на базе `SharedPreferences` и `Snackbar`. После входа открывает `MainActivity`.
- **RegisterActivity (`activity_register.xml`)** — экран регистрации нового пользователя с валидацией email и пароля.
- **MainActivity (`activity_main.xml`)** — главный экран с нижней панелью навигации (`BottomNavigationView`), объединяющий 3 основных раздела:
  1. **ScheduleFragment (`fragment_schedule.xml`)** — выбор дня недели (с воскресенья `Sun–Sat`), индикатор общей загруженности дня (`Daily Workload %`), почасовая лента событий (`07:00–23:00`) с отображением времени на подготовку/дорогу, добавление задач из `To-Do List` или вручную, перенос и удаление событий.
  2. **TodoFragment (`fragment_todo.xml`)** — список дел (`To-Do List`) с кнопкой `+` и параметрами задачи: название, длительность, время на подготовку, категория (`Study`, `Sport`, `Entertainment`, `Hobby`, `Chores`), приоритет, желаемое время суток, дедлайн и периодичность (`1 раз`, `N раз в неделю`, `каждую неделю в фиксированное время`).
  3. **AiChatFragment (`fragment_ai_chat.xml`) + `GeminiHelper`** — чат с ИИ-помощником на базе бесплатного **Gemini API** (`HttpURLConnection` + `org.json` в фоновом потоке). ИИ видит текущее расписание, загруженность дней и `To-Do List`, учитывает совместимость соседних событий (например, отдых после спорта перед учёбой) и предлагает готовые изменения с кнопками подтверждения **Apply Changes / Dismiss**.
- **DatabaseHelper (`SQLiteOpenHelper`)** — локальная база данных SQLite с таблицами `todo_items` и `schedule_events`.

## Скриншоты

- Экран заставки (Splash):
  ![Splash Screen](screenshots/splash.png)
- Экран входа (Login):
  ![Login Screen](screenshots/login.png)
- Экран регистрации (Register):
  ![Register Screen](screenshots/register.png)
