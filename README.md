# Yandex Maps Workshop

## 🎯 Обзор проекта

Этот воркшоп — пошаговое создание мобильного приложения с картами Yandex Maps на **Kotlin Multiplatform** и **Compose Multiplatform**. Базовое приложение демонстрирует основные возможности работы с картами: отображение, добавление меток и работу с камерой.

Продолжение воркшопа посвящено встраиванию **AI-ассистента**, который общается с пользователем в чате и взаимодействует с картой: ищет места, ставит метки и управляет камерой через вызов инструментов (function calling) у LLM.

## ⚡ Быстрый старт

Скрипт установит инструменты, создаст `local.properties` c ключами и прогреет Gradle. Выберите скрипт под вашу платформу:

```bash
# Android-разработка (macOS / Linux / Windows)
./scripts/setup-android.sh

# iOS-разработка (только macOS) — включает все шаги Android + Xcode/Tuist
# Xcode можно установить только из Mac App Store (вручную)
./scripts/setup-ios.sh
```

- **macOS / Linux** — обычный терминал.
- **Windows** — запустите в **Git Bash** (входит в Git for Windows): `bash scripts/setup-android.sh`.

## 🤖 План воркшопа (AI-ассистент)

1. Сделать настоящий вызов LLM и убрать заглушку
2. Добавить системный промпт ([Пример](https://gist.github.com/tamimattafi/79505591612ee8220f7c9348463dcab0#file-gistfile1-txt))
3. Добавить контекст
4. Собрать агентский цикл с инструментом поиска
5. Добавить инструменты для взаимодействия с картой
6. Добавить инструмент для взаимодействия с чатом

## 🧩 Вспомогательные классы

Ключевые классы, вокруг которых строится работа на воркшопе:

- **[`OpenAIClient`](common/additional/src/commonMain/kotlin/ru/yandex/maps/workshop/common/additional/llm/OpenAIClient.kt)** — минимальный клиент к OpenAI-совместимому эндпоинту chat-completions. Метод `complete { }` собирает запрос через DSL (сообщения, инструменты) и возвращает `CompletionResponse`; при не-2xx бросает `OpenAIException` с сырым телом ответа.
- **[`ChatRepository`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/chat/ChatRepository.kt)** — хранит историю чата как `Flow<List<ChatMessage>>`, отправляет сообщения и очищает диалог. На старте отвечает заглушкой (`MOCK`) — по плану воркшопа здесь подключаются `OpenAIClient` и `AssistantApi`.
- **[`AssistantApi`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/agent/AssistantApi.kt)** — высокоуровневые действия агента над картой и чатом: `searchPlaces`, `showOnMap`, `focusOnPoint` / `focusOnPoints`, `closeChat`. Именно на этот фасад опираются инструменты агента.
- **[`AgentTools`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/agent/tools/AgentTools.kt)** — интерфейс `AgentTool` и реестр `AgentToolset`: отдаёт спецификации инструментов (`specs`) модели и направляет её вызовы (`dispatch`) в нужный инструмент. Здесь же хелперы для описания инструментов (`toolSpecs`) и разбора аргументов. Новые инструменты подключаются в список `AgentToolset` (на старте зарегистрирован только `SearchPlacesTool`).
    - **[`SearchPlacesTool`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/agent/tools/SearchPlacesTool.kt)** — готовый пример: инструмент `search_places` принимает `query` (опционально `latitude` / `longitude` / `radius_meters`), вызывает `AssistantApi.searchPlaces` и возвращает кандидатов с координатами и адресами в JSON.
    - **[`ShowPlacesOnMapTool`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/agent/tools/ShowPlacesOnMapTool.kt)** — `show_places_on_map`: показывает места на карте через `AssistantApi.showOnMap`. Реализуется на воркшопе (`TODO`).
    - **[`FocusOnPointTool`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/agent/tools/FocusOnPointTool.kt)** — `focus_on_point`: центрирует камеру на точке через `AssistantApi.focusOnPoint`. Реализуется на воркшопе (`TODO`).
    - **[`FocusOnPointsTool`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/agent/tools/FocusOnPointsTool.kt)** — `focus_on_points`: вписывает несколько точек в видимую область через `AssistantApi.focusOnPoints`. Реализуется на воркшопе (`TODO`).
    - **[`CloseChatTool`](common/src/commonMain/kotlin/ru/yandex/maps/workshop/common/agent/tools/CloseChatTool.kt)** — закрывает панель чата через `AssistantApi.closeChat`. Реализуется на воркшопе (`TODO`, включая имя и описание инструмента).

## 🔗 Решения по шагам

Готовые решения каждого шага плана — отдельным коммитом (ветка `assistant-sample`):

1. [Сделать настоящий вызов LLM и убрать заглушку](https://github.com/kramlex/MapsWorkshop/commit/6806a0951709ef9d53f06c006526077985aa15c2) — `6806a09`
2. [Добавить системный промпт](https://github.com/kramlex/MapsWorkshop/commit/d34e782f26a54177761d755d5777db5549cb4517) — `d34e782`
3. [Добавить контекст](https://github.com/kramlex/MapsWorkshop/commit/bbd6c344a5bf18b0bae054f2052e29022bc1e8ad) — `bbd6c34`
4. [Собрать агентский цикл с инструментом поиска](https://github.com/kramlex/MapsWorkshop/commit/b236e16aa0d5b950d6b889bd2b75110de935a82c) — `b236e16`
5. [Добавить инструменты для взаимодействия с картой](https://github.com/kramlex/MapsWorkshop/commit/002774a225ba79cba8355e904e091d1e9b71de6b) — `002774a`
6. [Добавить инструмент для взаимодействия с чатом](https://github.com/kramlex/MapsWorkshop/commit/091362cd83bed6c780158870bad3dc98f60eabe4) — `091362c`

> 💡 **Не успеваете на воркшопе?** Переключитесь на коммит нужного шага, чтобы продолжить с готового решения, или перенесите конкретный шаг в свою ветку через `cherry-pick`.

```bash
# Посмотреть готовое решение шага (detached HEAD), напр. шаг 1
git checkout 6806a09

# Вернуться к своей ветке
git checkout assistant-sample

# Или перенести коммит конкретного шага в свою ветку поверх текущей работы
git cherry-pick 6806a09
```

## 🛠 Технологический стек

### Основные технологии
- **Kotlin Multiplatform** - кроссплатформенная разработка
- **Compose Multiplatform** - современный UI фреймворк
- **Yandex MapKit** - картографический SDK
- **Ktor Client** - HTTP-клиент для запросов к LLM
- **OpenAI-совместимый API** - LLM для AI-ассистента (по умолчанию DeepSeek)

### Целевые платформы
- Android (API 26+)
- iOS (iOS 15+)

## 💻 Необходимое программное обеспечение

### 1. Java Development Kit (JDK)
- **Версия**: 17 или новее (LTS версии)
- **Источник**:
    - [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
    - [OpenJDK](https://adoptium.net/)
    - [Amazon Corretto](https://aws.amazon.com/corretto/)

**Установка на macOS**:
```bash
# Через Homebrew
brew install openjdk@17

# Добавить в PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Установка на Windows**:
- Скачать установщик с официального сайта
- Запустить установщик от имени администратора
- Добавить JAVA_HOME в переменные среды

**Установка на Linux**:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Android Studio
- **Версия**: Hedgehog (2023.1.1) или новее
- **Источник**: [developer.android.com](https://developer.android.com/studio)

**Дополнительные компоненты**:
- Android SDK Platform 36
- Android SDK Build-Tools
- Android Emulator
- Android SDK Platform-Tools

**Настройка переменных среды**:
```bash
# macOS/Linux
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Windows
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\emulator
set PATH=%PATH%;%ANDROID_HOME%\platform-tools
```

### 3. Xcode (только для macOS)
- **Версия**: 15.0 или новее
- **Источник**: Mac App Store
- **Дополнительно**: Command Line Tools

**Установка Command Line Tools**:
```bash
xcode-select --install
```

### 4. Tuist
- **Версия**: 4.0 или новее
- **Источник**: [tuist.dev](https://tuist.dev/)
- **Назначение**: генерация Xcode-проекта и загрузка iOS-зависимостей (Yandex MapKit через Swift Package Manager). Заменяет CocoaPods.

**Установка**:
```bash
# Через Homebrew
brew install tuist

# Альтернатива — через mise
brew install mise
mise install tuist
```

### 5. Git
- **Версия**: 2.30.0 или новее
- **Источник**: [git-scm.com](https://git-scm.com/)

**Установка на macOS**:
```bash
brew install git
```

**Установка на Windows**:
- Скачать установщик с официального сайта
- Рекомендуется использовать Git Bash

**Установка на Linux**:
```bash
sudo apt install git
```

## 🔑 API ключи

- **Готовые ключи для воркшопа**: [Скачать с GitHub Gist](https://gist.github.com/tamimattafi/357d1f878d91b0a224bedf82c211fc5c)
- **Yandex MapKit API Key** - для карт
- **OpenAI-совместимый API Key** - для AI-ассистента (по умолчанию DeepSeek)

## 🏗 Структура проекта

```
MapsWorkshop/
├── common/               # Общая логика: карта, чат, AI-агент
│   └── additional/       # Базовые утилиты и LLM-клиент (Ktor)
├── composeApp/           # Основное приложение (Android/iOS)
├── mapkit-bindings/      # Kotlin обертки для Yandex MapKit
├── mapkit-interop/       # Нативные интерфейсы
└── iosApp/               # iOS приложение
```

### Детальное описание модулей

#### `common/`
Общая бизнес-логика приложения:
- `CommonApp.kt` - DI-контейнер приложения
- `DescriptionGenerator.kt` - генератор описаний для меток
- `SearchManager.kt` - менеджер поиска
- `chat/` - состояние и логика чата (`ChatViewModel`, `ChatRepository`, `ChatMessage`)
- `agent/` - AI-агент: `AssistantApi`, контроллер камеры и инструменты (`tools/`)

#### `common/additional/`
Базовые утилиты и инфраструктура:
- `llm/OpenAIClient.kt` - клиент к OpenAI-совместимому API и DSL запроса
- `udf/` - примитивы UDF-архитектуры (`ViewModel`, `Store`, `Event`)

#### `composeApp/`
Основное приложение с UI:
- `App.kt` - главный компонент приложения
- `MapActions.kt` - действия с картой
- `internal/view/` - UI чата (`ChatScreen`, `ChatSummaryBar`)

## 🚀 Настройка проекта

### 1. Клонирование репозитория
```bash
git clone <repository-url>
cd MapsWorkshop
```

### 2. Настройка API ключей
**Получить ключи**: [🔑 GitHub Gist](https://gist.github.com/tamimattafi/357d1f878d91b0a224bedf82c211fc5c)

Создать файл `local.properties` в корне проекта (он уже в `.gitignore`, поэтому ключи не попадут в репозиторий):

```properties
#Mapkit
mapkitToken=ваш_mapkit_token

#OpenAI Client
openAiApiKey=ваш_api_key
openAiModel=deepseek-v4-flash
openAiBaseUrl=https://api.deepseek.com/chat/completions
```

`openAiModel` и `openAiBaseUrl` необязательны — по умолчанию используются `deepseek-v4-flash` и `https://api.deepseek.com/chat/completions`.

### 3. Синхронизация проекта
```bash
# Синхронизация Gradle
./gradlew

# Генерация Xcode-проекта (Tuist)
./iosApp/generate.sh
```

Скрипт `iosApp/generate.sh` запускает `tuist install` (загрузка SPM-зависимостей, включая Yandex MapKit) и `tuist generate` (создание `MapsWorkshop.xcworkspace`). После выполнения откройте `iosApp/MapsWorkshop.xcworkspace` в Xcode.

> ⚠️ Файлы, сгенерированные Tuist (`*.xcodeproj`, `*.xcworkspace`, `Derived/`), не коммитятся — перегенерируйте их через `generate.sh` после клонирования или смены ветки.

## 📱 Исходное состояние проекта

**Что уже готово в проекте**:
- Полностью настроенный Kotlin Multiplatform проект с поддержкой Android и iOS
- Интегрированный Yandex MapKit SDK с готовыми Kotlin обертками
- Настроенная Gradle конфигурация для всех платформ
- Готовая архитектура приложения на UDF (Store / Reducer / Event)
- Полнофункциональные интерфейсы для работы с картами и метками
- Подготовленная инфраструктура AI-ассистента: UI чата, LLM-клиент (`OpenAIClient`), `AssistantApi` с инструментами и контроллером камеры

На старте чат отвечает заглушкой — задача воркшопа подключить настоящую LLM и собрать агентский цикл.

## 📊 Системные требования

### Операционная система
- **macOS**: 12.0 (Monterey) или новее (обязательно для iOS разработки)
- **Windows**: 10 (версия 1903) или новее
- **Linux**: Ubuntu 20.04 LTS или новее

### Специальные требования для iOS
- **macOS**: Обязательно для iOS разработки
- **Xcode**: Последняя стабильная версия
- **Tuist**: Установленный для генерации Xcode-проекта

## 📚 Дополнительные ресурсы

### Документация
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Yandex MapKit](https://yandex.ru/maps-api/products/mapkit)
- [DeepSeek API](https://api-docs.deepseek.com/)

### Gists
- [API ключи для воркшопа](https://gist.github.com/tamimattafi/357d1f878d91b0a224bedf82c211fc5c) — `mapkitToken`, `openAiApiKey` и др.
- [Пример системного промпта](https://gist.github.com/tamimattafi/79505591612ee8220f7c9348463dcab0#file-gistfile1-txt) — для AI-ассистента

---

**⚠️ Важно**:
- Не публикуйте ключи в публичных репозиториях
- После воркшопа ключи будут автоматически деактивированы
