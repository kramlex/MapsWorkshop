# Yandex Maps Workshop

## 🎯 Обзор проекта

Этот воркшоп — пошаговое создание мобильного приложения с картами Yandex Maps на **Kotlin Multiplatform** и **Compose Multiplatform**. Базовое приложение демонстрирует основные возможности работы с картами: отображение, добавление меток и работу с камерой.

Продолжение воркшопа посвящено встраиванию **AI-ассистента**, который общается с пользователем в чате и взаимодействует с картой: ищет места, ставит метки и управляет камерой через вызов инструментов (function calling) у LLM.

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

- **Готовые ключи для воркшопа**: [Скачать с GitHub Gist](https://gist.github.com/kramlex/483f0bf271e8af439e429c2582b19924)
- **Yandex MapKit API Key** - для карт
- **OpenAI-совместимый API Key** - для AI-ассистента (по умолчанию DeepSeek)

## 🏗 Структура проекта

```
MapsWorkshopInternal/
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
cd MapsWorkshopInternal
```

### 2. Настройка API ключей
**Получить ключи**: [🔑 GitHub Gist](https://gist.github.com/kramlex/483f0bf271e8af439e429c2582b19924)

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

## 🤖 План воркшопа (AI-ассистент)

1. Сделать настоящий вызов LLM и убрать заглушку
2. Добавить системный промпт
3. Добавить контекст
4. Собрать агентский цикл с инструментом поиска
5. Добавить инструменты для взаимодействия с картой
6. Добавить инструменты и `AssistantApi` для взаимодействия с чатом


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
- [Yandex MapKit](https://developer.tech.yandex.ru/maps/mapkit/)
- [DeepSeek API](https://api-docs.deepseek.com/)

---

**⚠️ Важно**:
- Не публикуйте ключи в публичных репозиториях
- После воркшопа ключи будут автоматически деактивированы
