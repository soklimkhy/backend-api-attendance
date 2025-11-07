## Quick orientation for AI coding agents

This is a small Kotlin Spring Boot REST API that uses Google Firebase (Firestore) for persistence and JWT/BCrypt for authentication. The goal of these notes is to provide the concrete, discoverable facts an automated coding assistant needs to be immediately productive in this repository.

Highlights
- Language & framework: Kotlin + Spring Boot (plugin and boot version in `build.gradle.kts`). Java toolchain target: 17.
- Persistence: Google Firestore via Firebase Admin SDK (`firebase-admin`, `google-cloud-firestore`). Firestore client bean is created in `src/main/kotlin/com/example/api_attendance/config/FirebaseConfig.kt`.
- Auth: local user records with BCrypt (`AuthService.kt`), JWT configured via `src/main/resources/application.yml` (secret + expirations), endpoints exposed in `src/main/kotlin/com/example/api_attendance/controller/AuthController.kt`.
- Main entry: `src/main/kotlin/com/example/api_attendance/ApiAttendanceApplication.kt` (initializes Firebase using `src/main/resources/firebase-service-account.json`).

Important files to inspect (examples of patterns)
- `build.gradle.kts` — dependency list and toolchain (use Gradle wrapper in CI/dev).
- `ApiAttendanceApplication.kt` — application bootstrap and Firebase initialization.
- `FirebaseConfig.kt` — creates Firestore `@Bean` and shows where the service-account JSON is read.
- `AuthService.kt` — BCrypt usage and simple register/login behavior (throws IllegalArgumentException on errors).
- `AuthController.kt` — REST patterns: controllers catch exceptions and return ResponseEntity with simple maps (`message`/`error`).
- `src/main/resources/application.yml` — runtime configuration (server port, jwt.secret, expirations).
- `HELP.md` — repo-specific gotchas (package name uses underscore: `com.example.api_attendance`).

Project-specific conventions and gotchas
- Package name: the original hyphenated package was invalid; code uses `com.example.api_attendance` (underscore) — follow this when adding new packages.
- Firebase service account: the app reads `src/main/resources/firebase-service-account.json` at startup. This file contains secrets and should not be committed; automated changes must not attempt to print or exfiltrate its contents.
- Firebase initialization appears both in `ApiAttendanceApplication.kt` and `FirebaseConfig.kt`. Prefer using `FirebaseConfig`'s bean if adding Firestore usage; check for duplicate initialization if modifying startup logic.
- Error handling style: controllers use try/catch and return ResponseEntity with status 400 or 401 and a JSON map; preserve this style for consistency.
- Authentication: passwords are stored hashed with `BCryptPasswordEncoder` in `AuthService`. Do not store plaintext or change to a different hashing mechanism without updating all usages and tests.

Build / run / test (developer commands)
- Build (Windows PowerShell using wrapper):
  - `.
  \gradlew.bat build`
- Run app locally (boots Spring + Firebase init):
  - `.
  \gradlew.bat bootRun` or run from IDE the `DemoApplication` (class in `ApiAttendanceApplication.kt`).
- Run tests:
  - `.
  \gradlew.bat test`
- Notes: Gradle uses toolchain Java 17. CI should use the Gradle wrapper present in the repo to ensure reproducible builds.

Integration points & external dependencies
- Firebase Admin / Firestore — initialized with a service account JSON in `src/main/resources/firebase-service-account.json` and project id `taskmaster-fectk` (visible in code). Any change to Firestore usage should consult `FirebaseConfig.kt`.
- JWT — configured via `application.yml` (`jwt.secret`, `access-token-expiration`, `refresh-token-expiration`). Keep the secret in secure stores for production.

How to implement a small change safely (example)
1. To add a new REST endpoint that reads/writes Firestore: create a `@RestController` under `controller/`, inject Firestore via the `@Bean` from `FirebaseConfig` (type `com.google.cloud.firestore.Firestore`).
2. Follow existing ResponseEntity try/catch pattern and return simple maps for message/error.
3. Add or update unit tests using JUnit 5 (tests live under `src/test/kotlin`), and run `.
   \gradlew.bat test` locally.

What not to do
- Do not commit or echo the contents of `firebase-service-account.json` or any secrets found in `application.yml`.
- Avoid changing package root `com.example.api_attendance` to a different base without updating `settings.gradle.kts` and package declarations.

If you need clarification
- If a requested change touches deployment or credentials, ask before editing – this repo requires a valid Firebase service account for runtime testing.

---
If you'd like, I can refine these notes by adding explicit examples (e.g., a small controller template that injects Firestore) or merge in any existing agent docs if you point me to them.
