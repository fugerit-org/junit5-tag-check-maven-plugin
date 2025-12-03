# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- report examples (documentation)

### Changed

- Better log handling of missing tags

### Fixed

- Wrap URLClassLoader creation in try-with-resources to ensure it's closed and avoid leaks. <https://github.com/fugerit-org/junit5-tag-check-maven-plugin/pull/2>

## [1.1.0] - 2025-11-28

### Added

- PDF report

### Changed

- fj-doc-version 8.17.9
- report generation engine with [Venus Fugerit Doc](https://github.com/fugerit-org/fj-doc)

### Fixed

- security issue <https://github.com/fugerit-org/junit5-tag-check-maven-plugin/security/code-scanning/1>

## [1.0.2] - 2025-11-26

### Fixed

- HTML output sanitized

## [1.0.1] - 2025-11-26

### Fixed

- documented output in json, xml and text

## [1.0.0] - 2025-11-26

### Added

- report-executed-tags mojo
