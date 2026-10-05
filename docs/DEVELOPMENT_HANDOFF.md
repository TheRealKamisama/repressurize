# Pressure Pipes Repressurized — Development Handoff

**Updated:** 2026-10-05 (Asia/Shanghai)

## Current status

All eight October 5 requirements **R1–R8 are implemented/diagnosed and verified within the requested scope**. The restored tank GUI and earlier seven fixes remain intact. The user explicitly requested that **all audio checks be deferred to their own personal testing**; no audible verification is claimed. No required code, integration, gameplay or packaging blocker remains for this pass. This is a tested development build, not a published release.

[MIGRATION_STATUS.md](MIGRATION_STATUS.md) is the authoritative checklist. [VERIFICATION_2026-10-05.md](VERIFICATION_2026-10-05.md) records current commands, versions, results, failed attempts and their resolutions. October 3 evidence remains historical in [VERIFICATION_2026-10-03.md](VERIFICATION_2026-10-03.md); it was not reused as acceptance of the new reports.

## October 5 results

| ID | Verified result |
|---|---|
| R1 | Explicit flow-placement opt-in reverses pump, pressure input/output, sluice, check valve, pipe sensor and Mekanism pressure gas machines while Shift is held. Other directional blocks and all-face tank modules retain normal placement. Six-direction placement/serialization/transfers, actual dedicated-client placement and saved restart pass. |
| R2 | Infinite Water Source fills buckets on the server. Survival/creative, both hands, singles/stacks and full inventories pass. A real survival client fills one bucket from three in its offhand; authoritative inventory and restart are checked. |
| R3 | Optional CC: Tweaked **1.120.2 Forge** implements the complete `pp_tank` / `pp_sensor` contract. Actual Lua tests all commands, errors, counters/averages, disassembly/reassembly and detach/reconnect. Both peers see authoritative six-output/filter mutations; saved computer/sensor restart passes. Base server starts without CC. |
| R4 | Connected indicator textures use the baked world-lighting/AO pipeline; the redundant opaque block-entity face is removed. Gauges use exposed-face lighting/directional shade with emission retained. Empty/partial/full single/multiple-height, exposed sides, daylight/artificial, AO settings and controller removal/reassembly pass. Both peers inspect post-restart surfaces/gauges. |
| R5 | All four gas recipes use installed `forge:alloys/advanced`, resolving to `mekanism:alloy_infused` in 10.4.16.80. Actual survival crafting consumes it; all four fresh JEI views are correct. Loading without Mekanism is clean. |
| R6 | Pump, input and their combined route each move **2,147,483,647 mB in one tick** with unrestricted finite source/sink handlers and conserve fluid. Explicit 4000 caps yield 4000. Saved configurations retain their values; startup logs the effective cap and path. |
| R7 | Borders start at `(43 + 21*i,42)`, icons at `(44 + 21*i,43)`; buttons/hover bounds align. Zero/two/six channels at scales 1–4, six edge clicks, six tooltips at scales 2/4, peer updates and saved modes pass. Full container/filter/gauge/inventory/shift-click functionality is preserved. |
| R8 | Ordinary `tank_fluid_output` actively pushes, as requested, through the existing automatic variant's `canOutput` gate. Legacy ordinary output was passive; this intentional extension is documented. Both variants, every face/mode, signal on/off, partial/full/no receiver, pull simulation/execution, conservation and counters pass. Working external-pull semantics are preserved. |

CC: Tweaked replaces OpenComputers as the required target. The old Scala integration and former dependency-blocker evidence remain references. Lua registry names are namespaced; the complete return/argument contract is in [COMPUTERCRAFT.md](COMPUTERCRAFT.md).

## Artifact and delivery

