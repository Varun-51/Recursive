# Recursive

> Translation that thinks twice.

**Local-first Java 21 desktop application that translates large PDF documents
using on-device AI — and verifies its own work before you see a single word.**

Machine translation that "just converts text" is not translation. Recursive
translates, then it *checks*: it re-reads the original and its own output as
an independent reviewer, confirms meaning, intent, negation, numbers, units,
proper nouns, references, and terminology all survived — and when they did
not, it re-translates with the failure analysis as guidance, up to a bounded
attempt budget. Everything runs through Ollama on your machine. No cloud,
no API keys, and no document ever leaves the computer.

## Why Recursive

Read a translated contract, dosage sheet, or safety manual and the danger is
rarely the wrong meaning — it is the lost **"not"**. A single dropped negation
turns a prohibition into an instruction. Recursive's recursive loop is built
to catch exactly that class of error:

| Check | What it protects |
|---|---|
| **Meaning & intent** | instructions, warnings, permissions, prohibitions survive translation |
| **Negation** | `must` stays `must`, `must not` stays `must not` — across every retry |
| **Numbers** | quantities, percentages, dates, times, decimals, ranges, version numbers match exactly |
| **Units** | kg, km, °C, %, MB, $, € — preserved, never converted |
| **Proper nouns** | names, product codes, IDs, URLs — never translated |
| **References** | section numbers, figure references, placeholders — intact |
| **Relationships** | cause/effect, conditions, comparisons, dependencies, sequence — logically consistent |
| **Terminology** | one technical term → one consistent translation throughout the document |

## The recursive loop

Every translation unit flows through the same state machine:

```
ORIGINAL TEXT
     ▼
[1] TRANSLATE ──────► [2] VALIDATE ──── match? ──yes──► ACCEPT AND STORE
(on-device LLM)          (meaning,            │
                          intent, numbers,     no
                          negation, units)     ▼
                                     [4] IDENTIFY WHAT FAILED
                                              ▼
                                     [5] RE-TRANSLATE (failure analysis as guidance)
                                              ▼
                                     [6] VALIDATE AGAIN ── pass? ──yes──► ACCEPT
                                                        │
                                                        no
                                                        ▼
                                               [7] BUDGET EXHAUSTED?
                                               yes ──► MARK NEEDS_REVIEW
```

The validate → re-translate → validate cycle is bounded (3 attempts by
default); anything that survives the loop without passing is flagged
**NEEDS_REVIEW** and surfaced in a dedicated review screen with the detected
issues, never silently accepted.

## Architecture

Clean Architecture with domain-driven boundaries, delivered as five Maven
modules:

```
recursive/
├── recursive-domain/          entities, value objects, ports — depends only on the JDK
├── recursive-application/     use cases, orchestration, commands/DTOs, progress events
├── recursive-infrastructure/  adapters: PDFBox, Tess4J, Ollama client, SQLite, OSHI
├── recursive-app/             JavaFX presentation layer (main entry point)
└── recursive-packaging/       jpackage configuration and installer assets
```

Dependency rule: `domain ← application ← infrastructure ← app`. The domain
layer has no external dependencies; every integration point is a port
(`TranslationEngine`, `SemanticValidator`, `DocumentParser`,
`DocumentReconstructor`, `JobRepository`, `PageRepository`,
`BlockRepository`, `GlossaryRepository`, `ImageRepository`,
`ModelProvider`, `OcrEngine`, `HardwareDetector`) implemented in
infrastructure.

Two deliberate architecture decisions worth knowing:

