# Survey Tool - Developer Guide

Documentation for developers working on the Survey Tool project.

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Build](#build)
- [Test](#test)
- [Run](#run)
- [Release](#release)
- [CI/CD](#cicd)
- [Development Workflow](#development-workflow)
- [Code Organization](#code-organization)
- [Key Components](#key-components)
- [Configuration Format](#configuration-format)
- [Schema files](#schema-files)
- [Contributing](#contributing)
- [Additional Resources](#additional-resources)
- [License](#license)

---

## Project Overview

Survey Tool is a Kotlin Multiplatform desktop application. It provides a declarative, YAML-based approach to creating and running surveys and quizzes.

### Tech Stack

- **Language**: Kotlin 2.3
- **JVM Target**: Java 25
- **Build System**: Gradle 9.0+ (Kotlin DSL)
- **UI Framework**: Compose Multiplatform 1.10.0
- **Configuration**: YAML (SnakeYAML Engine 3.0)
- **Data Export**: CSV (Kotlin CSV 1.10.0)
- **Testing**: Kotlin Test, MockK 1.14

### Supported Platforms

- **Current**: Desktop (JVM) - Windows, Linux
- **Planned**: Android (currently disabled in build configuration)

---

## Architecture

### Application Flow

```
┌─────────────────┐
│  Survey Load    │  Initial state - load survey configuration
│     Screen      │
└────────┬────────┘
         │ Load YAML
         ▼
┌─────────────────┐
│  Survey Summary │  Display survey metadata, leaderboard
│     Screen      │
└────────┬────────┘
         │ Start Survey
         ▼
┌─────────────────┐
│  Survey Content │  Navigate through pages, answer questions
│     Screen      │
└────────┬────────┘
         │ Submit
         ▼
┌─────────────────┐
│  Results/Score  │  Show completion, score (quiz), leaderboard
└─────────────────┘
```

### State Management

The application uses Compose state management with the following key state holders:

- **SurveyLoadModel**: Manages survey loading state (Idle, Loading, Loaded, Error)
- **SurveyState**: Tracks current page, answers, progress, and navigation
- **SurveyDataManager**: Handles data persistence (CSV result)

### Data Flow

```
YAML Config → YamlReader → SurveyConfig → SurveyState → UI Components
                                              ↓
                                        SurveyDataManager → CSV Files
```

---

## Project Structure

```
survery-tool/
├── .github/
│   └── workflows/
│       ├── ci.yml              # Continuous integration
│       └── release.yml         # Release automation
├── composeApp/                 # Main application module
│   ├── src/
│   │   ├── androidMain/        # Android-specific code (disabled)
│   │   ├── commonMain/         # Shared code
│   │   │   ├── composeResources/
│   │   │   │   ├── drawable/   # Images and icons
│   │   │   │   └── values/     # Strings, app info
│   │   │   └── kotlin/com/zinkel/survey/
│   │   │       ├── config/     # Configuration loading
│   │   │       ├── data/       # Data management
│   │   │       ├── ui/         # UI components
│   │   │       └── Platform.kt # Platform abstractions
│   │   ├── commonTest/         # Shared tests
│   │   └── jvmMain/            # JVM-specific code
│   │       └── kotlin/com/zinkel/survey/
│   │           └── Main.kt     # Application entry point
│   └── build.gradle.kts        # Module build configuration
├── docs/                       # Usage and development documentation
├── examples/                   # Example survey configurations
├── gradle/
│   ├── wrapper/                # Gradle wrapper
│   └── libs.versions.toml      # Dependency version catalog
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Project settings
├── gradle.properties           # Gradle properties
├── template.yaml               # Survey configuration template
└── README.md                   # Project documentation
```

### Source Code Organization

```
com.zinkel.survey/
├── config/
│   ├── SurveyConfig.kt         # Data models for survey configuration
│   ├── SurveyConfigLoader.kt   # Configuration loading logic
│   └── YamlReader.kt           # YAML parsing
├── data/
│   ├── CSVAccess.kt            # CSV file operations
│   ├── SurveyContentData.kt    # Answer data models
│   └── SurveyDataManager.kt    # Data persistence manager
├── ui/
│   ├── elements/
│   │   ├── HighScore.kt        # Leaderboard UI
│   │   └── SurveyElements.kt   # Reusable UI components
│   ├── SurveyApp.kt            # Main app composable
│   ├── SurveyContentScreen.kt  # Question display screen
│   ├── SurveyLoadApp.kt        # Load screen app
│   ├── SurveyLoadScreen.kt     # Load screen UI
│   ├── SurveyLoadState.kt      # Load state management
│   ├── SurveyState.kt          # Survey state management
│   ├── SurveySummaryScreen.kt  # Summary/results screen
│   └── Theme.kt                # UI theme
└── Platform.kt                 # Platform-specific utilities
```

---

## Prerequisites

### Required

- **JDK 25** or higher
- **Gradle 9.0+** (included via wrapper)
- **Git** (for version control)

### Recommended

- **IntelliJ IDEA** with Kotlin plugin
- **Compose Multiplatform IDE Support** plugin
- **Git** client or IDE integration

---

## Setup

### 1. Clone Repository

```bash
git clone github.com/HuppiFluppi/survey-tool.git
cd survery-tool
```

### 2. Verify Java Version

```bash
java -version
# Should show version 25 or higher
```

### 3. Verify Gradle

```bash
./gradlew --version
# Windows: gradlew.bat --version
```

### 4. Import into IDE

**IntelliJ IDEA**:
1. File → Open → Select `survery-tool` directory
2. Wait for Gradle sync to complete
3. Ensure Project SDK is set to JDK 25+

---

## Build

### Build All

Compile all source sets and create artifacts:

```bash
./gradlew assemble
```

### Build Specific Targets

```bash
# JVM only
./gradlew jvmJar

# Clean build
./gradlew clean assemble
```

### Build with Version

```bash
./gradlew -Pversion=1.0.0 assemble
```

---

## Test

### Run All Tests

```bash
./gradlew allTests
```

Test reports are generated at:
- `composeApp/build/reports/tests/`

### Run Tests (Force Rerun)

```bash
./gradlew cleanAllTests allTests --no-build-cache
```

### Run Specific Test

```bash
./gradlew test --tests "com.zinkel.survey.ui.SurveyModelTest"
```

### Testing Framework

- **Kotlin Test**: Standard testing library
- **MockK**: Mocking framework for Kotlin
- **Compose UI Test**: UI testing (experimental)

---

## Run

### Run from Gradle

```bash
./gradlew run
```

### Run from IDE

1. Open `Main.kt` in `composeApp/src/jvmMain/kotlin/com/zinkel/survey/`
2. Click the green run icon next to `fun main()`
3. Or use Run → Run 'MainKt'

### Run with Arguments

```bash
./gradlew run --args="<arguments>"
```

---

## Release

### Package Uber JAR

Create a standalone executable JAR with all dependencies:

```bash
./gradlew packageUberJarForCurrentOS
```

Output: `composeApp/build/compose/jars/survey-tool-<os>-<arch>-<version>.jar`

### Package Release Uber JAR

```bash
./gradlew packageReleaseUberJarForCurrentOS
```

### Create Native Installers

#### Windows MSI

```bash
./gradlew packageReleaseMsi
```

Output: `composeApp/build/compose/binaries/main/msi/`

#### Linux DEB

```bash
./gradlew packageReleaseDeb
```

Output: `composeApp/build/compose/binaries/main/deb/`

### Release with Version

```bash
./gradlew -Pversion=1.2.3 packageReleaseUberJarForCurrentOS
```

---

## CI/CD

### Continuous Integration (CI)

**Trigger**: Push or PR to `master` branch

**Workflow** (`.github/workflows/ci.yml`):
1. Checkout code
2. Setup Java 25
3. Setup Gradle
4. Build: `./gradlew clean assemble`
5. Test: `./gradlew allTests`
6. Upload test reports

**Manual Trigger**: GitHub Actions → CI → Run workflow

### Release Workflow

**Trigger**: Push tag matching pattern `[0-9].[0-9]+.[0-9]+` (e.g., `1.0.0`)

**Workflow** (`.github/workflows/release.yml`):

#### Linux Job
1. Build and test
2. Package release JAR
3. Create GitHub release with Linux JAR

#### Windows Job
1. Build and test
2. Package release JAR
3. Upload Windows JAR to release

**Creating a Release**:

```bash
# Tag the commit
git tag 1.0.0
git push origin 1.0.0

# GitHub Actions will automatically:
# - Build for Linux and Windows
# - Run tests
# - Create GitHub release
# - Upload JAR artifacts
```

### Artifacts

CI/CD produces:
- **Test Reports**: Uploaded as workflow artifacts
- **Release JARs**: Attached to GitHub releases
  - `survey-tool-linux-x64-<version>-release.jar`
  - `survey-tool-windows-x64-<version>-release.jar`

---

## Development Workflow

### 1. Code Style

- Follow Kotlin coding conventions
- Follow convenctions and structure of existing code
- Use meaningful variable/function names
- Add KDoc comments for public APIs
- Keep functions focused and small

### 2. Testing

- Write tests for new features
- Ensure existing tests pass
- Aim for meaningful test coverage
- Use MockK for mocking dependencies

### 3. Commit Messages

Format: `<type>: <description>`

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `test`: Test changes
- `chore`: Build/tooling changes

Example: `feat: add dropdown support for choice questions`

---

## Code Organization

### Configuration Layer (`config/`)

**SurveyConfig.kt**
- Data classes representing immutable survey structure
- Enums for types (SurveyType, DataQuestionType, etc.)
- Sealed classes for question types

**YamlReader.kt**
- YAML parsing using SnakeYAML Engine
- Converts YAML to SurveyConfig objects

**SurveyConfigLoader.kt**
- File loading orchestration
- Error handling for invalid configurations

### Data Layer (`data/`)

**SurveyContentData.kt**
- Answer data models
- Response tracking

**SurveyDataManager.kt**
- Manages survey instances
- Handles data persistence

**CSVAccess.kt**
- CSV file I/O operations
- Export survey results
- Load/save leaderboard data

### UI Layer (`ui/`)

**SurveyLoadApp.kt / SurveyLoadScreen.kt**
- Initial screen for loading survey configurations
- File picker integration
- Error display

**SurveySummaryScreen.kt**
- Survey overview and metadata
- Leaderboard display
- Start/resume actions

**SurveyContentScreen.kt**
- Question rendering
- Answer collection
- Page navigation

**SurveyElements.kt**
- Reusable UI components
- Question type renderers (text, choice, rating, etc.)
- Input validation

**State Management**
- `SurveyLoadState.kt`: Load screen state
- `SurveyState.kt`: Survey navigation and answer state

---

## Key Components

### Survey Configuration Model

```kotlin
data class SurveyConfig(
    val title: String,
    val description: String,
    val type: SurveyType,
    val score: ScoreSettings,
    val pages: List<SurveyPage>
)
```

### Question Types

1. **TextQuestion**: Free-form text input
2. **ChoiceQuestion**: Single/multiple choice
3. **DataQuestion**: Structured data (name, email, phone)
4. **RatingQuestion**: Visual rating scales
5. **LikertQuestion**: Grid-based rating
6. **DateTimeQuestion**: Date/time picker
7. **SliderQuestion**: Numeric range/value slider
8. **Information**: Non-interactive content

### State Flow

```kotlin
// Load state
sealed class SurveyLoadUiState {
    object NotLoaded
    object Loading
    data class Loaded(val config: SurveyConfig)
    data class Error(val message: String)
}

// Survey state
class SurveyState {
    var currentPageIndex: Int
    var answers: Map<String, Any>
    var score: Int
    // ...
}
```

---

## Configuration Format

### YAML Structure

```yaml
title: "Survey Title"
description: "Survey description"
type: survey  # or quiz
score:
  show_question_scores: false
  show_leaderboard: true
  leaderboard:
    show_scores: true
    limit: 10

---  # Page separator
title: "Page 1"
content:
  - type: text
    title: "Question text"
    config:
      multiline: false
```

See `template.yaml` for complete documentation.

---

## Schema Files

**survey_schema.json:** in `docs/files` can be used to verify survey configuration templates.
It uses json schema and can be used together with json schema validation tools (e.g. in the IDE).
To match the document structure of a template file, `anyOf` is used to handle survey head or page data.

> Note: Tool support of multiple documents in a single yaml file might be limited 

Make sure to update the schema file to reflect any changes to the survey configuration input.

---

## Contributing

### Pull Request Process

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Ensure all tests pass
5. Update documentation
6. Submit PR with clear description

### Code Review Checklist

- [ ] Code follows Kotlin and project conventions
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes (or documented)
- [ ] CI passes

### Issue Reporting

Include:
- Clear description
- Steps to reproduce
- Expected vs actual behavior
- Environment (OS, Java version)
- Sample configuration (if applicable)

---

## HowTo

### Add or extend a question

To add a new question or extend an existing one, several steps must be taken.
There are two main approaches for this. First, one could go from the configuration yaml and follow the chain till the ui element.
Second approach is to create the ui element and then work backwards to make all necessary changes in the surrounding classes.

Following steps need to be taken.
1. Add the new configuration to `template.yaml` together with a description of the option and settings
2. Add the new configuration to `SurveyConfig.kt` to add your new question or add to existing one
3. Alter `YamlReader.kt` to reflect the changes (might not be needed depending on the changes)
4. For new questions, alter `SurveyContentData.kt` to add the question
5. Alter `SurveyElements.kt` to make sure the new question or configuration changes are visible in UI
6. For new questions, add an `updateAnswer` overload in `SurveyState.kt`
7. For new questions, add a branch for when in `SurveyContentScreen.kt`
8. Add to [User Guide](usage.md) documentation and check if examples need update (e.g. besides the template.yaml, all questions should be in `minimal-all-question-types-survey.yaml`) 
9. Change survey schemas in `docs/files/` to reflect changes.

Make sure to not hardcode string and use the compose resource handling.

Examples:
- Addition of Slider question: [Commit](https://github.com/HuppiFluppi/survey-tool/commit/17ea42e633f4db9022b60ae2bd49a7137889ceb6)
- Extension of Choice question to allow horizontal ui: [Commit](https://github.com/HuppiFluppi/survey-tool/commit/64e69a2daaf1c2b07435d26bf60a220cdbf3c929)
- Extension of Rating question to allow numbers: [Commit](https://github.com/HuppiFluppi/survey-tool/commit/f3938e4b83008ac935a9a773b81fb30a1adf7393)

---

## Additional Resources

- **Compose Multiplatform**: https://www.jetbrains.com/lp/compose-multiplatform/
- **Kotlin Documentation**: https://kotlinlang.org/docs/
- **Gradle User Guide**: https://docs.gradle.org/
- **SnakeYAML**: https://bitbucket.org/snakeyaml/snakeyaml-engine/

---

## License

This project is licensed under the MIT License. See [LICENSE](../LICENSE) file for details.
