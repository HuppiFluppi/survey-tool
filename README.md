# Survey Tool

A lightweight tool for running surveys or quizzes. Whether you're collecting customer feedback, running a trivia night, or conducting research, Survey Tool provides an easy-to-use interface for participants to answer questions.
The tool loads a configuration file (YAML format) that defines your survey questions, and then presents them to participants in a clean, intuitive interface. It uses Kotlin and Compose Multiplatform. 

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Quick Start](#quick-start)
- [Development](#development)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Disclaimer](#disclaimer)
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

See [User Guide](docs/usage.md) for more details on how to create and run a survey.

Steps to use:
1) Define a survey configuration
   - Check the [template](template.yaml) for a documented configuration
   - Check the [examples folder](examples) for different kind of survey and quiz configurations
   - Add questions to your like
   - Configure settings as needed
   
2) Run the survey
   - Run the survey tool
   - Load the survey configuration

## Roadmap
- [x] Add possibility to resume survey by loading existing data (highscore, instanceId, etc.)
- [x] Add support for specifying the participant name/mail/phone
- [x] Add content element with text+description+image
- [x] Extend question types I (DateTimePicker, Dropdown, Configurable Rating(Icon, Color, Scale))
- [x] Extend documentation (question types, survey config, etc.)
- [x] Add Github build pipeline for release
- [x] Add Slider/Range, number rating & horizontal choice question type
- [x] Add support for text input validation and more data patterns (age, birthday, nickname)
- [x] Add support for images (summary page background, survey description, highscore background, page header)
- [ ] Add conditional pages/question
- [x] Add command line arguments to load configuration
- [ ] Add system test (load, fill, submit, check)
- [ ] Refactor package structure and remedy some code smells
- [ ] Add remote configuration loading and data sending
- [ ] Enable Android target
- [ ] Add fullscreen/kiosk mode (Desktop)
- [ ] Add survey multi language support
- [ ] Add support for custom color themes
- [ ] Extend question types II (ImageChoice, LikertSlider, ?)

## Contributing
Contributions are welcome! Please:
- Open issues with clear steps to reproduce and expected behavior
- Submit Pull Requests with concise descriptions, tests (where applicable), and clean commit history
- Follow the project’s coding style and patterns

See [Developer Guide](docs/development.md#contributing) for more details.

## Disclaimer
This software is provided "as is", without warranty of any kind. The author is certain, parts of this software could be done better.
The todos are plenty and bugs are likely hiding. Use at your own risk and have fun. This is a learning and experiment project.

## Development
See [Developer Guide](docs/development.md) for development information.

## License
This project is provided under MIT license. See [license file](LICENSE)
