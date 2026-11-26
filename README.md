# האפליקציה שלי — זכור אותי (ProjectMM)

**Автор:** [Ваше Имя и Фамилия]

## Описание экранов

- **SplashActivity (`activity_splash.xml`)** — экран заставки с анимацией появления логотипа (`scale` + `alpha`), который через 3 секунды в отдельном потоке (`Thread`) автоматически переходит на экран входа.
- **LoginActivity (`activity_login.xml`)** — экран входа в аккаунт с проверкой корректности email и пароля, получением данных после регистрации через `Intent` и функцией «זכור אותי» («Запомни меня») на базе `SharedPreferences` и `Snackbar`.
- **RegisterActivity (`activity_register.xml`)** — экран регистрации нового пользователя с проверкой email, длины пароля (минимум 6 символов) и совпадения паролей, передающий данные на экран входа через `Intent extras`.

## Скриншоты

- Экран заставки (Splash):
  ![Splash Screen](screenshots/splash.png)
- Экран входа (Login):
  ![Login Screen](screenshots/login.png)
- Экран регистрации (Register):
  ![Register Screen](screenshots/register.png)
