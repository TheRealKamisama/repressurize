# Migration verification — 2026-10-03

This is an active verification record. The migration is not declared finished. The current packaged candidate SHA-256 is `AD22061E73032F48CA8126D4905A150EF785FC659A301E21547E077969358139`; the final two-client chunk cycle and dedicated restart passed.

## Tested environment

- Minecraft 1.20.1, Forge 47.1.39, Eclipse Adoptium Java 17.0.20.1, Gradle 8.5.
- BdLib 1.27.0.8-mc1.20.1; Scalable Cats Force 3.3.0-build-2 (with-library).
- Optional test profile: Mekanism 1.20.1-10.4.16.80, JEI 15.62.0.217, Jade 11.13.3+forge, EnderIO 6.2.18-beta.
- All downloads use `http://localhost:7897` for HTTP and HTTPS. Java tools also receive the corresponding proxy system properties.
- The original working tree was inspected before edits. A pre-edit source/resources/docs snapshot is in `build/migration-evidence/baseline-2026-10-02.zip`. No reset, checkout, commit, push or publication was performed.

## Commands and evidence

Run from the repository root unless specified otherwise. Logs are under `build/migration-evidence/`.

| Command/check | Recorded result | Scope |
|---|---|---|
| `node scripts/validate-assets.cjs` | Passed: 73 reachable models, 62 local textures, 37 recipe files | Resolves parent/texture slots and validates recipe symbols; runtime baking is checked separately. |
| `gradlew.bat --offline runGameTestServer -PcompatTest` | Final source run passed **23 required tests** in **2m 16s** (`final-source-tests.log`) | Real Forge server tests, including old flowing-fluid canisters, multi-face router traversal, recipes/drops and optional integrations. The regular Gradle `test` task remains `NO-SOURCE`. |
| `gradlew.bat --offline build qaJar` | Passed in **55s** (`delivery-build.log`), including `jar`, `reobfJar` and the separately remapped QA helper | Produces the current candidate above. Main JAR contains no QA classes or test templates. |
| `gradlew.bat --offline runClient -PrunDirectory=build/render-qa -PmigrationQa -PcompatTest` | Completed in 3m 11s (`visual-qa-compat-fixed.log`) | Fresh development client; ten captured views were inspected. The final packaged run supplies the expanded views and all-state assertion described below. |
| Normal Forge server with the reobfuscated JAR and required dependencies | Reached `Done (6.603s)!`; shut down normally (`packaged-base-server.log`) | Base dedicated-server packaging smoke test, with optional mods absent. |
| Normal Forge client with the production JAR and separate QA helper | Reached the world and produced ten screenshots | Actual reobfuscated JAR, not Gradle source directories. Evidence: `build/package-client/PressureVisual/packaged-client-retry.log` and its `screenshots/`. |
| Two normal Forge clients connected to `127.0.0.1:25595` | Both passed | Real vanilla item/menu packets verified source placement, canister 2500→1500 mB, ghost-filter non-consumption, lava filter, output `RS_ON`, and matching 12000/64000 mB tank state. JSON reports exist in both client instances and the server directory. |
| Normal dedicated-server restart | Passed (`network-qa-reload.json`) | Source block, player canister 1500 mB, tank 12000 mB, lava filter and output `RS_ON` survived shutdown/restart. The final run additionally confirmed actual chunk unload/reload and rendering on both peers. |

The first separate QA JAR exposed a remapping-classpath defect in the helper (`PipeBlock.defaultBlockState` remained named). Supplying the production class hierarchy to `reobfQaJar` corrected it. The production JAR excludes all QA classes, templates and the `pressureqa` mod descriptor.

The final packaged optional-mod client completed all 17 captures and validated **512 block states / 35 items**, with no missing Pressure sprites or item translations. JEI confirmed one empty canister and zero filled variants with `showCanisters=false`. Jade displayed the actual tank's `47000 / 128000 mB` and water filter. A real Nether interaction consumed 1000 mB, synchronized the remaining 1500 mB and displayed smoke. Permanent images and selected logs are in [evidence/2026-10-03](evidence/2026-10-03/).

## What the server suite exercises