- Repository: `C:\Users\TheRe\Documents\ChatGPT\Repressurise`.
- Branch: `codex/pressure-pipes-1.20.1`. The user requested commit and push of this verified migration follow-up on October 5; this commit includes the implementation and evidence. No release was published. Initial Git status contained only untracked `forge-installer.jar.log`, which is preserved outside the commit.
- Final JAR: [pressure-2.0.0-DEV-mc1.20.1.jar](../build/libs/pressure-2.0.0-DEV-mc1.20.1.jar).
- SHA-256: `5A72E5C98ED9E3ADCB1726B3A013442FD7B11C00720604CAD2130B0B87D49CF3`.
- Server and clients used this exact tested hash. Later builds vary only the manifest timestamp; all 477 file-entry names and production class/asset/recipe/mod-metadata bytes match. The timestamp variant is archived and the tested candidate restored to `build/libs/`; see [artifact comparison](evidence/2026-10-05/artifact-content-comparison.json).
- `build/qa/pressure-qa.jar` is a separate test fixture. **Do not distribute it.** Main JAR has no QA classes/test structures.
- Active code is `src/main/java/`, assets are `resources/`; `legacy-src/` is uncompiled. The earlier snapshot `build/migration-evidence/baseline-2026-10-02.zip` and October 3 evidence remain preserved. This pass archives the original JAR as `build/migration-evidence/2026-10-05/baseline-pressure.jar`.
- Every future project commit must include `Co-authored-by: Codex <codex@openai.com>`.

## Tested environment and commands

Minecraft **1.20.1**, Forge **47.1.39**, Java **17.0.20.1** (Eclipse Adoptium), Gradle **8.5** / ForgeGradle **6**; BdLib **1.27.0.8-mc1.20.1**, Scalable Cats Force **3.3.0-build-2 with-library**. Optional profile: Mekanism **10.4.16.80**, JEI **15.62.0.217**, Jade **11.13.3+forge**, EnderIO **6.2.18-beta**, CC: Tweaked **1.120.2** (Modrinth Forge artifact ID `gRtcHJd8`, avoiding numeric resolution to Fabric).

From the repository root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Users\TheRe\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.20.1+1'
$env:GRADLE_USER_HOME = 'C:\Users\TheRe\.gradle'
$env:HTTP_PROXY = 'http://localhost:7897'
$env:HTTPS_PROXY = 'http://localhost:7897'
.\gradlew.bat --offline build qaJar
.\gradlew.bat --offline qaJar runGameTestServer -PcompatTest
node scripts/validate-assets.cjs
```

- Final full build: **passed, 51s**, including reobfuscation (`final-build.log`). Ordinary `test` is `NO-SOURCE`, not runtime acceptance.
- Source Forge suite: **31 required tests passed, 2m 41s** (`server-all-faces.log`).
- Normal installed Forge running the **production JAR**: **31 tests passed**, zero failures (`packaged-gametests-isolated.log`).
- Static assets: **73 models / 62 textures / 37 recipes**, zero issues.
- Fresh packaged client: **512 states / 35 items**, no missing Pressure sprites/translations; preserved models/items/screens/Jade/Nether effects and four JEI recipes inspected.
- Paired clients: actual vanilla item/menu packets, chunk unload/reload and new placement/bucket/computer synchronization pass.
- Dedicated restart: source, contents, six modes, filter, inventory, computer program and sensor counts/averages pass.
- Final renderer pass: **21 scenes per artifact**, GUI scales **1–4**, and read-only post-restart peer views/tooltips pass. Task clients and servers are shut down after verification.

For online resolution, also pass `-Dhttp.proxyHost=localhost -Dhttp.proxyPort=7897 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897`. All downloads use this proxy; offline commands need the populated cache. Detailed normal launch commands are in the verification record. `-PcompatTest` enables optional runtime dependencies and EnderIO's development refmap remapping; installed Forge needs no development remapping.

## Preservation and remaining limits

The prior canister authority/flowing-fluid normalization, dynamic gauges, pump/pipe UVs/transforms, seven textured models, tank ownership/limits/frames, full tank GUI, network traversal/invalidation, router priorities/filters, hand-pump durability/container/waterlogged behavior, drain/sluice distinction, sensors/accounting and Mekanism/JEI/Jade/EnderIO integrations retain passing regressions. New recipe tests fix a historical false positive where an unresolved ingredient was mistaken for an empty slot.

Existing `run/config/pressure-common.toml` still has `fluidTransferPerTick = 4000`. To choose unrestricted automatic extraction, explicitly set `pressure.fluidTransferPerTick = 2147483647`. Saved values are not overwritten; endpoint capabilities can impose real limits. The separately deployed user artifact/config was not identified by the screenshots.

Audio is user-owned, deferred and unverified. EnderIO can log missing optional Athena capacitor-bank models when Athena is absent; Pressure's models and EnderIO fluid-conduit integration pass. Forge's monitor failure was resolved only in isolated QA profiles using `earlyWindowControl=false`; invalid 1×1 images were rejected/replaced. Idle clients were cleaned up and the completed-client shutdown guard corrected. No unresolved Pressure implementation blocker remains.
