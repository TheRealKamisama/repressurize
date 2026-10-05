# Migration verification — 2026-10-05

The requested migration pass is complete: R1–R8 and required revised acceptance checks pass. Historical October 3 evidence was not used to verify this candidate. The user explicitly deferred all audio checks to their own later testing; no audible verification is claimed. Task-owned clients and servers are shut down after verification.

## Investigation and implementation

- Initial `git status --short`: only `?? forge-installer.jar.log`; existing files and log preserved. The historical source JAR was copied to `build/migration-evidence/2026-10-05/baseline-pressure.jar` before rebuilding.
- All ten supplied 1.12.2/1.20.1 screenshots and the three October 5 images were viewed.
- R5: installed Mekanism 1.20.1-10.4.16.80 defines `forge:alloys/advanced` containing `mekanism:alloy_infused`; no `mekanism:alloys/advanced` exists. All four recipes now use the installed Forge tag. The historical recipe test treated unresolved ingredients as empty slots; it now distinguishes `Ingredient.EMPTY` and a separate test requires the actual alloy and rejects its omission.
- R6: `run/config/pressure-common.toml` and every existing isolated test instance still contain `fluidTransferPerTick = 4000`. The source default was already 2147483647. These saved values are not overwritten. New finite-source/sink tests measure pump, input and pump→input independently with both explicit 4000 and unrestricted settings, including the integer boundary. The user's deployed instance/config is not established by the screenshots.
- R8: legacy ordinary output is passive and automatic output pushes. The requested behavior deliberately extends the ordinary registry variant to push too, through the same `canOutput` redstone gate; its external-pull handler is preserved.
- R1: explicit opt-in reverses pump/input/output/sluice/check-valve/pipe-sensor and Mekanism pressure gas machines. Tank modules expose all relevant faces and do not use their facing to select upstream/downstream, so they and presentation-only directional blocks retain normal placement.
- R2: server-side infinite-source bucket interaction uses vanilla filled-result inventory behavior, including survival stacks/offhand, creative duplicate suppression and delivery when inventory is full.
- R4: connected indicator replaces the baked face's texture, retaining its AO/normals/shading. The redundant opaque block-entity face overlay is removed; fluid gauge uses exposed-face world light and directional shade, with fluid emission retained. Fresh before/after and peer acceptance passed.
- R7: texture borders are at `(43 + 21*i,42)`, item insets `(44 + 21*i,43)`. Outline/icon/button/hover geometry shares these coordinates; redstone controls use 18×18 bounds. Multi-scale interaction, tooltip, peer and restart acceptance passed.
- R3: complete tank/sensor command implementation uses optional CC: Tweaked 1.120.2, Forge release ID `gRtcHJd8`. Numeric Modrinth resolution selected Fabric, corrected before runtime use. All computer access is on the server thread. Actual Lua contract, errors, reconnect/reassembly, peer mutation and saved restart acceptance passed.

## Commands so far

Java 17 is `C:/Users/TheRe/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.20.1+1/bin/java.exe`; Gradle uses the existing `C:/Users/TheRe/.gradle` cache. Downloads use HTTP/HTTPS proxy `http://localhost:7897`; Java resolution receives `-Dhttp.proxyHost=localhost -Dhttp.proxyPort=7897 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897`. `scripts/download-proxy.cjs` uses the same CONNECT proxy because this environment's curl Schannel credentials fail.

- `gradlew.bat [four proxy properties] compileJava`: initial changes compiled; later CC compile identified the Fabric artifact mismatch. After pinning Forge, compilation passed in 1m (`compile-forge-cc.log`).
- Current results are below. Logs and dependency metadata are under `build/migration-evidence/2026-10-05/`.

## Current passing checks and remaining verification

