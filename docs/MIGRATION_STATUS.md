# Pressure Pipes Repressurized — Migration Status

**Updated:** 2026-10-05 (Asia/Shanghai)  
**Status:** Migration incomplete. Eight new requirements are open, including regressions that reopen earlier acceptance checks. CC: Tweaked replaces OpenComputers as the required computer integration; audible feedback remains unverified.  
**Purpose:** A concrete goal and acceptance checklist for the next migration session.

**Evidence:** [VERIFICATION_2026-10-03.md](VERIFICATION_2026-10-03.md) records the previous server suite, packaged-client/server checks, two-client synchronization and restart results. Those results remain historical evidence, not acceptance of the October 5 reports. The former OpenComputers runtime blocker is superseded by the user's CC: Tweaked requirement. The three new screenshots are preserved in [evidence/2026-10-05/](evidence/2026-10-05/README.md). No implementation, build or new runtime verification was performed for this documentation revision.

## Goal for the receiving session

Continue migrating Pressure Pipes Repressurized to **Minecraft 1.20.1 / Forge 47.x / Java 17**, using the compatible BdLib version in `build.properties`. Keep mod ID `pressure` and display name `Pressure Pipes Repressurized`. Preserve the original seven fixes and full legacy tank GUI, implement all eight October 5 requirements below, and complete the remaining feature-parity and verification work listed here. Target **CC: Tweaked instead of OpenComputers**, retaining equivalent tank and pipe-sensor computer functionality. The existing goal references this document as its task specification and completion checklist; this revision extends that linked scope without replacing its preservation, implementation or validation requirements.

**Minecraft 1.12.2 is the correctness reference for appearance and behavior.** Use the supplied 1.12.2 screenshots and `legacy-src/` together; implement full equivalents through the 1.20.1 APIs. Preserve features during small, reviewable changes. A right-click status message does not satisfy the tank GUI requirement. Any genuine integration blocker or deferred work must be documented explicitly; an open or deferred item is not completed parity.

This document combines the original seven reports and ten screenshots, the eight October 5 requirements and three new screenshots, the current source/resources, and [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md). The October 5 revision supersedes earlier acceptance claims for the affected behavior. Preserve the earlier evidence and working fixes while investigating the new reports.

## October 5 revision: eight new requirements

IDs R1–R8 follow the user's new numbering and are separate from the original issues 1–7. Every new item starts **open**. A source observation or inspected screenshot does not establish a confirmed cause or verified fix. Use **implemented but unverified** after code changes, **verified** only after the acceptance checks pass, and **blocked** only with a concrete technical blocker and recorded investigation.

| ID | Requirement | Current status / evidence |
|---|---|---|
| R1 | Shift placement reverses facing only for blocks with input/output sides | **Open — requested feature.** Includes High Pressure Pump and High Pressure Input/Output; unrelated directional blocks must retain their placement behavior. |
| R2 | Right-click Infinite Water Source with an empty bucket to fill it | **Open — requested feature.** Validate server-authoritative bucket/inventory updates. |
| R3 | Replace OpenComputers integration target with CC: Tweaked | **Open — implementation required.** The old dependency blocker no longer blocks the chosen integration target; select and test an actual Forge 1.20.1 CC: Tweaked version/API. |
| R4 | Connected Tank Indicator appears darker; standalone placement looks correct | **Open — user reported and screenshot inspected.** [Image 1](evidence/2026-10-05/tank-indicator-connected-darkening.png). Ambient light/AO is a hypothesis. |
| R5 | Mekanism gas-block recipes contain an empty alloy tag | **Open — user reported and screenshot inspected.** [Image 2](evidence/2026-10-05/mekanism-gas-recipe-empty-tag.png) shows `Empty Tag: mekanism:alloys/advanced`. |
| R6 | High Pressure Pump and Input should have near-infinite throughput | **Open — user reported.** Effective throughput seems capped; the cap's cause has not been established. |
| R7 | Tank output GUI widgets are slightly misaligned | **Open — user reported and screenshot inspected.** [Image 3](evidence/2026-10-05/tank-output-gui-misalignment.png). |
| R8 | Tank Fluid Output must actively push into an adjacent fluid handler | **Open — user reported legacy behavior.** Existing redstone gating when a pipe pulls is user-confirmed working and must be preserved. |

### R1. Shift placement reverses input/output orientation

**Required result:** Holding Shift while placing an eligible block selects the exact opposite of its normal placement facing. Restrict this to blocks with input/output sides, including `pressure:pump`, `pressure:input` and `pressure:output`. Audit other eligible fluid/gas machines and modules by their actual side semantics; do not make every block with a facing property reverse automatically.

**Starting points:** `DirectionalMachineBlock.getStateForPlacement`, specialized placement methods and `TankModuleBlock`. The shared directional base currently chooses the opposite of the nearest looking direction without a Shift branch. Establish an explicit eligibility rule before changing a shared base.

**Acceptance checks:** Compare normal/Shift placement in all six directions for each eligible block, including the stated pump/input/output examples. Confirm actual input/output faces, textures and transfers match the reversed state on client and server and persist after reload. Regression-check excluded directional blocks, configurator rotation and normal placement.

### R2. Fill a bucket from Infinite Water Source

**Required result:** Right-clicking `pressure:water_source` with an empty bucket gives a water bucket without depleting the infinite source, placing water in the world or duplicating items.

