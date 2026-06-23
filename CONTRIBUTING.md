# Contributing to Hermes Dispatch

Thanks for your interest! This is an early-stage open-source project.

## Getting started
1. Install Android Studio (Ladybug or newer) / JDK 17.
2. Clone and open the project; let Gradle sync.
3. Run the checks:
   ```bash
   ./gradlew testOssDebugUnitTest lintOssDebug assembleOssDebug
   ```

## Ground rules
- **Architecture:** MVVM + Hilt + Repository. Keep transport (`data/remote`),
  storage (`data/local`), and UI (`ui/`) separate. Business logic that can be a
  pure function (e.g. parsers) should live in a testable, I/O-free unit.
- **Tests:** add JVM unit tests for logic you can test without a device
  (see `SseParserTest`). PRs that change parsing/mapping must include tests.
- **Security:** never commit secrets, keystores, or `google-services.json`.
  Don't add cleartext-HTTP allowances to release config. Store credentials only
  via `SecureSettings`. See [SECURITY.md](SECURITY.md).
- **Flavors:** keep the `oss` flavor free of Google/Firebase dependencies
  (F-Droid eligibility). Firebase code belongs only under `src/play/`.
- **Commits:** use [Conventional Commits](https://www.conventionalcommits.org)
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- **Style:** official Kotlin style (`kotlin.code.style=official`); keep it
  idiomatic and match surrounding code.

## Pull requests
Keep PRs focused and incremental. Describe what changed and how you verified it.
CI (unit tests + lint + assemble) must pass.

By contributing you agree your contributions are licensed under [GPL-3.0](LICENSE).
