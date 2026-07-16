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

The app integrates the official [GlyphMatrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit):
- SDK AAR lives in `app/libs/glyph-matrix-sdk-2.0.aar`
- Device identifier: `Glyph.DEVICE_23112` (Phone 3)
- Matrix: 25×25 = 625 individually addressable LEDs
- Uses `setAppMatrixFrame(int[])` for in-app display
- Permission: `com.nothing.ketchum.permission.ENABLE`
- minSdk 33 (required by SDK)

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

- `app/` — the application module (`namespace`/`applicationId`
  `com.emberforge.generated.glyphplayground`).
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