**Starting points:** `SourceBlock`, `WaterSourceBlockEntity` and Forge's fluid-container interaction helpers. A fluid capability alone does not prove that block right-click interaction is implemented.

**Acceptance checks:** Survival and creative, main hand/offhand, a single bucket and stacked empty buckets, full inventory, repeated filling and dedicated-client synchronization. Preserve existing pipe/canister extraction and verify non-bucket interactions are not incorrectly consumed.

### R3. CC: Tweaked computer integration replaces OpenComputers

**Required result:** Implement optional Forge 1.20.1 CC: Tweaked peripherals with equivalents of the complete legacy computer command contract. OpenComputers is no longer a required target; retain its legacy source and historical blocker evidence as references.

**Contract:** Tank data port (`pp_tank`): `isConnected`, `getCapacity`, `hasFluid`, `getFluid`, `getOutputs`, `setOutputMode`, `getFilter`, `setFilter`, `getCountsIn`, `getCountsOut`. Pipe sensor (`pp_sensor`): `getAverages`, `getCounts`. Preserve the meaningful return data, mutation rules and error handling through the CC: Tweaked/Lua API; document any necessary representation changes.

**Starting points:** `legacy-src/net/bdew/pressure/compat/opencomputers/`, `blocks/tank/blocks/DataPort.scala`, `blocks/valves/sensor/TilePipeSensor.scala`, and the active tank output/filter and flow-accounting APIs. Pin a compatible CC: Tweaked dependency and use its supported peripheral registration/threading APIs. Its availability/version and behavior have not been verified in this revision.

**Acceptance checks:** In an actual CC: Tweaked computer, attach/discover both peripherals and exercise every command, disconnected/reassembled tanks, valid/invalid filters and output modes, counts/averages during transfer, detach/reconnect and saved-world restart. Check mutations against authoritative tank state and another client's GUI. Verify ordinary client/server startup with CC: Tweaked absent; ensure optional classes do not become mandatory runtime dependencies.

### R4. Connected Tank Indicator lighting

**Required result:** Connected indicator surfaces and the dynamic gauge receive correct world lighting and match neighboring tank surfaces and the standalone indicator under comparable conditions. Preserve controller-aware frames, continuous indicator columns and fluid display.

**Starting points:** `client/TankFrameModel.java`, `client/PressureBlockEntityRenderer.java` and the indicator models. Investigate AO, face shading/normals, packed light, occlusion and any overlapping dynamic geometry; the screenshot does not prove which of these causes the darkening.

**Acceptance checks:** Fresh-client comparisons of standalone/assembled indicators in empty, partial and full tanks; all exposed faces, single/multiple-height columns, daylight and artificial lighting, AO settings, controller removal/reassembly, chunk reload and a second client. Capture comparable before/after views; retain normal directional shading rather than forcing all faces to full brightness.

### R5. Mekanism gas recipes: empty advanced-alloy tag

**Required result:** Every gas-related Pressure recipe has a populated, correct alloy ingredient and can be crafted with the supported installed Mekanism version. Keep optional-mod conditions and startup without Mekanism intact.

**Starting points:** `resources/data/pressure/recipes/gas_input.json`, `gas_output.json`, `tank_gas_input.json`, `tank_gas_output.json` all reference `mekanism:alloys/advanced`. Inspect actual Mekanism 1.20.1 tag data before choosing a replacement; a plausible alternative tag name is not proof. Reassess the October 3 recipe checks against the displayed JEI ingredient and the deployed dependency profile.

**Acceptance checks:** Fresh JEI display for all four recipes with no empty-tag ingredient, actual survival crafting with the intended alloy, server recipe loading/data reload, and packaged-client/server checks both with and without Mekanism. Record the exact supported Mekanism version and resolved tag contents.

### R6. Near-infinite High Pressure Pump/Input throughput

**Required result:** Pump and High Pressure Input have effectively unrestricted transfer, as requested and consistent with the legacy pressure system, subject to real source/sink availability and API integer bounds. Preserve fluid conservation, simulation, filters and redstone behavior.

**Starting points:** `PumpBlockEntity`, `InputBlockEntity`, `FluidTransfer`, pressure traversal and `PressureConfig.FLUID_TRANSFER_PER_TICK`. The source default is already `Integer.MAX_VALUE`, and active extraction uses that setting. Inspect effective saved server/world configuration, source/sink capability limits and intermediate handlers; an old configuration cap is only a possible explanation. Do not silently overwrite user configuration or assume the default proves runtime throughput.

**Acceptance checks:** Measure amount per tick with deliberately high-capacity unrestricted source/sink handlers through both machines separately and together, at volumes well above 4,000 mB/t. Record effective configuration and distinguish endpoint limits from mod-imposed limits. Check integer-boundary behavior, no overflow/loss/duplication, blocked/filtered cases, redstone, reload and dedicated-server behavior. Record how any retained explicit throughput setting interacts with the requested default.

### R7. Align the tank output GUI

**Required result:** All six output outlines, icons/disabled markers, redstone buttons, tooltips and clickable regions align with the intended tank background at every supported GUI scale. Preserve the full tank GUI and existing settings/synchronization.