| Command / check | Result / scope |
|---|---|
| `gradlew.bat --offline build qaJar` | Final full build passed in **51s** (`final-build.log`), including the reobfuscated artifact and separately reobfuscated fixture. The ordinary `test` task is `NO-SOURCE`; Forge gameplay tests supply runtime acceptance. |
| `gradlew.bat --offline qaJar runGameTestServer -PcompatTest` | **31 required tests passed**, 2m 41s (`server-all-faces.log`). Full legacy regression suite plus all R1–R8 server checks, including real CC Lua commands. |
| `node scripts/validate-assets.cjs` | Passed: 73 models / 62 local textures / 37 recipe files. |
| Installed base server: Java 17 + four proxy properties + `@user_jvm_args.txt @libraries/net/minecraftforge/forge/1.20.1-47.1.39/win_args.txt nogui` from `build/package-server` | Final JAR starts with optional mods absent: `Done (7.128s)!`, normal shutdown. No Pressure loot/recipe or optional-class errors. Retained effective rate 4000 is logged with its config path. |
| `scripts/prepare-packaged-client.ps1 -Username PressureRevisionBefore -Mode revision -Baseline`; same for `PressureRevisionAfter` without `-Baseline`; Java 17 `@launch.args` from each instance | Final **21 matched lighting scenes** include single-height empty/partial/full and unobstructed side views, multi-height empty/partial/full, daylight/artificial, AO on/off, controller removal/reassembly. Zero/two/six outputs at GUI scales 1–4 and all six edge clicks pass. Both serial clients exited normally. Mean flat indicator RGB changes 121.20/123.38/134.63 → 131.31/133.69/147.06; neighboring tank is 131.49/133.85/147.22. |
| `scripts/prepare-packaged-client.ps1 -Username PressureVisual -Mode visual`; Java 17 `@launch.args` | Valid 1280×960 package captures, 512 states / 35 items bake/translation check, one JEI canister and no filled variants, preserved models/screens/Jade, Nether amount 1500 and smoke, all four JEI recipes show the infused alloy. All preservation images inspected, including powered/unpowered six-facing galleries, rings, frames, items and menus. |
| Normal server with `-Dpressure.networkQaServer=true -Dpressure.revisionNetwork=true`; both prepared network clients use Java 17 `@launch.args` | Both peers pass the original canister/menu/ghost-filter state, actual chunk unload/reload, actual survival offhand bucket filling and Shift pump placement. Both see all six CC-set `RS_OFF` modes and `minecraft:water` filter. Final reports are saved. |
| From `build/package-gametest`: Java 17 `-Xmx2G -Dpressure.packagedGameTests=true -Dforge.enabledGameTestNamespaces=pressure [four proxy properties] @forge.args nogui` | **All 31 tests pass against the production JAR in normal installed Forge**, including the complete CC Lua contract and survival crafting. `packaged-gametests-isolated.log` / `packaged-gametest-results.json`. `forge.args` uses the absolute libraries of the installed `build/package-server` distribution; this is not userdev. |
| From `build/package-server`: Java 17 `-Dpressure.revisionNetworkReload=true [four proxy properties] @user_jvm_args.txt @libraries/net/minecraftforge/forge/1.20.1-47.1.39/win_args.txt nogui` | **Dedicated restart passed** (`packaged-network-restart-forced.log`). World source, pump north facing, 12000/64000 mB, water filter, all six `RS_OFF` modes, canister 1500 mB, one water bucket and two offhand empties persist. Actual CC startup rediscovers tank/sensor and checks persisted counts/averages. The fixture temporarily forces remote chunks to tick without players and releases them on completion. |
| Same installed server with `-Dpressure.peerViewServer=true`; each normal client separately uses Java 17 `@launch-peer.args` | Both fresh peers inspect correct connected indicator lighting/gauge after restart and show all six `RS_OFF` button labels. The second peer exercises every output tooltip at GUI scales 2/4. `PRESSURE_PEER_VIEW_PASSED` in both logs; clients exit normally. No desktop cursor movement is used: the test frame supplies logical hover coordinates to the actual screen renderer. |

Candidate: `build/libs/pressure-2.0.0-DEV-mc1.20.1.jar`, SHA-256 `5A72E5C98ED9E3ADCB1726B3A013442FD7B11C00720604CAD2130B0B87D49CF3`. This exact hash is installed on the tested server and clients. Main JAR has no QA classes/test structures; `build/qa/pressure-qa.jar` is a separate, non-distributable fixture.

