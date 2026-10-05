# CC: Tweaked peripherals

Install CC: Tweaked 1.120.2 for Forge 1.20.1 (or a newer compatible Forge version). Computers can attach directly or use wired modems. CC: Tweaked is optional; base installations need no computer mod.

```lua
local tank = peripheral.find("pp_tank")
print(tank.isConnected(), tank.getCapacity())
print(textutils.serialize(tank.getFluid()))
tank.setOutputMode("red", "RS_ON")
tank.setFilter("minecraft:water")
local sensor = peripheral.find("pp_sensor")
print(textutils.serialize(sensor.getAverages()))
```

| Tank data-port command | Result / arguments |
|---|---|
| `isConnected()` | Boolean; false while its controller is absent |
| `getCapacity()` | Capacity in mB |
| `hasFluid()` | Boolean |
| `getFluid()` | `{name="minecraft:water", amount=1000}` or nil when empty |
| `getOutputs()` | Table keyed by red, green, blue, yellow, cyan, purple. Connected entries contain `type="fluid"`, `mode`, `average` in mB/t; unused entries have `type="unconnected"`. |
| `setOutputMode(color, mode)` | True; modes `ALWAYS`, `RS_ON`, `RS_OFF`, `NEVER`. Case-insensitive. Invalid names/modes and unconnected outputs raise Lua errors. |
| `getFilter()` | Fluid registry ID or nil |
| `setFilter(id)` | True; unknown IDs raise errors without changing the filter. `nil` or no argument clears it. |
| `getCountsIn()`, `getCountsOut()` | Lua sequences of `{name=fluidId, count=mB}` records; executed transfers only |

Pipe sensor `getAverages()` returns a sequence of `{name=fluidId, average=mBPerTick}` records over its recent 50-tick window. `getCounts()` returns `{name=fluidId, count=mB}` records. Counts and sample history survive saves.

Namespaced registry IDs replace 1.12.2's unqualified fluid names. Counts remain Lua numbers (double precision: exact integer representation through 2^53); normal Minecraft transfer calls retain signed 32-bit amounts. Tank modes, filters and counters use the same authoritative state as the GUI. Calls execute on the server thread. Disconnected tank methods raise a Lua error except `isConnected()`; empty fluid/filter values return nil. Empty count/average sequences are empty tables.
