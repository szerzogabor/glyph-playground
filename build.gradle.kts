plugins {
    // 8.9+ is the first AGP line that supports compileSdk 36 (Android 16).
    id("com.android.application") version "8.9.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// Regenerates the Graphify knowledge graph (graphify-out/). AST-only, no API key.
// Requires the `graphify` CLI on PATH: `uv tool install graphifyy` (or `pipx install graphifyy`).
tasks.register<Exec>("graphifyUpdate") {
    group = "documentation"
    description = "Regenerate the Graphify knowledge graph in graphify-out/"
    workingDir = rootDir
    commandLine(
        if (System.getProperty("os.name").startsWith("Windows")) "graphify.exe" else "graphify",
        "update", "."
    )
}