The build manifest's `Implementation-Timestamp` changes on later builds. The 51s final build also produced timestamp variant `EDDBA9B59DDE044303D4072C3FB076499FE54FF1EA4667C972F6E53615372150`. Every entry name and every production class/resource/recipe/mod-metadata byte matches the runtime-tested candidate; only the manifest timestamp differs. [Artifact comparison](evidence/2026-10-05/artifact-content-comparison.json) records the hashes. The timestamp variant is preserved in `build/migration-evidence/2026-10-05/build-timestamp-variant.jar`; delivery restores the fully hash-matched tested 5A72… candidate to `build/libs/`. No source changes were reverted.

Mekanism recipe tests now use actual survival `CraftingMenu` result-taking and confirm alloy consumption. The old unresolved-tag test could pass with an empty slot; that false acceptance is fixed.

Throughput measurements cover finite source/sink paths `pump`, `input`, `pump+input`; amounts 1,000,000,000 and 2,147,483,647 move entirely with cap 2147483647, while explicit 4000 yields 4000 in every path. Filtered and unpowered cases conserve fluid. The source default needs no further cap removal. The actual user's deployed configuration is still not established; local saved 4000 values are confirmed and preserved.

## Failures investigated and corrected or still under check

- Creative full-inventory bucket delivery needed a separate result copy because `Inventory.add` can clear the insertion stack. The complete matrix now passes.
- New crafting QA initially reused the global fake player and interfered with a delayed canister test in the normal installed-server suite. A dedicated profile fixes the fixture race; the repeated normal packaged suite passes all 31 tests. No production canister regression was found.
- The CC fixture initially scheduled repeated callbacks inside an already-running GameTest callback; moving registration outside avoids a scheduler map mutation. The actual Lua contract passes.
- Forge's early window lost the primary monitor. `-Dfml.earlyprogresswindow=false` does not control this Forge version. Its documented `config/fml.toml` setting `earlyWindowControl=false`, changed only in isolated QA profiles, allowed valid full-size rendering and both clients. The first 1×1 captures were rejected and overwritten by valid 1280×960 captures.
- Idle failed/completed clients exhausted Windows commit memory in a later parallel retry. The three remaining task-owned clients were closed at the user's request; remaining checks run serially. The completed network-client shutdown check now executes before the missing-player/world guard, so server disconnect cannot strand a completed client. No unrelated Java processes were terminated.
- The no-player restart fixture loaded remote chunks without simulation, so the saved computer could not complete startup. Forcing the same four remote chunks as the client pass resolves it; restart acceptance passed and the fixture releases its tickets.
- Audio: one recorded client initialized OpenAL on Remote Audio; later device availability varied. This does not establish audible acceptance. The user explicitly excluded all audio checks and will test personally; no audible verification is claimed.

## Delivery and reproducible entry points

Final tested JAR path/hash above; no required implementation or acceptance blocker remains. The user requested commit and push of the verified implementation/evidence on October 5; this follow-up commit records that delivery. No release was published. `forge-installer.jar.log` is preserved outside the commit, and prior source/evidence remains intact. Static checks and build results are distinct from the runtime acceptance recorded here.

The proxy properties abbreviated in the table are exactly:

```text
-Dhttp.proxyHost=localhost -Dhttp.proxyPort=7897 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897
```

For paired clients, prepare with `scripts/prepare-packaged-client.ps1 -Username PressureQA1 -Mode network` and `PressureQA2`, install the same production/fixture/required/CC jars, then use Java 17 `@launch.args` from each instance. The server command is Java 17 `-Dpressure.networkQaServer=true -Dpressure.revisionNetwork=true` followed by the four proxy properties and `@user_jvm_args.txt @libraries/net/minecraftforge/forge/1.20.1-47.1.39/win_args.txt nogui` from `build/package-server`. The read-only `launch-peer.args` disables network action fixtures and sets `-Dpressure.peerViewClient=true` for the saved-world peer views.

The normal packaged suite uses the installed distribution's libraries and optional profile in `build/package-gametest`, with a separate QA ticker because Forge disables its ordinary GameTest command registration in production. It still executes real Minecraft GameTest helpers/world/capabilities against the reobfuscated production mod, not substitutes for mod logic.

Selected permanent evidence and logs are indexed in [evidence/2026-10-05/README.md](evidence/2026-10-05/README.md). Source paths, per-requirement investigations and test methods above provide the fix-to-evidence map; the authoritative checklist and handoff have matching status.
