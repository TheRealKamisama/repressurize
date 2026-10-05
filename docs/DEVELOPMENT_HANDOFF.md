# Pressure Pipes Repressurized — Development Handoff

**Updated:** 2026-10-05 (Asia/Shanghai)

## Current status

The Minecraft 1.20.1 / Forge migration has substantial implementation and historical runtime verification. The original seven regressions and full tank GUI have implementations. The user's October 5 revision adds eight open requirements, reopens affected acceptance checks, and replaces OpenComputers with **CC: Tweaked**. [MIGRATION_STATUS.md](MIGRATION_STATUS.md) is the revised goal specification and completion checklist. **Do not call the migration complete.**

The October 3 command results, runtime scope, images and former blocker evidence remain in [VERIFICATION_2026-10-03.md](VERIFICATION_2026-10-03.md). Two-client placement/menu synchronization, actual chunk unload/reload, inspected rendering and dedicated restart passed on that JAR. Those checks do not establish acceptance of the new reports. No code fixes, new build or runtime verification were performed in this documentation revision.

## October 5 requirements and next work

All items are **open**; use the detailed R1–R8 investigation and acceptance sections in the migration checklist. Mark code changes **implemented but unverified**, then **verified** only after the corresponding checks pass. Record concrete blockers and continue independent work.

| ID | Required change / preservation boundary |
|---|---|
| R1 | Shift placement reverses orientation only for blocks with input/output sides, including High Pressure Pump and High Pressure Input/Output. Preserve unrelated directional placement. |
| R2 | Right-click Infinite Water Source with an empty bucket fills it; verify stacked/offhand/survival/creative inventory behavior. |
| R3 | Implement optional CC: Tweaked tank data-port and pipe-sensor peripherals equivalent to all legacy computer commands. OpenComputers is no longer required. |
| R4 | Fix connected Tank Indicator darkening, preserving correct standalone lighting, frames and gauge. Ambient light/AO is an unconfirmed hypothesis. |
| R5 | Repair all four Mekanism gas-block recipes showing `Empty Tag: mekanism:alloys/advanced`; inspect actual installed tags and test JEI plus crafting. |
| R6 | Measure and restore near-infinite High Pressure Pump/Input throughput. Source default is already `Integer.MAX_VALUE`; investigate effective config and endpoint/intermediate limits rather than assuming the cause. |
| R7 | Align all six tank output widgets, buttons and hit regions at multiple GUI scales; preserve full menu functionality and synchronization. |
| R8 | Restore the reported Tank Fluid Output's active push behavior. Preserve existing redstone rules, which the user confirms work correctly with an externally pulling pipe. |

The three screenshots were inspected and preserved in [evidence/2026-10-05/](evidence/2026-10-05/README.md). They are user evidence, not a fresh-client check of the checked-out implementation; their artifact/dependency configuration is not established.

For R8, distinguish registry variants during reproduction: legacy `FluidOutput.scala` has an empty `doOutput`, while `FluidAutoOutput.scala` actively pushes; active code ticks `tank_fluid_output_auto` but excludes `tank_fluid_output`. Reconcile that distinction with the reported 1.12.2 behavior without dropping the requested active-output result or changing working redstone semantics.

Continue with reproduction/configuration checks, recipe/throughput/output fixes, restricted placement/bucket features, indicator/GUI fixes and complete CC: Tweaked integration. Preserve earlier fixes, then rerun revised server/gameplay, fresh-client, multiplayer/persistence and final packaging acceptance. All downloads must use `http://localhost:7897`.

## Working tree and delivery

- Repository: `C:\Users\TheRe\Documents\ChatGPT\Repressurise`.
- Branch remains `codex/pressure-pipes-1.20.1`. The user authorized committing and pushing the current migration checkpoint on October 5. This checkpoint preserves the open R1–R8 requirements and historical verification boundaries; it is not a completed migration or release.
- Active Java is under `src/main/java/`; runtime resources are under `resources/`; `legacy-src/` remains the 1.12.2 correctness reference and is not compiled.
- The pre-edit source/resources/docs snapshot is `build/migration-evidence/baseline-2026-10-02.zip`. A comparison against that snapshot is recorded in `docs/evidence/2026-10-03/change-review.txt`.
- Every future project commit must include `Co-authored-by: Codex <codex@openai.com>`.

Last verified production artifact (October 3; does not include the new requirements): [pressure-2.0.0-DEV-mc1.20.1.jar](../build/libs/pressure-2.0.0-DEV-mc1.20.1.jar).

SHA-256: `AD22061E73032F48CA8126D4905A150EF785FC659A301E21547E077969358139`.

This JAR was built with `gradlew.bat --offline build qaJar` (55 seconds, including `jar` and `reobfJar`). The separately generated `build/qa/pressure-qa.jar` is a test fixture and must not be distributed with the mod. The main JAR contains no QA classes or test structures. Recheck the hash after any further production build.

## Runtime and build configuration

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.1.39 |
| Java | 17; tested with Eclipse Adoptium 17.0.20.1 |
| Gradle / ForgeGradle | Wrapper 8.5 / ForgeGradle 6 |
| Required BdLib | 1.27.0.8-mc1.20.1 |
| Required Scalable Cats Force | 3.3.0-build-2, with-library |
| Optional test profile | Mekanism 10.4.16.80; JEI 15.62.0.217; Jade 11.13.3+forge; EnderIO 6.2.18-beta |

From the repository root in PowerShell:

