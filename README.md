# glyph-playground

Glyph Playground is an Android drawing/design tool for the Nothing Phone (3)
Glyph Matrix. It renders the 25×25 Glyph Matrix on-screen, lets you toggle
LEDs by tapping or dragging, save/load named patterns, and display them on
the physical Glyph Matrix via the official Nothing Glyph Matrix SDK.

See [AGENTS.md](AGENTS.md) for the full project guide (architecture, CI,
ground rules).

## Building

```sh
gradle assembleRelease
```

## Graphify

This repository is integrated with
[Graphify](https://github.com/Graphify-Labs/graphify), a tool that turns the
codebase into a queryable **knowledge graph** using local, deterministic
tree-sitter AST parsing. The graph lives in `graphify-out/` and helps AI
coding agents (and humans) trace dependencies, find core abstractions, and
answer "what connects X to Y?" questions without repeated full-repo searches.
Agent-facing usage rules are in the `graphify` section of
[AGENTS.md](AGENTS.md).

Generated files:

- `graphify-out/graph.json` — the queryable graph (nodes, edges, communities)
- `graphify-out/GRAPH_REPORT.md` — summary: hubs, god nodes, suggested queries
- `graphify-out/graph.html` — interactive force-directed visualization (open in a browser)

### Prerequisites

- **Python 3.10–3.13**
- **[uv](https://docs.astral.sh/uv/)** (recommended) or
  [pipx](https://pipx.pypa.io/)

No LLM API key is required — code extraction is fully local (AST-only).

### Installing dependencies

Cross-platform (Windows, macOS, Linux):

```sh
uv tool install graphifyy   # note: package is "graphifyy", CLI is "graphify"
# or: pipx install graphifyy
```

If `graphify` is not found afterwards, run `uv tool update-shell`
(or `pipx ensurepath`) and open a new terminal.

### Generating the graph

The initial graph is committed, so this is only needed from scratch:

```sh
graphify extract . --code-only
graphify cluster-only . --no-label
```

### Updating / regenerating the graph

After code changes, a single command re-extracts changed files and updates
the graph (no API cost):

```sh
graphify update .
```

or, via Gradle (same thing, matches the project's existing tooling):

```sh
gradle graphifyUpdate
```

Optional: `graphify hook install` installs git hooks that rebuild the graph
automatically after each commit/checkout.

### Querying the graph

```sh
graphify query "how are patterns saved and loaded?"
graphify path "MainActivity" "GlyphController"
graphify explain "PatternRepository"
graphify god-nodes
```

Configuration: `.graphifyignore` (gitignore syntax) controls what gets
indexed; `graphify-out/cache/` and `graphify-out/.graphify_root` are
machine-specific and gitignored.
