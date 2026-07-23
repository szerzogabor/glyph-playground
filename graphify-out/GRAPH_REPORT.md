# Graph Report - glyph-playground  (2026-07-23)

## Corpus Check
- 19 files · ~11,074 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 149 nodes · 251 edges · 14 communities (11 shown, 3 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 14 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6182390e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GlyphPattern
- GlyphToyService
- renderWidget
- MainActivity.kt
- WidgetPrefs
- .sampleCells
- GlyphMatrixPreview
- glyph-playground — agent guide
- GlyphLayout
- GlyphBitmapRenderer
- Screen

## God Nodes (most connected - your core abstractions)
1. `GlyphPattern` - 16 edges
2. `PatternRepository` - 13 edges
3. `GlyphToyService` - 13 edges
4. `GlyphController` - 10 edges
5. `GlyphPlaygroundApp()` - 9 edges
6. `EditorScreen()` - 9 edges
7. `renderWidget()` - 9 edges
8. `WidgetPrefs` - 9 edges
9. `glyph-playground — agent guide` - 8 edges
10. `ImageToGlyph` - 7 edges

## Surprising Connections (you probably didn't know these)
- `renderWidget()` --calls--> `PatternRepository`  [INFERRED]
  app/src/main/java/com/emberforge/generated/glyphplayground/widget/GlyphWidgetProvider.kt → app/src/main/java/com/emberforge/generated/glyphplayground/GlyphPattern.kt
- `GlyphPlaygroundApp()` --calls--> `PictureToGlyphScreen()`  [INFERRED]
  app/src/main/java/com/emberforge/generated/glyphplayground/MainActivity.kt → app/src/main/java/com/emberforge/generated/glyphplayground/ui/PictureToGlyphScreen.kt
- `EditorScreen()` --calls--> `GlyphMatrixCanvas()`  [INFERRED]
  app/src/main/java/com/emberforge/generated/glyphplayground/MainActivity.kt → app/src/main/java/com/emberforge/generated/glyphplayground/ui/GlyphMatrixCanvas.kt
- `PresetChip()` --calls--> `GlyphMatrixPreview()`  [INFERRED]
  app/src/main/java/com/emberforge/generated/glyphplayground/MainActivity.kt → app/src/main/java/com/emberforge/generated/glyphplayground/ui/GlyphMatrixCanvas.kt
- `PatternCard()` --calls--> `GlyphMatrixPreview()`  [INFERRED]
  app/src/main/java/com/emberforge/generated/glyphplayground/MainActivity.kt → app/src/main/java/com/emberforge/generated/glyphplayground/ui/GlyphMatrixCanvas.kt

## Import Cycles
- None detected.

## Communities (14 total, 3 thin omitted)

### Community 0 - "GlyphPattern"
Cohesion: 0.19
Nodes (12): fromImportJson(), GlyphPattern, parseBrightnessJson(), parseJson(), PatternRepository, toExportJson(), ConfigRow(), ConfigScreen() (+4 more)

### Community 1 - "GlyphToyService"
Cohesion: 0.19
Nodes (8): GlyphToyService, hide(), Context, GlyphMatrixManager, Intent, show(), IBinder, Service

### Community 2 - "renderWidget"
Cohesion: 0.29
Nodes (10): broadcast(), GlyphWidgetProvider, Context, Intent, refreshAll(), renderWidget(), AppWidgetManager, AppWidgetProvider (+2 more)

### Community 3 - "MainActivity.kt"
Cohesion: 0.14
Nodes (15): android, GlyphController, GlyphMatrixManager, EditorScreen(), GlyphPlaygroundApp(), Bundle, ComponentActivity, LibraryScreen() (+7 more)

### Community 5 - ".sampleCells"
Cohesion: 0.40
Nodes (4): CellGrid, ImageToGlyph, Bitmap, FloatArray

### Community 6 - "GlyphMatrixPreview"
Cohesion: 0.36
Nodes (8): drawGrid(), drawGridAt(), GlyphMatrixCanvas(), GlyphMatrixPreview(), hitIndex(), PictureToGlyphScreen(), Modifier, Offset

### Community 7 - "glyph-playground — agent guide"
Cohesion: 0.11
Nodes (16): Building locally, Glyph Matrix SDK, glyph-playground — agent guide, graphify, Ground rules, Project layout, What CI does, What it does (+8 more)

### Community 10 - "Screen"
Cohesion: 0.25
Nodes (6): Screen, EDITOR, LIBRARY, PICTURE_TO_GLYPH, PredefinedGlyph, PredefinedGlyphs

## Knowledge Gaps
- **16 isolated node(s):** `EDITOR`, `LIBRARY`, `PICTURE_TO_GLYPH`, `What it does`, `Glyph Matrix SDK` (+11 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PatternRepository` connect `GlyphPattern` to `GlyphToyService`, `renderWidget`, `MainActivity.kt`?**
  _High betweenness centrality (0.160) - this node is a cross-community bridge._
- **Why does `GlyphPattern` connect `GlyphPattern` to `GlyphToyService`, `MainActivity.kt`?**
  _High betweenness centrality (0.156) - this node is a cross-community bridge._
- **Why does `GlyphToyService` connect `GlyphToyService` to `GlyphPattern`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `PatternRepository` (e.g. with `.loadGlyphs()` and `.onCreate()`) actually correct?**
  _`PatternRepository` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `EDITOR`, `LIBRARY`, `PICTURE_TO_GLYPH` to the rest of the system?**
  _16 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MainActivity.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1383399209486166 - nodes in this community are weakly interconnected._
- **Should `glyph-playground — agent guide` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._