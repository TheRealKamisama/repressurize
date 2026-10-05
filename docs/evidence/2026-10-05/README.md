# October 5 user-reported migration evidence

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
