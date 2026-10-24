# glyph-playground — agent guide

This is an **Android application**, scaffolded by EmberForge Forge.

## What it does

Glyph Playground is a drawing/design tool for the Nothing Phone (3) Glyph
Matrix. Users can:
- See the **25×25 Glyph Matrix** grid rendered on-screen
- Toggle individual LEDs on/off by tapping or dragging
- Save named patterns to local storage
- Load saved patterns from a library
- Display patterns on the **actual Glyph Matrix hardware** via the official
  Nothing Glyph Matrix SDK (`glyph-matrix-sdk-2.0.aar`)

## Glyph Matrix SDK

**Official SDK:** https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit

The app integrates the official GlyphMatrix Developer Kit:
- SDK AAR lives in `app/libs/glyph-matrix-sdk-2.0.aar`
- Device identifier: `Glyph.DEVICE_23112` (Phone 3)
- Matrix: 25×25 grid, physically **round** — LEDs are arranged in a
  circular disc; corner cells outside the circle have no physical LED
- Uses `setAppMatrixFrame(int[])` for in-app display
- Permission: `com.nothing.ketchum.permission.ENABLE`
- minSdk 33 (required by SDK)
- The UI canvas clips to a circle and uses round LED dots to match the
  physical Glyph Matrix shape. `GlyphLayout.VALID_INDICES` holds the set
  of indices that fall inside the circular boundary.

## What CI does

On every push to `main`, `.github/workflows/ci.yml`:
1. Builds a signed release APK (`gradle assembleRelease`).
2. Uploads it as a **GitHub Release** (tag `v1.0.<run_number>`), together
   with a slim `metadata.json` (`versionCode`, `versionName`,
   `downloadUrl`, `sizeBytes`) that the EmberForge app reads to list and
   install releases.

`versionCode` is `github.run_number`; `versionName` is
`1.0.<run_number>`.

On every pull request against `main`, `.github/workflows/pr-build.yml`
runs the same `gradle assembleRelease` and publishes nothing — it exists
so a broken build (especially a toolchain bump) fails on the PR instead
of on `main`, where a failure also costs that run's release APK. It also
bundles the unsigned Google Play AAB (`gradle :app:bundlePlay`) so a
broken Play setup fails there too.

## Google Play

Google Play is a separate channel, not CI:
- The `play` build type (`:app:bundlePlay`) is signed with the **upload
  key**, never with the committed debug keystore (that key and its
  password are public). The key and the Play service account's JSON key
  stay on the build machine, named by `GLYPH_UPLOAD_PROPERTIES` and
  `GLYPH_PLAY_SERVICE_ACCOUNT` in its `~/.gradle/gradle.properties`.
- The forge app `glyph-playground-play`
  (`tools/glyph-playground-play.forge.yaml`) builds the **`release`
  branch** on the `play-upload` machine and uploads it with Gradle Play
  Publisher (`./gradlew :app:publishPlayBundle`) to the internal track.
  Merging `main` into `release` is a Play release.
- The Play `versionCode` is the commit count of the built ref
  (`git rev-list --count HEAD`), so it needs a full clone.
- `.github/workflows/release-aab.yml` is manual only and debug-signed:
  for local testing, never for Play.

## Ground rules

- **Do not touch `keystore/`.** CI generates this project's own
  persistent debug keystore there on the first run and commits it. Every
  build must keep signing with that same key, or installed copies can no
  longer update in place.
- The `release` and `debug` builds are signed with the committed debug
  keystore on purpose — the GitHub Release APK is a demo distribution
  channel. The Play build never uses that key (see "Google Play").
- **Keep `targetSdk` at the newest stable API level.** Google Play
  enforces a target API level floor every year (API 36 / Android 16 from
  Aug 31, 2026), and an app below it can no longer be updated on Play.
  Bumping `targetSdk`/`compileSdk` usually drags AGP and Gradle along:
  compileSdk 36 needs AGP 8.9+, which needs Gradle 8.11.1+ (the
  workflows pin 8.13). `minSdk` stays at 33 — that is a Glyph SDK
  requirement, not a Play one.

## Project layout

- `app/` — the application module. Code `namespace` is
  `com.emberforge.generated.glyphplayground`; the published
  `applicationId` (the APK/AAB package name) is
  `com.ogbar.glyphplayground`.
  - `libs/glyph-matrix-sdk-2.0.aar` — Nothing Glyph Matrix SDK.
  - `src/main/java/com/emberforge/generated/glyphplayground/`
    - `MainActivity.kt` — single Compose Activity with Editor, Library,
      and Preview screens.
    - `GlyphLayout.kt` — 25×25 grid constants and index helpers.
    - `GlyphPattern.kt` — `GlyphPattern` data class and
      `PatternRepository` (SharedPreferences-backed storage).
    - `GlyphController.kt` — wraps `GlyphMatrixManager` from the SDK
      for displaying patterns on the physical Glyph.
    - `ui/GlyphMatrixCanvas.kt` — Compose Canvas rendering the 25×25
      pixel-art style grid with tap and drag support.
  - `src/main/AndroidManifest.xml`, `src/main/res/` — manifest and
    resources.
- `build.gradle.kts`, `settings.gradle.kts` — Gradle setup (AGP + Kotlin
  + Compose compiler plugins, versions pinned).

## Building locally

```sh
./gradlew assembleRelease     # or: gradle assembleRelease (Gradle 8.13)
```

The Gradle wrapper (8.13) is there for machines without Gradle on PATH,
such as the forge nodes.

There is no unit-test step in CI yet — add one (and tests) when the work
calls for it.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
