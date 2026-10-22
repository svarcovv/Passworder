# Passworder

Локальный менеджер паролей для Windows 10/11 x64.

[Скачать для Windows](https://github.com/svarcovv/Passworder/releases/latest/download/Passworder-Setup.exe)

Установка и обновление используют одну папку: `%LOCALAPPDATA%\Programs\Passworder`.
Ярлыки на рабочем столе и в меню «Пуск» ведут на установленную сборку.
При запуске другой сборки предыдущий экземпляр завершает работу.
Если открыто редактирование записи, сначала нужно сохранить или закрыть его.

- Записи с логином, паролем и ссылкой, поиск и избранное.
- Генератор паролей, светлая и темная темы.
- Мастер-пароль, автоблокировка и зашифрованные резервные копии.
- Копирование с очисткой буфера через 20 секунд.
- Трей и настраиваемая глобальная горячая клавиша.

## Данные

Хранилище: `%LOCALAPPDATA%\Passworder\vault.pwr`.
Резервная копия перед последним сохранением: `vault.pwr.bak`.
Копии сохраняются и восстанавливаются в настройках.
Мастер-пароль восстановить нельзя.

Крестик скрывает окно в трей и блокирует хранилище. Полный выход - через меню
значка в трее. Удаление приложения сохраняет хранилище и резервную копию.

## Клавиши

| Сочетание | Действие |
| --- | --- |
| Ctrl+Alt+P | Показать окно |
| Ctrl+N | Добавить запись |
| Ctrl+F | Поиск |
| Ctrl+Shift+L | Заблокировать |
| Ctrl+C в таблице | Копировать пароль |
| Ctrl+Shift+C в таблице | Копировать логин |
| Enter / Delete | Редактировать / удалить |

## Разработка

Java 21, Maven, Swing и FlatLaf. В IntelliJ проект открывается через `pom.xml`.
Главный класс: `svarcovv.dev.passworder.Passworder`.

```powershell
.\mvnw.cmd verify
java -jar target\Passworder.jar
```

Windows-сборка с Eclipse Temurin JDK 21 в `JAVA_HOME`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1 -Destination dist\0.2.3
powershell -ExecutionPolicy Bypass -File scripts\package-installer.ps1 -AppImage dist\0.2.3\Passworder
```

`run.cmd` собирает и запускает текущие исходники.
`DesktopSmoke` проверяет интерфейс на временном хранилище с вымышленными данными.

[Устройство хранилища](SECURITY.md) · [Лицензии зависимостей](THIRD_PARTY.md) · [Runtime](RUNTIME.md)