**Starting points:** `client/TankScreen.java`, the tank background texture and legacy `blocks/tank/gui/GuiTank.scala` / `TankOutputWidget.scala`. Compare the widget coordinates and dimensions with the texture; do not infer a complete fix from one offset adjustment.

**Acceptance checks:** Fresh-client screenshots at multiple GUI scales with zero, partial and six configured outputs; inspect borders, icons, button states, hovering and hit regions. Click each redstone control and confirm server/second-client updates and persistence. Regression-check gauge, container/filter slots, shift-click and inventory alignment.

### R8. Restore active tank fluid output; preserve redstone control

**Required result:** The reported Tank Fluid Output actively pushes fluid into a receiving handler without requiring a pulling pipe. Retain the existing output selection, filtering, flow accounting and redstone rules. The user confirms those redstone rules already work when a pipe pulls; changing their semantics is outside this fix.

**Investigation detail:** Active `TankModuleBlockEntity.serverTick` handles `tank_fluid_output_auto` but excludes `tank_fluid_output`. The reference source distinguishes the ordinary output (`FluidOutput.scala`, empty `doOutput`) from the actively pumping output (`FluidAutoOutput.scala`, `FluidHelper.pushFluid`); both use the legacy `FluidOutput` module category. Reconcile the exact registry ID/variant and the reported 1.12.2 interaction before treating the excluded ordinary variant as a proven regression cause. Preserve the user's requested active-pumping result and document any variant distinction explicitly.

**Acceptance checks:** Connect the reported output variant directly to a passive receiving tank/handler and verify automatic transfer without a pulling pipe. Cover all configured output faces and redstone modes with signal on/off; also repeat the existing external-pull scenario unchanged. Check simulation, partial/full/blocked receivers, no-target cases, conservation, output counters, saved configuration/reload and dedicated-client synchronization. Regression-check the automatic-output variant and pressure output.

## Evidence and verification boundaries

### Historical implementation pass (2026-10-03)

The original issue descriptions below remain the acceptance specification. This table records the October 3 verified scope; R1–R8 above are the current additional acceptance requirements. No overall migration-complete claim is made.

| Item | Current status | Evidence / outstanding checks |
|---|---|---|
| Canister world placement (7) | Implemented; broad runtime checks passed | Water/lava source persistence, repeated use, occupied/partial targets, both height bounds, border, offhand, creative/Nether consumption, NBT, and old flowing-fluid variants pass. Packaged two-client placement, actual chunk reload and dedicated restart passed on the final JAR. Nether smoke is captured on a real client; audible output is unavailable. |
| Seven missing textures (4) | Implemented; render checks passed | All seven have textured inventory and placed forms. Separate fresh-client galleries cover all six facings, unpowered/powered states; legacy valve/sensor shape and arms are restored. The packaged client checked all 512 registered states and 35 items without missing baked sprites. |
| Pipe ring/item and pump icon (1–3) | Implemented; render/gameplay checks passed | XZ/XY/YZ rings, small cubic inventory/held models in both hands and third person, dropped pipe, pump inventory UVs and facing galleries were inspected. Server tests cover ring flow, break/reconnect and multi-face router endpoints. |
| Canister gauge (6) | Implemented; render checks passed | Empty/partial/full water and lava plus a Mekanism fluid render independently. The real Nether interaction updates the held canister to 1500 mB. Dedicated synchronization and restart have passed; the final chunk cycle also passed. |
| Tank membership and frames (5) | Historical checks passed; indicator lighting reopened under R4 | Ownership, limits, persistence and connected perimeter checks remain recorded. They do not close the newly reported connected-indicator darkening. |
| Full tank GUI | Historical menu checks passed; output alignment reopened under R7 | Gauge, containers, filter, six outputs, settings/conservation and client synchronization were checked. R7 requires additional visual and interaction acceptance. |
| Other parity / integrations | Historical implementation/checks; R3/R5/R6/R8 open | Previous machine/flow/integration results remain recorded. CC: Tweaked is required; Mekanism recipes, pump/input throughput and active tank output require renewed checks. The former OpenComputers blocker is historical. |
| Packaging | Historical package verified; revised final candidate required | The October 3 reobfuscated package passed the recorded client/server profile; SHA-256 `AD22061E73032F48CA8126D4905A150EF785FC659A301E21547E077969358139`. Rebuild and validate the eventual R1–R8 implementation before claiming final packaging acceptance. |

October 3 checks: `gradlew.bat --offline build qaJar` passed (55s); `gradlew.bat --offline runGameTestServer -PcompatTest` passed 23 tests (2m 16s); `node scripts/validate-assets.cjs` passed 73 models / 62 textures / 37 recipe files. The packaged optional-mod client checked 512 states / 35 items and produced inspected captures. Two normal packaged clients passed placement/menu synchronization, actual chunk unload/reload and restored rendering; the dedicated restart preserved world/player state. Commands, logs, images, exact artifact hash, former OpenComputers blocker and audio limits are in the historical verification report. The preparation notes below are retained as historical context.

- **Screenshot inspected:** Appearance was reviewed from the supplied image; this does not establish a new runtime test of the current source or JAR.
- **User reported:** The user observed the behavior in 1.20.1. Keep the report open even where there is no corresponding screenshot.
- **Source inspected:** Code or assets exist, or a concrete implementation gap was found. A suggested cause still needs runtime validation.
- **Historical verification:** A command/result or user check is recorded in the development handoff; it was not rerun while preparing this document.
- **Verified fixed:** Use this status only after implementing the change and recording the required fresh-client/gameplay evidence.

