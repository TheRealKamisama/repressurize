# Pressure Pipes Repressurized

Pressure Pipes Repressurized brings Pressure Pipes to Minecraft 1.20.1 on Forge. It keeps the `pressure` mod ID and adds Forge fluid capabilities for pipe inputs and outputs, pumps, redstone valves, flow sensors, routers, canisters, and pressure tanks.

## Installation

Requires Minecraft Java Edition 1.20.1, Forge 47.1.39 or newer in the 47.x line, and Java 17.

Install Pressure Pipes Repressurized, BdLib 1.27.0.8 for Minecraft 1.20.1, and Scalable Cats Force 3.3.0-build-2 in the instance's `mods` folder. Scalable Cats Force is required by BdLib.

Optional integrations support Mekanism 10.4.16.80, JEI 15.62.0.217, Jade 11.13.3, and EnderIO 6.2.18-beta. Mekanism adds pressure gas input/output and tank gas input/output blocks; reversible rotary recipes provide their gas/fluid correspondence. The configurator can lock or clear normal and pressurized EnderIO fluid-conduit networks.

## Usage

- Right-click a router to open its face settings. Each face cycles through disabled, passive input, active input, and low, medium, or high-priority output, and has an independent redstone gate.
- Right-click a filterable face with the configurator to search and select a fluid. A fluid container in the off hand applies its fluid immediately; crouch with an empty off hand to clear the filter.
- Right-click a tank controller to open its screen. It shows fluid contents, container input/output slots, a filter target and six output channels. Click the filter with a fluid container on the cursor to set it, or with an empty cursor to clear it. Output channels appear when outlet faces connect to compatible neighbours; each has its own redstone setting.
- Tank blocks and indicators contribute storage; modules connect through other modules to one controller. Tank interfaces expose the container input and output to item automation. Tank access ports expose stored fluid.
- Right-click a tank sensor to configure fill level, output flow or fluid matching. Output flow can target a particular output channel. Crouch-right-click still cycles the fill threshold.
- Canisters show their stored fluid and fill level in inventory. Placing a source consumes 1,000 mB, including in creative mode; water vaporizes in the Nether. The hand pump fills inventory containers from fluid blocks or machines, including individual buckets taken from a stack.
- The drain accepts excess fluid and can turn supported experience fluids into orbs. The sluice holds one bucket and stops accepting more while its outlet is blocked.
- The automatic fluid-transfer cap is `pressure.fluidTransferPerTick` in `pressure-common.toml`; use `2147483647` for legacy unrestricted throughput.
