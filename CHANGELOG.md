# Changelog

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Phase 1 core pipeline: PDF parsing (PDFBox), SQLite persistence,
  on-device translation via Ollama, deterministic semantic verification
  (AdaptiveFlow), OCR (Tess4J), PDF reconstruction, hardware detection
  (OSHI), and the composition root with a headless self-check.
- JavaFX application shell (phase 1 status window).
- CI: build and test on three OSes, code quality gates (Checkstyle, PMD,
  SpotBugs), dependency review, CodeQL, release and native package
  workflows, Dependabot.

## [0.1.0] - 2026-08-11

### Added

- Initial project structure: domain, application, infrastructure, app,
  and packaging modules with 35 passing test classes.