The original 2026-09-30 preparation pass inspected documentation, source/resources, Git state, screenshots and the then-existing artifact. The implementation and runtime results above supersede that preparation-only boundary.

## Historical workspace and build baseline (2026-09-30)

| Item | Current baseline |
|---|---|
| Repository | `C:\Users\TheRe\Documents\ChatGPT\Repressurise` |
| Reference workspace | `C:\Users\TheRe\Documents\ChatGPT\Repressurise-Workspace` |
| Branch / HEAD | `codex/pressure-pipes-1.20.1` / `2f4678674aa827c8a0057cfb56ca8bb83c8c764f` |
| Working tree | Extensive uncommitted migration work; inspected with `git status --short` before documentation edits. Preserve it. |
| Active implementation | `src/main/java/`; runtime assets, recipes and loot tables under `resources/` |
| Legacy reference | `legacy-src/` contains the old Scala implementation and is not compiled |
| Pinned dependencies | Minecraft `1.20.1`, Forge `47.1.39`, BdLib `1.27.0.8-mc1.20.1`, Scalable Cats Force `3.3.0-build-2` |
| Toolchain | Java 17, ForgeGradle 6, Gradle wrapper 8.5, official mappings |
| Existing QA world | `run/saves/New World` |

Read [AGENTS.md](../AGENTS.md), [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md), and [README.md](../README.md) before edits. Keep development status in `docs/*.md`; update README only for player-facing changes. Include `Co-authored-by: Codex <codex@openai.com>` in every project commit.

### Existing implementation and previous verification

The active implementation includes Forge registrations/configuration, pressure capabilities and graph traversal, fluid machines, router menu/screen, tanks/modules, sensors, canisters, the hand pump, configurator and debugger. Resource files, recipes, loot tables and translations are present. Presence does not establish complete behavior or visual parity.

| Check | Recorded result | Remaining limitation |
|---|---|---|
| `compileJava` / `processResources` | Passed in preceding migration/client iterations; up to date in the latest full build | Compilation/resource copying does not validate model baking or gameplay |
| `.\gradlew.bat runClient -PrunDirectory=run` | Handoff records a successful Forge 1.20.1 / 47.1.39 launch and `BUILD SUCCESSFUL` | Smoke test; broad rendering and gameplay checks remain |
| Placed High Pressure Pump | Earlier user check confirmed correct placed orientation after rebuilding/relaunching | Preserve this fix; inventory orientation remains open under issue 3 |
| Hand pump registration | Current source preserves `durability(256)` and calls `super(properties)` without adding `stacksTo(1)` | Keep the prior registration-crash fix intact; this is a source check, not a new gameplay check |
| `.\gradlew.bat build` | Handoff records `BUILD SUCCESSFUL` on 2026-09-30, including `jar` and `reobfJar`, in 45 seconds | Test artifact only; no full release-readiness claim |
| Gradle `test` | Historical result `NO-SOURCE`; current `src/test` directories contain no test files | No automated gameplay coverage |
| Static asset references / logs | Handoff records existing referenced files and no common missing-model/texture log matches | Neither result rules out checkerboards, unresolved texture slots, wrong UVs, or missing dynamic rendering |

Historical artifact (superseded by the current build above): [`build/libs/pressure-2.0.0-DEV-mc1.20.1.jar`](../build/libs/pressure-2.0.0-DEV-mc1.20.1.jar). Its SHA-256 was checked during this preparation pass and matched the then-current handoff:

```text
2CF23AAB2F6FF0B24CD801088A442E359E356C7D61F7766B9A360F479E57DFCE
```

That historical hash identifies the preparation snapshot; it does not identify the current JAR or prove which artifact produced the original screenshots. The current hash is recorded above. A normal client needs the mod JAR, BdLib and Scalable Cats Force in `mods/`. The handoff reports uncommitted/unpushed work, no upstream, and no PR or published release; recheck publication state before any later delivery.

## Screenshot index

All ten images were opened and reviewed. Links resolve to the sibling reference workspace, which must be retained with this checkout. No 1.12.2 pipe-ring screenshot, filled-canister 1.20.1 screenshot, or fluid-placement sequence is present in the supplied set.