- **AdaptiveFlow powers the micro-loop, not the job.** The
  translate → validate → re-translate loop inside each block is executed as
  an AdaptiveFlow micro-pipeline (built on
  [AdaptiveFlow](https://github.com/Varun-51/AdaptiveFlow), the author's own
  Java 21 DAG library) — the retry, backoff, and failure-isolation semantics
  come from a battle-tested engine. Job orchestration (pause, resume,
  progress, crash recovery) is owned by the application layer and persisted
  to SQLite; those are two different problems and get two different engines.
- **SQLite is a checkpoint, not an afterthought.** Every block completion is
  written immediately, so pause/resume and crash recovery are trivial: on
  restart, the app picks up the first non-completed page. Memory is managed
  aggressively — one page at a time, bounded context windows, and a heap
  monitor that forces GC before a large page is loaded.

## Tech stack

| Concern | Choice |
|---|---|
| Language / JVM | Java 21 (virtual threads) |
| UI | JavaFX 21 (OpenJFX) |
| PDF processing | Apache PDFBox 3.0 |
| OCR (scanned pages) | Tess4J (Tesseract wrapper) |
| On-device AI | Ollama (HTTP on `localhost:11434`) |
| Persistence | SQLite via JDBC + HikariCP |
| Micro-pipeline engine | AdaptiveFlow 1.0.5 |
| Hardware detection | OSHI 6.6 |
| JSON | Jackson 2.16 |
| Logging | SLF4J + Logback |
| Packaging | jpackage (`.exe` / `.app` / `.deb`) |
| Testing | JUnit 5 + AssertJ |
| Targets | Windows, macOS, Linux |

## Building

```shell
git clone https://github.com/Varun-51/Recursive.git
cd Recursive
./mvnw verify
```

Requires JDK 21. Tests run against an embedded HTTP fake for the Ollama
client and a temporary database — no Ollama installation is required to
build or test.

## Packaging

A bundle with a bundled JVM can be produced locally without any installer
toolchain:

```shell
./mvnw -DskipTests -P release -Dskip.jpackage=false package
```

This shades all dependencies (including the native JavaFX libraries) into
the app jar and places a runnable application image in
`recursive-packaging/target/dist/Recursive/`. The `release` profile is what
makes the shaded jar self-contained; the native-package CI workflow then
builds the platform installers (.exe / .deb / .dmg) on the runners that
carry the required toolchains (WiX on Windows).

## Running

```shell
./mvnw -pl recursive-app -am javafx:run
```

First launch detects hardware (RAM/CPU/GPU/disk), checks for Ollama, and
guides you: recommended model selection for your hardware, download of the
default model if none is installed, then the dashboard.

A translation job needs three things: a **source PDF**, a **source/target
language pair**, and a **local model** from

```shell
ollama list
```

## Application screens

The JavaFX shell provides seven screens, reached from the sidebar:

| Screen | Purpose |
|---|---|
| **Dashboard** | hardware summary, model/API status, per-job progress |
| **Setup** | first-run guide: hardware facts, Ollama server start, recommended model pull |
| **Jobs** | create jobs (name, language pair, PDF, model), start / pause / resume / cancel, live counters |
| **Translate** | the pipeline: ingest a PDF, translate page by page, review blocks, edit and re-validate them, export the finished document |
| **Models** | locally installed Ollama models with a hardware-based recommendation, plus remote OpenAI-compatible catalogs |
| **Settings** | recursion policy (max depth, stable passes, confidence threshold), storage locations, version information |
| **Logs** | live view of the in-process log ring with a severity filter |

All blocking work (database queries, OCR, model requests) runs off the
JavaFX thread via `BackgroundTasks`; every screen reflects failures in the
status line instead of crashing.

## Development status

| Phase | Deliverable | Status |
|---|---|---|
| 0 | Maven skeleton, repo layout | shipped |
| 1 | Domain model, ports, SQLite persistence, hardware detection, Ollama process management | shipped |
| 2 | PDF parsing, chunking, OCR, translation engine, semantic validator, AdaptiveFlow micro-pipeline | shipped |
| 3 | Job orchestration, worker pool, events, pause/resume, crash recovery, reconstruction | shipped |
| 4 | JavaFX screens (dashboard, jobs, translate/review pipeline, models, settings, logs) | shipped |
| 5 | jpackage packaging, first-launch wizard (setup screen), documentation | shipped |

## License

MIT — see [LICENSE](LICENSE).
