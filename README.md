# Survey Tool

A lightweight tool for creating and running surveys or quizzes. Using Kotlin and Compose Multiplatform. 

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Quick Start](#quick-start)
- [Scoring & Leaderboard](#scoring--leaderboard)
- [Development](#development)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Overview
Survey Tool abstracts survey/quiz definition through a declarative configuration in yaml.

The idea goes back some years when I was tasked to create a quiz for visitors of a job fair.
We wanted to offer a programming quiz at our booth that applicants could fill out to show their skills and win prizes.
As a bonus, we were able to contact the top scorers for possible job offers.

While I quickly thought about creating this as a Kotlin Compose app, I was missing the time back then to do it.
Instead, I went with a different (quick and dirty) approach at the time.
Now I came back to create this tool to try out Compose Multiplatform and learn something new. 
And maybe help others who come across this repo, having the same need I had years ago.

## Features
- Declarative survey/quiz configuration
- Support for multiple pages
- Support for different survey questions (Text, Multiple Choice, Rating, Likert)
- Quizzes with points and optional leaderboard
- Currently supports Desktop

## Tech stack
- Kotlin: 2.2 / JVM 21
- Build system: Gradle (Kotlin DSL)
- UI: Jetpack Compose (Android) or Compose Multiplatform

## Quick Start

Steps to use:

1) Define a survey configuration
- Check the [template](template.yaml) for a documented configuration
- Check the [examples folder](examples) for different kind of survey and quiz configurations
- Add questions to your like
- Configure settings as needed

2) Run the survey
- Run the survey tool
- Load the survey configuration

## Scoring & Leaderboard
- Scoring is optional and typically enabled for quiz-type surveys.
- Points must be configured per question.
- Leaderboard can be configured to display scores.

## Roadmap
- [ ] Enable Android target
- [ ] Refactor package structure and remedy some code smells
- [ ] Add support for specifying the participant name question 
- [ ] Add possibility to resume survey by loading existing data (highscore, instanceId, etc.)
- [ ] Add remote configuration loading and data sending
- [ ] Add support for more question types
- [ ] Add multi survey languages support
- [ ] Add support for images (Background, Summary, Page Header)
- [ ] Add support for custom color themes
- [ ] Add fullscreen/kiosk mode (Desktop)
- [ ] Add conditional pages

## Contributing
Contributions are welcome! Please:
- Open issues with clear steps to reproduce and expected behavior
- Submit Pull Requests with concise descriptions, tests (where applicable), and clean commit history
- Follow the project’s coding style and patterns

## Disclaimer
This software is provided "as is", without warranty of any kind. The author is certain, parts of this software could be done better.
The todos are plenty and bugs are likely hiding. Use at your own risk and have fun. This is a learning and experiment project.

## Development

### Flow & State of loaded survey
- Initial: Summary (overview with metadata and actions)
- Start: Transition into the first content page
- Advance: Move to the next page until completion
- Cancel: Return to the summary at any time

The model tracks:
- Survey metadata (title, type, page count)
- Current page and total progress
- Page content and optional per-question score visibility
- Leaderboard configuration (if enabled)

### Build
Gradle tasks:
- Build: `./gradlew assemble`
- Run (Desktop): `./gradlew run`

### Test
- Unit tests: `./gradlew test`
- Lint/Static analysis: Use your preferred tools (e.g., IDE inspections, Detekt, Ktlint)
- Consider CI integration to enforce checks

## License
This project is provided under MIT license. See [license file](LICENSE)