| ID | Version / screenshot | Evidence from the image |
|---|---|---|
| N1 | 1.20.1 — [2026-09-30_16.45.05.png](../../Repressurise-Workspace/screenshots/1.20.1/2026-09-30_16.45.05.png) | Pipe ring has two conspicuous upright/malformed sections; first-person held pipe is oversized; hotbar pipe appears as a flat face |
| N2 | 1.20.1 — [2026-09-30_16.47.22.png](../../Repressurise-Workspace/screenshots/1.20.1/2026-09-30_16.47.22.png) | Third-person held pipe is much larger than the legacy reference; malformed ring sections remain visible behind the player |
| N3 | 1.20.1 — [20260930164149.png](../../Repressurise-Workspace/screenshots/1.20.1/20260930164149.png) | Close-up identifies the High Pressure Pump inventory icon for orientation comparison |
| N4 | 1.20.1 — [20260930164212.png](../../Repressurise-Workspace/screenshots/1.20.1/20260930164212.png) | Creative inventory contains seven purple/black checkerboard block icons; other icons render, including the pump and a plain canister |
| N5 | 1.20.1 — [20260930164255.png](../../Repressurise-Workspace/screenshots/1.20.1/20260930164255.png) | Placed tank/module faces lack the dark surrounding frames shown in 1.12.2; several tank inventory icons also lack frames |
| L1 | 1.12.2 — [2026-09-30_16.54.49.png](../../Repressurise-Workspace/screenshots/1.12.2/2026-09-30_16.54.49.png) | Unassembled tank/module examples have clear dark frames around individual faces; matching held/hotbar appearance is visible |
| L2 | 1.12.2 — [2026-09-30_16.54.59.png](../../Repressurise-Workspace/screenshots/1.12.2/2026-09-30_16.54.59.png) | Held pipe is a small cube in third person and has a cube-shaped hotbar icon |
| L3 | 1.12.2 — [2026-09-30_16.55.27.png](../../Repressurise-Workspace/screenshots/1.12.2/2026-09-30_16.55.27.png) | Creative inventory reference for machine/pump icons and framed tank/module icons |
| L4 | 1.12.2 — [2026-09-30_16.55.33.png](../../Repressurise-Workspace/screenshots/1.12.2/2026-09-30_16.55.33.png) | Adding the controller forms a connected tank surface: internal frame seams disappear while the outside perimeter remains framed |
| L5 | 1.12.2 — [2026-09-30_16.55.53.png](../../Repressurise-Workspace/screenshots/1.12.2/2026-09-30_16.55.53.png) | Filled water and lava canisters display distinct blue/orange fluid gauges beside the canister image |

The named identities of the seven broken block icons and the corresponding placed-block failures come from the user's report, supported by N4. The supplied screenshots do not show a separate placed view of each of those seven blocks. L3 is the closest supplied inventory reference for the pump; use the legacy model/UV mapping to confirm the exact correction.

## Seven reported regressions

Numbering follows the original report. The issue descriptions and investigation leads below are the original baseline; the current status table supersedes old implementation-presence observations.

### 1. High Pressure Pipe connections break in a ring

**Evidence:** User report; N1 and N2. Two sections of the ring render incorrectly. The visible defect includes geometry/orientation as well as the reported connected-texture appearance, so inspect the connection models and rotations together.

**Required result:** A closed ring connects continuously with the same pipe appearance and proportions as 1.12.2. No direction produces an upright slab, gap, wrong face, or disconnected-looking segment.

**Starting points:** `src/main/java/net/bdew/pressure/blocks/PipeBlock.java`; `resources/assets/pressure/blockstates/pipe.json`; block models `pipe_center.json`, `pipe_section.json`, and `pipe_straight.json`; legacy `blocks/pipe/BlockPipe.scala`. The current straight model is built along the Y axis, while the `straight=x` variant only applies a Y rotation. Trace this rotation against the intended axis before assuming the PNG itself is wrong. This is a source observation, not a runtime-confirmed cause of every malformed section.

**Acceptance checks:** Recreate the screenshot ring; test rings in the XZ, XY and YZ planes, all straight axes, elbows and branch junctions. Break/reconnect segments, change placement order and reload the world. Verify continuous geometry/UVs and pressure-network connectivity.

### 2. High Pressure Pipe held model is too large

**Evidence:** User report; compare N1/N2 with L2. The 1.20.1 pipe occupies too much of the hand view; its inventory/hotbar appearance also looks flat compared with the legacy cube.

**Required result:** Restore the legacy held size and cube presentation while retaining the correct placed-pipe dimensions.

**Starting points:** `resources/assets/pressure/models/item/pipe.json` currently only inherits `pressure:block/pipe_center`; that block model has no item display transforms. Review the 1.12.2 item presentation and supply appropriate 1.20.1 GUI and hand transforms.

**Acceptance checks:** Compare inventory/hotbar and first-/third-person views, both hands, against L2 at comparable camera settings. Check dropped-item presentation after changing shared transforms. Ensure issue 1's placed geometry remains correct.

### 3. High Pressure Pump inventory icon has incorrect orientation

**Evidence:** Latest user report; N3/N4 compared with L3. The previous handoff says the icon rendered normally; that only establishes an earlier textured icon check. It does not close this orientation regression.

**Required result:** Match the legacy inventory face/UV orientation while preserving the already corrected placed pump.

**Starting points:** `resources/assets/pressure/models/item/pump.json` uses `minecraft:block/cube`, while `models/block/pump_off.json` and `pump_on.json` use `pressure:block/rotated`. The shared rotated model applies side UV turns: down 180°, west 270°, east 90°. Review the item mapping and display transform independently of block-state facing rotations. Preserve `DirectionalMachineBlock.getRenderShape()` returning `RenderShape.MODEL`.

**Acceptance checks:** Compare the pump inventory/hotbar icon with the 1.12.2 reference. Then regression-check the placed pump in all six facings, powered and unpowered, including the side-face arrows/UVs. Record inventory and placed checks separately.

### 4. Seven blocks have missing textures in inventory and world

**Evidence:** User report for both contexts; N4 visibly contains seven missing-texture icons. These are purple/black checkerboards, matching the missing-model/texture symptom previously encountered with the pump.

