# diff-coverage-idea-plugin

![Build](https://github.com/SurpSG/diff-coverage-idea-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/16403-diff-coverage.svg)](https://plugins.jetbrains.com/plugin/16403-diff-coverage)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/16403-diff-coverage.svg)](https://plugins.jetbrains.com/plugin/16403-diff-coverage)

* Create or open gradle project
* Apply and setup [diff coverage plugin](https://github.com/form-com/diff-coverage-gradle)
* Make some changes to your code and run tests to collect coverage info
* Run diff coverage idea plugin: `ctrl+shift+d`

Diff coverage IDEA plugin:
* detects a module to which DiffCoverage Gradle plugin is applied
* Runs `diffCoverage` Gradle task
* Displays coverage windows for modified code
* Shows balloon with a link to diff coverage html report 

<!-- Plugin description -->

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "diff-coverage-idea-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/SurpSG/diff-coverage-idea-plugin/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
  