```powershell
$env:GRADLE_USER_HOME = 'C:\Users\TheRe\.gradle'
$env:HTTP_PROXY = 'http://localhost:7897'
$env:HTTPS_PROXY = 'http://localhost:7897'
.\gradlew.bat build
.\gradlew.bat runGameTestServer -PcompatTest
.\gradlew.bat runClient -PrunDirectory=run
```

Use `--offline` only with the populated cache. For Java/Gradle dependency downloads also pass `-Dhttp.proxyHost=localhost -Dhttp.proxyPort=7897 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897` when needed. The user requires this proxy for downloads.

`-PcompatTest` installs the pinned optional mods in the development run and enables the SRG-to-official mixin remapping needed by EnderIO in userdev. Normal packaged Forge runs do not use that development remapping.

## Previously implemented changes (preserve; revised acceptance above)

- Canister placement is server-authoritative and applies Forge's returned drained stack. It validates bounds/permissions, consumes one bucket once, preserves fluid NBT, and rejects partial/invalid placements. Saved flowing-fluid variants are normalized to source fluids, fixing a separately reproduced transient-placement case. The creative tab excludes empty/duplicate flowing variants while retaining other registered fluids.
- Canister gauges use each stack's current fluid texture, tint and fill ratio. Nether smoke is explicitly sent from the server, and the using player receives the vaporization sound notification. Audio playback itself cannot be heard on this test desktop.
- All seven missing-texture blocks use resolvable face slots. Directional models retain legacy UV rotation; valve/sensor bodies and conditional pipe arms use the legacy narrow geometry. Pipe X-axis rotation and item transforms are corrected. The pump inventory model inherits the corrected placed model.
- Tank frames follow synchronized, persisted controller ownership, including diagonal corners. New modules do not evict existing members at configured limits. Adjacent controllers do not share capacity. Indicator caps/vertical gauges and sensor UVs are restored.
- Tank menu/screen restores the gauge, container input/output, ghost filter, six outputs/redstone controls, player inventory and shift-click. Container handling conserves fluid when output is blocked. Tank interfaces expose item automation. Output modes, 20-tick averages and input/output counters persist.
- Pressure traversal follows the forward path correctly, handles every endpoint face of a router, avoids forcing unloaded chunks, and refreshes cached connections on chunk changes. Debugger output again enumerates inputs, outputs and traversed blocks.
- Pump passive filling/filtering and unrestricted legacy default throughput are restored. Existing user configuration values remain intact; the `fluidTransferPerTick` setting controls the cap.
- Drain voiding/XP conversion and sluice backpressure are distinct again. Hand pumps ray-trace sources, preserve waterlogged blocks, handle stacked/replacement containers and synchronize inventory while retaining the durability registration fix.
- Searchable configurator and tank-sensor screens, router ghost filters/player inventory, router/filter/output-arrow overlays, and persisted sensor counts/averages are implemented.
- Mekanism restores all four gas blocks and reversible recipe-based gas/fluid conversion. JEI canister visibility, Jade server data, and EnderIO fluid-conduit lock/clear integration are implemented.

## Historical verification (2026-10-03)

- Final source server suite: **23 required tests passed**, `gradlew.bat --offline runGameTestServer -PcompatTest`, **2m 16s**; `build/migration-evidence/final-source-tests.log`.
- Runtime crafting/loot checks cover **37 recipes and 28 registered blocks** with the optional profile.
- Static asset check: `node scripts/validate-assets.cjs` passes **73 models, 62 local textures and 37 recipe files**.
- A normal packaged optional-mod client checked **512 block states / 35 items**, confirmed item translations and JEI filtering, and captured 17 views. Jade showed actual tank contents/filter, and real Nether use synchronized the remaining 1500 mB and displayed smoke.
- Inspected images cover both powered/unpowered facing galleries, rings in three planes, both hands/third-person/dropped pipe, standalone and connected frames, controller removal/reassembly, gauges and all restored screens. Permanent evidence is under `docs/evidence/2026-10-03/`.
- A normal packaged dedicated server starts with optional mods absent. The optional gas loot tables now use optional item tags so absent Mekanism items do not break loot parsing.
- Two packaged clients previously verified source placement, exact canister consumption, ghost-filter non-consumption and shared tank/output settings. Shutdown/restart preserved world and player data. The current candidate also passed actual chunk unload/reload and both clients' restored rendering checks.

## Remaining work, blockers and limits

The former classic OpenComputers runtime blocker is **superseded by the user's CC: Tweaked target**, not a reason to defer R3. CC: Tweaked implementation and runtime verification are open; no compatible version/API has been pinned or tested in this revision. Preserve the full command contract: `pp_tank` exposes `isConnected`, `getCapacity`, `hasFluid`, `getFluid`, `getOutputs`, `setOutputMode`, `getFilter`, `setFilter`, `getCountsIn`, `getCountsOut`; `pp_sensor` exposes `getAverages`, `getCounts`. The legacy implementations and October 3 blocker evidence remain historical references, and active flow accounting must be retained.

The desktop has no usable OpenAL audio device, so audible feedback remains unverified. EnderIO also logs missing optional Athena capacitor-bank models when Athena is absent; Pressure's own bake checks pass.

The prior paired chunk cycle and restart passed; repeat relevant checks after the new fixes and on the final revised package. R1–R8 are open work, not established technical blockers. Preserve the checked artifact/evidence, investigate the reported causes, implement the full CC: Tweaked equivalent and address the audible-feedback limit before declaring full completion. Keep README player-facing. The historical pump render and hand-pump constructor fixes must remain intact.