| Affected block | Registry ID | Initial block model(s) to trace from its item and block-state JSON |
|---|---|---|
| High Pressure Input | `pressure:input` | `input_off.json`, `input_on.json` |
| High Pressure Output | `pressure:output` | `output.json` |
| Fluid Drain | `pressure:drain` | `drain.json` |
| Sluice | `pressure:sluice` | `sluice.json` |
| High Pressure Check Valve | `pressure:check_valve` | `valve_off.json`, `valve_on.json`; compare legacy valve geometry |
| High Pressure Pipe Sensor | `pressure:pipe_sensor` | `pipe_sensor_off.json`, `pipe_sensor_on.json` |
| Tank Controller | `pressure:tank_controller` | `tank_controller.json` |

**Required result:** All seven render the proper 1.12.2-equivalent textures, geometry and face orientation in both inventory and world.

**Starting points:** Inspect matching files under `resources/assets/pressure/blockstates/`, `models/item/` and `models/block/`, plus their Java render shapes. Several affected block models put literal `pressure:block/...` paths directly in element face `texture` fields, whereas the corrected pump uses named texture slots such as `#side` through its parent. Investigate texture-slot resolution as a shared failure point. Also verify actual state variants, inherited slots, texture-atlas references and singular `block`/`item` paths. These observations identify investigation targets; they do not replace a fresh-client check.

**Acceptance checks:** Check each named block separately in creative/player inventory, held form and placed form; cover every supported facing and active/powered state. Preserve legacy shapes instead of replacing specialized blocks with generic cubes. Capture before/after images and inspect resource-load logs. No checkerboards or invisible fallback blocks may remain.

### 5. Tank blocks/modules lack frames and connected frame rendering

**Evidence:** User report; N5 compared with L1 and L4. In 1.12.2, separate blocks show individual frames; once connected to a controller, the tank's outer frame remains while internal seams merge. The 1.20.1 examples omit the frames entirely.

**Required result:** Restore framed inventory/unassembled appearance and controller-aware connected frames across the tank family. A permanently painted border on every block would not reproduce the assembled appearance.

**Starting points:** Current models such as `tank_block.json` and `tank_fluid_input.json` use ordinary cube models with a main texture and no connected-frame layer. The legacy implementation uses `model/PressureMultiblockEnhancer.scala` and the multiblock cases in `model/ExtendedModelLoader.scala`. The frame asset `resources/assets/pressure/textures/block/connected/edge.png` exists. Port the full equivalent, including client updates when membership changes; audit the controller, tank blocks, indicator, sensor and all fluid/pressure/access/filter/interface/data modules.

**Acceptance checks:** Recreate L1's unassembled wall and L4's assembled wall. Check all exposed faces, outside edges/corners and internal seams, inventory frames, controller addition/removal, module changes, and adjacent independent tanks. Check chunk/world reload and another multiplayer client so stale membership/frame rendering is caught.

### 6. Canister inventory icons do not show their contents

**Evidence:** User report; L5 directly shows the expected blue water and orange lava gauges. N4 shows the generic canister icon, but no filled 1.20.1 canister comparison is supplied; reproduce that case before closing the issue.

**Required result:** Inventory icons identify the stored fluid and show a proportional fill level like 1.12.2, including a sensible empty state.

**Starting points:** `resources/assets/pressure/models/item/canister.json` is a static generated item with only `layer0`. The current client setup registers the router screen and has no equivalent of legacy `model/CanisterModelEnhancer.scala`. That enhancer adds a GUI fluid gauge using the fluid's still texture and amount/capacity; `textures/gui/canister_overlay.png` still exists. Restore dynamic item rendering from the actual stack contents, including appropriate fluid tint/texture handling.

**Acceptance checks:** Inspect empty, partly filled and full water/lava canisters, plus an available compatible modded fluid. Check player inventory, hotbar and the canister creative tab. Filling, draining and issue 7's placement must update the displayed level immediately; separate stacks must not share stale fluid appearances. Verify reload and multiplayer inventory synchronization.

### 7. Canister-placed fluid disappears and contents are not consumed

**Evidence:** User report only; no screenshot or live reproduction in this preparation pass. A fluid block appears briefly, then disappears, while the canister's stored amount does not decrease. Treat this as a gameplay/state-synchronization defect separate from issue 6.

**Required result:** A valid placement persists under normal fluid rules and consumes exactly one bucket (1,000 mB) once, with matching server and client contents. Failed placement must not consume fluid. Preserve legacy special cases such as water vaporization in the Nether, including the correct consumption and feedback.

**Starting points:** `src/main/java/net/bdew/pressure/items/CanisterItem.java`, especially `useOn`, its capability handler and the `FluidUtil.tryPlaceFluid` result. The world-placement branch currently calls the helper without the server-side guard used in the handler-transfer branch, checks only success, and does not apply the returned container to the held stack. Review Forge's helper contract and server-authoritative world/inventory updates. The transient appearance suggests prediction/synchronization or discarded container changes, but the exact cause is not confirmed. Compare legacy `items/Canister.scala`, which explicitly drains one bucket and places the block on the server.

**Acceptance checks:** Reproduce in survival with a known starting amount of water and lava, and verify amount before/after. Wait through fluid ticks, leave/return to the chunk and reload the world; valid source placement must persist. Check repeated placements, less than one bucket remaining, occupied/invalid targets, dimension/build-height restrictions, water in the Nether, container transfers and offhand use. Compare creative behavior with the legacy implementation. Repeat with a dedicated server and connected client, checking both authoritative world state and inventory so no ghost placement, duplication, or double consumption remains.

