# Agent Guide

## Project goal

Finish migrating Pressure Pipes Repressurized to Minecraft 1.20.1 on Forge, using the compatible BdLib release. Keep the mod ID `pressure` and display name `Pressure Pipes Repressurized`.

## Start here

- Read [docs/DEVELOPMENT_HANDOFF.md](docs/DEVELOPMENT_HANDOFF.md) for the verified status, test results, known gaps, build commands, and next steps.
- Read [README.md](README.md) for the project overview and player-facing behavior.
- Inspect `git status --short` before editing. The migration is currently uncommitted; preserve existing work and avoid destructive resets or checkouts.

## Project conventions

- The active implementation is under `src/main/java/`; `legacy-src/` is a reference copy of the old implementation and is not compiled.
- Runtime assets, block states, models, recipes, and loot tables live under `resources/`.
- Target Minecraft 1.20.1, Forge 47.x, and Java 17. BdLib is required at runtime; use the version recorded in `build.properties`.
- Keep `README.md` user-facing. Do not put development status, workflow, branch state, build/test results, or internal handoff notes there; put development documentation in `docs/*.md`.
- Update the relevant `docs/*.md` files when implementation or verification status changes. Update README only for player-facing installation, feature, and usage information.

## Implementation principles

- Fixes and feature work may include small, step-by-step overhauls of the affected systems. Keep each step reviewable and preserve the complete behavior while improving the design.
- Preserve feature parity with the legacy mod. Never omit or downgrade a feature as a migration shortcut. In particular, restore the tank GUI; a right-click text popup is not an acceptable replacement for it.
- When a legacy feature is difficult to port, implement a full equivalent for 1.20.1 and record real technical blockers. Do not silently replace it with a reduced interaction.

## Commits

- Include `Co-authored-by: Codex <codex@openai.com>` in every commit created for this project.

## Verification and completion

- Distinguish code that exists from behavior verified in game. Report the exact build or run command and its result; a successful `runClient` is not a full release build.
- For rendering changes, verify the affected block in a fresh client and check relevant states/facings. The pump has been visually checked; the rest of the models still need a broader pass.
- The migration is not complete yet. Resolve or explicitly defer the gaps listed in `docs/DEVELOPMENT_HANDOFF.md`, including the missing compatibility integrations and legacy tank GUI, and complete the required gameplay and packaging checks before calling the project finished.
