<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# diff-coverage-idea-plugin Changelog

## [Unreleased]
### Added
- Generate HTML report by Gradle plugin even if Diff Coverage Gradle plugin disables this option
### Changed
- Diff Coverage is run by run configuration instead of shortcut
### Fixed
- Show Diff Coverage report notification if `diffCoverage` Gradle task failed due to violation rules
- Displaying all packages on code coverage view with checked 'Flatten Packages'

## [0.0.4]
### Fixed
- Intellij 2021.1 compatibility
### Changed
- Code coverage classes list flat structure

## [0.0.3]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