## Migration gaps and current disposition

Fixing the original seven reports alone does not finish the migration. Carry these items and R1–R8 into the receiving session's goal. October 3 verification remains historical where a new report reopens acceptance.

| Work item | Current state / required result | Reference and validation |
|---|---|---|
| Legacy tank GUI | Restored with historical menu/server/multiplayer checks; **R7 alignment open**. | Preserve the gauge, two container slots, filter, six output widgets/redstone controls and player inventory. Recheck rendering/hit regions and settings synchronization after R7. |
| Mekanism gas integration | Four conditional blocks and transfers implemented; **R5 recipe acceptance reopened**. | Legacy `blocks/gas/`; preserve conversion, simulation, filtering and persistence. Resolve the actual alloy tag and craft all four recipes with the supported installed version. |
| CC: Tweaked integration | **Open:** replaces OpenComputers by user instruction. Full peripheral implementation/runtime acceptance required under R3. | Use the legacy computer commands as the equivalence contract; test a pinned Forge 1.20.1 CC: Tweaked API/runtime and startup with it absent. The old OpenComputers blocker is superseded, not a completed CC integration. |
| EnderIO integration | Implemented; lock/clear passed against installed 6.2.18-beta. | Legacy `compat/enderio/`; validate against an actual supported 1.20.1 dependency/API, including filtering behavior. |
| JEI integration | Implemented; the packaged client confirmed one empty canister and hidden filled variants under the default option. | Legacy `compat/jei/PressureJeiPlugin.scala`; restore applicable discovery/recipe integration and test with JEI installed. |
| Waila information providers | Ported to Jade; a packaged-client overlay showed the server's fluid amount, capacity and filter. | Legacy `waila/`; restore equivalent block/tank/module/valve information using the supported 1.20.1 overlay API. Verify the displayed data against server state. |
| Dynamic visual parity beyond the seven reports | Existing overlays/columns/frames retain historical checks; **R4 indicator lighting open**. | Inspect standalone and connected indicators under matched lighting without regressing connected frames or dynamic displays. |
| Full gameplay parity | Historical 23-test suite and packaged/multiplayer/restart checks passed; **R1–R8 and audible playback remain unverified**. | Use the validation matrix and each revision's acceptance checks; record actual results and any real blockers. |
| Recipes, loot and language | Historical crafting/drop/name checks recorded; **R5 gas recipe checks reopened**. | Test actual ingredient resolution/JEI and crafting; preserve loot, language and dependency metadata. |
| Packaging and delivery | October 3 JAR retains historical validation. **Revised candidate build/packaging open**; work remains uncommitted, no release published. | Rebuild after implementation, test the final packaged JAR, record its hash/dependencies and review the working tree. |

Compatibility status must distinguish an available integration that needs implementation from a dependency/API genuinely unavailable on 1.20.1. Record the exact dependency/version, missing API or runtime, attempted validation, and remaining full-equivalent work for any blocker. Do not silently delete features or mark a placeholder as parity.

## Required validation matrix

| Area | Minimum checks before calling it verified |
|---|---|
| Models and resources | Every registered block/item; inventory, held and world presentation; all supported facings, state variants, legacy shapes/UVs and dynamic overlays; fresh client after rendering changes |
| Pressure network | Sources, input/output, pump, pipes, rings/branches, valve direction and redstone, graph invalidation after breaking/reconnecting and chunk reload |
| Fluid machines | Source generation and bucket filling; measured near-infinite pump/input throughput with effective configuration recorded; conservation, power/redstone, drain/sluice, filters and blocked/no-target cases |
| Router | Every per-face mode, output priorities, filters, redstone gates, GUI interaction, persistence and multiplayer configuration updates |
| Tanks | Assembly/disassembly, controller ownership, capacity/content preservation, every module, active fluid output and preserved external-pull redstone behavior, filters/access/interfaces/data port, connected indicator lighting and aligned complete GUI |
| Sensors and indicators | Fill thresholds, output flow and fluid-match modes; redstone outputs, displayed contents/configuration and synchronized rendering |
| Items | Canister gauge/placement/transfer edge cases; hand-pump transfer and durability; configurator and debugger behavior |
| Persistence and multiplayer | Save/reload, chunk unload/reload, reconnect and a second client; compare fluid amounts and settings with server state |
| Content and integrations | Recipes including all four Mekanism gas alloy ingredients, loot, language/screens; complete CC: Tweaked tank/sensor peripherals; each integration with its dependency installed; base startup when optional dependencies are absent |
| Packaging | Full build including `reobfJar`; inspect packaged assets/metadata and launch the resulting JAR in a normal Forge instance; dedicated-server startup and relevant gameplay checks |

## Suggested execution order and commands

The original canister/missing-texture-first pass is preserved in the October 3 record. Continue with the revised open work rather than redoing already verified fixes without evidence of a regression.