- Canister water placement after fluid ticks, exact bucket consumption, offhand, occupied/partial targets, simulation, fluid NBT, Nether vaporization/creative consumption, repeated water/lava placement and height/border rejection.
- Hand-pump replacement of one bucket from a stack, inventory delivery and survival durability.
- Tank ownership through modules, adjacent controllers without shared capacity, stable ownership at module limits, bucket fill/drain and blocked-output conservation, ghost filters, shift-click, output-mode persistence and executed-flow counters.
- Pressure ring flow without duplicate delivery, break/reconnect invalidation, powered-pump throughput/filtering, router priorities/gates/passive/disabled modes and unfiltered reload, pipe-sensor executed counts and average persistence.
- Drain voiding, sluice backpressure and fluid filtering.
- Crafting all 37 enabled recipes and obtaining each of the 28 registered blocks' own-item drops with the optional profile.
- Mekanism simulation, gas/fluid ratios, gas pressure input/output, tank gas extraction/automatic output, redstone gates and NBT persistence.
- EnderIO normal fluid-conduit lock and clear against the actual installed 6.2.18-beta implementation.

The current QA sources are under `src/gametest/`; `-PmigrationQa` adds the separate test mod to the client run. `qaJar` builds `build/qa/pressure-qa.jar` solely for isolated packaged-runtime checks. Do not distribute this helper with the mod.

## Rendering evidence and boundaries

Reference screenshots were all inspected under the sibling workspace's `screenshots/1.12.2` and `screenshots/1.20.1` directories. The legacy models and Scala source supplied the geometry, UV and behavior references.

Fresh captures show textured icons for all seven reported blocks, corrected ring geometry in XZ/XY/YZ planes, a small cubic held pipe in both hands and third person, canister water/lava levels, connected tank frames, and actual tank/configurator/sensor/router screens. Inventory pump UVs inherit the already-correct placed model. Tank indicator caps and tank sensor UVs received additional source corrections and are included in the next capture pass.

The unpowered/powered facing galleries, standalone/assembled/removed/restored frames, Jade overlay, modded-fluid gauges, restored screens, both held views, dropped pipe and Nether smoke were captured and inspected from the packaged client. Earlier captures that showed the terrain-loading screen were replaced by a fixture that waits for the actual Nether terrain to be ready.

## Concrete remaining dependency blocker

Classic OpenComputers has no available 1.20.1 runtime in the checked release data. The current published **OpenComputers 1.8.10** releases target **Minecraft 1.12.2 and 1.7.10** (2026-08-04). The version query for 1.20.1 returned `[]`; the official source repository's matching maintained branches are `master-MC1.12` / `OC1.7-MC1.12`, with no 1.20 branch.

