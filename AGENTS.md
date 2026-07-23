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

## Ground rules

- **Do not touch `keystore/`.** CI generates this project's own
  persistent debug keystore there on the first run and commits it. Every
  build must keep signing with that same key, or installed copies can no
  longer update in place.
- The app is signed with a committed debug keystore on purpose — this is
  a demo distribution channel, not a Play Store release.

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
gradle assembleRelease
```

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