1. Inspect `git status --short`, reread the documents and reproduce R4–R8 against the actual deployed artifact/configuration. Identify the exact fluid-output variant and installed dependency versions. Compare legacy/current behavior and screenshots.
2. Fix R5 recipes, R6 throughput and R8 active output in separate reviewable steps; preserve the user's working redstone behavior. Implement R1 restricted Shift placement and R2 bucket filling.
3. Fix R4 connected-indicator lighting and R7 GUI alignment, then complete fresh-client rendering/interaction checks.
4. Implement the complete R3 CC: Tweaked peripheral contract with a supported 1.20.1 runtime. Record concrete blockers if any and continue independent work.
5. Preserve the original seven fixes/full GUI and complete all revised acceptance checks plus the validation matrix, including persistence and multiplayer.
6. Run the full build, final packaged-client/server checks and hash verification. Update this tracker/handoff with exact commands, evidence and remaining work; do not declare completion with open acceptance.

Run from the repository root in PowerShell:

```powershell
git status --short
$env:GRADLE_USER_HOME = 'C:\Users\TheRe\.gradle'
.\gradlew.bat build
.\gradlew.bat runClient -PrunDirectory=run
```

All network downloads must use the user-provided local proxy; set it before any online dependency/asset resolution:

```powershell
$env:HTTP_PROXY = 'http://localhost:7897'
$env:HTTPS_PROXY = 'http://localhost:7897'
```

For Java/Gradle downloads also pass `-Dhttp.proxyHost=localhost -Dhttp.proxyPort=7897 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897`. Offline runs require the populated dependency cache.

For dedicated-server validation, the build declares a server run configuration:

```powershell
.\gradlew.bat runServer -PrunDirectory=run-server
```

Follow the normal server EULA/setup requirements before launch. Completed normal packaged-server and restart commands/results are recorded in the verification report. A successful `runClient` is not a full artifact build, and either Gradle success alone is narrower than gameplay verification.

For final packaged-client testing, install the rebuilt JAR together with BdLib `1.27.0.8-mc1.20.1` and Scalable Cats Force `3.3.0-build-2` in a Minecraft 1.20.1 / Forge 47.x / Java 17 instance. Record the exact Forge version tested, JAR path and SHA-256. Reopen `run/saves/New World` for comparisons where useful, and use a fresh client and fresh placements to detect stale resources/states.

## Completion and reporting checklist

The original checks below retain their historical evidence. Reopened acceptance and all October 5 additions must pass on the revised implementation before final delivery.

- [ ] R1: Shift reverses eligible input/output blocks only, across all facings, with transfers and persistence checked.
- [ ] R2: Infinite Water Source fills buckets with correct survival/creative/stack/offhand inventory behavior.
- [ ] R3: Full CC: Tweaked tank/sensor peripheral contract implemented and tested; optional dependency absence checked.
- [ ] R4: Connected indicator lighting fixed and fresh-client/multiplayer/state comparisons pass.
- [ ] R5: All four Mekanism gas recipes resolve the correct alloy and pass JEI/actual crafting checks.
- [ ] R6: Pump/Input near-infinite throughput measured, effective configuration recorded, conservation/limits checked.
- [ ] R7: All tank output widgets and hit regions align at multiple GUI scales; synchronization remains correct.
- [ ] R8: Reported tank output actively pushes fluid; all existing external-pull redstone behavior remains correct.

- [x] Issue 1: Pipe ring/connections fixed and all axis/connection checks verified.
- [x] Issue 2: Pipe inventory/hand transforms match the legacy presentation.
- [x] Issue 3: Pump inventory orientation fixed; placed pump remains correct across facings/states.
- [x] Issue 4: All seven named blocks pass inventory and placed-model checks.
- [ ] Issue 5: Preserve historically checked frames/connections; complete reopened connected-indicator lighting acceptance under R4.
- [x] Issue 6: Canister icons display the correct fluid and proportional fill after every content change.
- [x] Issue 7: Canister placement persists and consumes fluid correctly on server and client.
- [ ] Complete legacy-equivalent tank GUI: preserve restored functionality and complete reopened output alignment acceptance under R7.
- [ ] Compatibility and dynamic parity: complete CC: Tweaked/R4/R5 acceptance; document any genuine remaining blockers without marking them complete.
- [ ] Unqualified full parity/release acceptance: all R1–R8, original preserved functionality and required gameplay/render/content/persistence checks pass; audible feedback is verified or its concrete remaining limit is reported.
- [ ] Revised implementation passes full build and final packaged-client/dedicated-server checks; final JAR path/hash/dependencies recorded. October 3 results do not verify a future artifact.
- [x] Working-tree review and intended delivery status recorded; project commits include the required co-author trailer.
- [x] This tracker and `DEVELOPMENT_HANDOFF.md` reflect the same current verification status.

For every completed issue, record the changed files, exact reproduction/check steps, command/result where applicable, tested runtime versions, and screenshots or equivalent gameplay evidence. For every unverified or blocked issue, state what remains and why. Do not mark migration finished while required behavior, integrations, or gameplay/packaging checks remain incomplete.

### Historical delivery state and next candidate

The October 3 checked JAR was `build/libs/pressure-2.0.0-DEV-mc1.20.1.jar`, SHA-256 `AD22061E73032F48CA8126D4905A150EF785FC659A301E21547E077969358139`. The main JAR, both clients and the dedicated server were hash-matched; QA classes/test structures were excluded. No replacement artifact was built for this documentation revision. All eight October 5 requirements, their acceptance checks, revised packaging and audible sound verification remain open, so the migration is not declared complete. OpenComputers is superseded as a required target by CC: Tweaked.