Sources: [release metadata](https://api.modrinth.com/v2/project/opencomputers/version), [1.20.1 version query](https://api.modrinth.com/v2/project/opencomputers/version?game_versions=%5B%221.20.1%22%5D), [source repository](https://github.com/MightyPirates/OpenComputers). Retrieved evidence is saved as `build/migration-evidence/opencomputers-versions.json` and `opencomputers-1.20.1.json`.

The unported contract remains explicit: `pp_tank` provides `isConnected`, `getCapacity`, `hasFluid`, `getFluid`, `getOutputs`, `setOutputMode`, `getFilter`, `setFilter`, `getCountsIn`, and `getCountsOut`; `pp_sensor` provides `getAverages` and `getCounts`. Their reference implementations remain in `legacy-src/`. Tank and pipe-sensor count/average storage is restored in the active source, but an actual computer driver and integration runtime test remain blocked. No placeholder is claimed as working OpenComputers support, and OpenComputers 2 is not silently substituted for the classic API.

## Environment limitations and fixes found by validation

- OpenAL could not open an audio device in the test desktop. Sound playback is not audibly verified. Nether consumption is server-tested; explicit server particle delivery and the using player's sound notification were restored after inspecting Forge's client-only particle callback.
- EnderIO's development mixins initially used SRG names against official mappings. `-PcompatTest` now enables Forge's generated refmap remapping; the normal packaged runtime needs no such development override.
- The base package initially logged missing gas-item loot references with Mekanism absent. Gas drops now use optional item tags; the final base-profile run must confirm the log is clean.
- The early migration's 4000 mB/t machine default was lower than the legacy implementation. New configurations now default to unrestricted throughput (`2147483647`); existing user configuration values are preserved.
- A separate test reproduced a second canister placement cause: a saved `minecraft:flowing_water` canister from the old creative tab placed a transient flow even after inventory synchronization was repaired (`flowing-canister-reproduction.log`). Canisters now normalize flowing-fluid variants to their source fluid on read/fill/drain, preserving amount and fluid NBT. The old creative tab also listed empty/flowing variants; the current tab contains source fluids only. The follow-up 22-test run passed, including persistence after fluid ticks.

## Final packaged multiplayer evidence

Both clients and the server used SHA-256 `AD22061E73032F48CA8126D4905A150EF785FC659A301E21547E077969358139`. The server logged `PRESSURE_NETWORK_QA_CHUNK_UNLOADED`, then both clients reported `PRESSURE_NETWORK_QA_CHUNK_RELOAD_PASSED`. Reports confirm 12000/64000 mB, lava filter, output RS_ON, persistent placed water and a 1500 mB canister. The dedicated restart re-read saved world/player NBT successfully.

Evidence: [server report](evidence/2026-10-03/network-server.json), [client 1](evidence/2026-10-03/network-client-1.json), [client 2](evidence/2026-10-03/network-client-2.json), [restart](evidence/2026-10-03/network-restart.json), and corresponding logs/images in that folder. A transient Forge login timeout required a bounded reconnect in one run; gameplay checks then passed. The final camera pass waited for chunk meshes and fixed viewing direction before capturing the restored frames.

Normal server command (from build/package-server): Java 17 `-Dpressure.networkQaServer=true @user_jvm_args.txt @libraries/net/minecraftforge/forge/1.20.1-47.1.39/win_args.txt nogui`. Restart verification uses `-Dpressure.networkQaReload=true` instead. Each normal client runs Java 17 `@launch.args` from an isolated instance prepared by `scripts/prepare-packaged-client.ps1`. These are installed Forge distributions, separate from Gradle userdev.

Task-owned test instances shut down after verification. Work remains uncommitted. This is a tested development JAR; classic OpenComputers and audible playback remain unresolved, and the migration is not declared complete.

## Fix-to-evidence map

| Checklist item | Principal changed files | Reproduction/evidence |
|---|---|---|
| 1 — pipe ring | `resources/assets/pressure/blockstates/pipe.json`; `pressurenet/PressureGraph.java`, `PressureConnection.java` | Ring gallery in XZ/XY/YZ; ring transfer/break/reconnect and multi-face endpoint server tests. |
| 2 — held pipe | `resources/assets/pressure/models/block/pipe_center.json` (default block display transforms) | Both hands, third-person and dropped-pipe captures, compared with the supplied 1.12.2 reference and unchanged 3.2–12.8 geometry. |
| 3 — pump icon | `resources/assets/pressure/models/item/pump.json`; shared `models/block/rotated.json` | Inventory gallery and six-facing off/on galleries; legacy side UV rotations preserved. |
| 4 — seven textures | Block models `input_off/on`, `output`, `drain`, `sluice`, `valve_off/on`, `pipe_sensor_off/on`, `tank_controller`; valve/sensor blockstates and Java blocks | Each named block appears in inventory and facing galleries; all-state bake assertion rejects missing sprites. |
| 5 — tank frames | `client/TankFrameModel.java`, `PressureBlockEntityRenderer.java`, `blocks/tank/TankStructure.java`, tank block entities, `SimpleTankBlock.java`, `PressureNodeBlockEntity.java`, tank indicator/sensor models | Standalone/assembled/removal/reassembly captures, ownership/limits tests and both clients after actual chunk reload. |
| 6 — canister gauge | `client/FluidGauge.java`, `PressureClientSetup.java`, `items/CanisterItem.java` | Empty/partial/full water/lava and Mekanism-fluid gallery; real interaction updates held contents. |
| 7 — placement | `items/CanisterItem.java`, `config/PressureContent.java` | Source placement, conservation, flowing-variant reproduction/fix, bounds, Nether, offhand, dedicated clients and restart. |
| Tank GUI | `blocks/tank/TankMenu.java`, `client/TankScreen.java`, `TankControllerBlockEntity.java`, `TankModuleBlockEntity.java`, `TankControllerBlock.java`, menu/client registrations | Packaged screen, container/shift-click/ghost-filter tests and real menu packets across two clients. |

Java paths in this table are relative to `src/main/java/net/bdew/pressure/`. The complete changed/new-file comparison is [change-review.txt](evidence/2026-10-03/change-review.txt). The matrix galleries use columns input, output, drain, sluice, valve, pipe sensor, tank controller and pump; rows are down, up, north, south, west and east.
