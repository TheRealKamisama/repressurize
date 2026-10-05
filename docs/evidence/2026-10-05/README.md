# October 5 migration evidence

## Current implementation and acceptance

All R1–R8 acceptance checks pass in the recorded October 5 scope. [VERIFICATION_2026-10-05.md](../../VERIFICATION_2026-10-05.md) contains the exact commands, versions, investigation and boundaries. Audio is explicitly deferred to the user's personal check.

| Evidence | Scope |
|---|---|
| [before/](before/) / [after/](after/) | 21 matched indicator scenes: empty/partial/full, single/multiple-height, exposed sides, daytime/artificial lighting, AO and controller removal/reassembly. Zero/two/six outputs at GUI scales 1–4; candidate captures six clicked controls. Client logs included. |
| [indicator-before-after.png](indicator-before-after.png) | Cropped comparison of the measured connected-face darkening and world-lit fix; normal directional shading is retained. |
| [packaged/](packaged/) | Valid 1280×960 final-package galleries/screens, canister levels, Jade, Nether smoke/consumption and four populated JEI gas recipes. |
| [network/](network/) | Two-client state reports, actual chunk reload, saved dedicated restart, post-restart indicator/GUI views from both peers, and all six output tooltips at scales 2/4. |
| [server-all-faces.log](server-all-faces.log) | 31 source Forge tests passed, including measured integer-boundary transfer and full actual CC Lua contract. |
| [packaged-gametests-isolated.log](packaged-gametests-isolated.log) | 31 tests passed against the reobfuscated production JAR in a normal installed Forge server. Result JSON is in `network/`. |
| [packaged-base-server.log](packaged-base-server.log) | Final tested JAR starts with optional mods absent, preserving the saved 4000 cap; no Pressure optional-class/content errors. |
| [packaged-network-restart-forced.log](packaged-network-restart-forced.log) | Saved source, contents, inventory, facing, six modes/filter and actual CC computer/sensor restart pass. Temporary QA chunk tickets are released. |
| [final-build.log](final-build.log) | Full build/reobfuscation passed in 51s. |
| [mekanism-alloy-tag.json](mekanism-alloy-tag.json) | Actual installed 10.4.16.80 tag content; obsolete Mekanism tag is absent. |
| [artifact-content-comparison.json](artifact-content-comparison.json) | Later build differs only in manifest timestamp; all 477 entry names and production class/resource/recipe/mod-metadata bytes match the tested delivery candidate. |

Delivery SHA-256: `5A72E5C98ED9E3ADCB1726B3A013442FD7B11C00720604CAD2130B0B87D49CF3`. Only the production JAR is distributable; the separate QA helper is excluded. All task clients/servers are shut down after checks. Original images below are preserved unchanged and do not themselves establish acceptance.

## Original user attachments

These three attachments were opened and inspected on 2026-10-05 and copied unchanged from the user's temporary attachment paths into this directory. They support the reports in [MIGRATION_STATUS.md](../../MIGRATION_STATUS.md), R4/R5/R7. No fresh-client run, code fix or runtime acceptance is established by inspecting them. The producing JAR, mod versions, world settings and resource/shader configuration have not been identified.

| User image | Preserved attachment | Observation |
|---|---|---|
| 1 | [tank-indicator-connected-darkening.png](tank-indicator-connected-darkening.png) | Assembled indicator column shows darker vertical regions than neighboring tank surfaces; the user reports standalone placement looks correct. Ambient light/AO is a suggested cause, not a diagnosis. |
| 2 | [mekanism-gas-recipe-empty-tag.png](mekanism-gas-recipe-empty-tag.png) | JEI crafting view displays `Empty Tag: mekanism:alloys/advanced` over a gas-related block recipe. All four affected recipe files must be investigated and crafted. |
| 3 | [tank-output-gui-misalignment.png](tank-output-gui-misalignment.png) | Tank output outlines/icons and mode-button placement need alignment review against the background, including hit regions and other GUI scales. |

Original temporary attachment names, in user image order:

1. `codex-clipboard-1b5ee0cb-f55c-40b2-86e1-1cf60dca59ae.png`
2. `codex-clipboard-f125340e-7fa1-434a-93ea-f251d5324172.png`
3. `codex-clipboard-d977440f-82c1-44dd-b611-ad5ffb6751c6.png`

Each copied file's SHA-256 was compared with its original and matched. The remaining five requirements are textual user reports/features; no additional screenshot is implied.

Documentation revision checks: `git diff --check` exited 0 (existing LF/CRLF warnings only; untracked documentation is outside that command's scope). A PowerShell link-target check using `[regex]::Matches` and `Test-Path -LiteralPath` resolved all 28 local Markdown links across the status, handoff and this evidence index. `Get-FileHash -Algorithm SHA256` matched all three copies against the original attachments. No Gradle build or gameplay/rendering check was run for this documentation-only revision.
